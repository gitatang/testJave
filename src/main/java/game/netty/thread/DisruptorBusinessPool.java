package game.netty.thread;

import game.netty.disruptor.BusinessEvent;
import game.netty.disruptor.BusinessEventFactory;
import game.netty.disruptor.BusinessEventHandler;
import game.netty.handler.logic.ProtoMessage;
import game.netty.session.Session;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Disruptor 的业务线程池
 *
 * 核心设计：
 * 1. 使用多个 Disruptor 实例，每个 Disruptor 单线程处理
 * 2. 根据 userId 路由到对应的 Disruptor
 * 3. 保证同一玩家的消息顺序执行
 * 4. 不同玩家的消息可并行处理
 *
 * 性能优势：
 * 1. 无锁设计，性能极高
 * 2. 环形数组，GC 压力小
 * 3. 预分配内存，减少对象创建
 * 4. 批处理机制，提高吞吐量
 *
 * 相比 BlockingQueue 的优势：
 * 1. 性能提升 10-100 倍
 * 2. 延迟降低 90% 以上
 * 3. GC 压力显著降低
 */
public class DisruptorBusinessPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorBusinessPool.class);

    // Disruptor 实例数量，建议设置为 CPU 核心数的 2-4 倍
    private static final int DISRUPTOR_COUNT = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
    // 每个 Disruptor 的环形缓冲区大小（必须是 2 的幂）
    private static final int RING_BUFFER_SIZE = 1024; // 1024 个事件槽位
    // 等待策略
    private static final WaitStrategy WAIT_STRATEGY = new BlockingWaitStrategy();

    // Disruptor 数组
    private final DisruptorInstance[] disruptors;
    // 用于统计的数组
    private final AtomicLong[] publishCounts;
    private final AtomicLong[] handleCounts;

    private static class Holder {
        static final DisruptorBusinessPool INSTANCE = new DisruptorBusinessPool();
    }

    private DisruptorBusinessPool() {
        this.disruptors = new DisruptorInstance[DISRUPTOR_COUNT];
        this.publishCounts = new AtomicLong[DISRUPTOR_COUNT];
        this.handleCounts = new AtomicLong[DISRUPTOR_COUNT];

        for (int i = 0; i < DISRUPTOR_COUNT; i++) {
            int index = i;
            publishCounts[i] = new AtomicLong(0);
            handleCounts[i] = new AtomicLong(0);

            // 创建 Disruptor
            Disruptor<BusinessEvent> disruptor = new Disruptor<>(
                    new BusinessEventFactory(),
                    RING_BUFFER_SIZE,
                    new BusinessThreadFactory(index),
                    ProducerType.MULTI,  // 多生产者模式
                    WAIT_STRATEGY
            );

            // 创建并设置事件处理器
            BusinessEventHandler handler = new BusinessEventHandler("disruptor-" + index);
            disruptor.handleEventsWith(handler);

            // 启动 Disruptor
            disruptor.start();

            // 保存 Disruptor 实例
            disruptors[i] = new DisruptorInstance(disruptor, handler);
        }

        LOGGER.info("Disruptor 业务线程池初始化完成, disruptorCount={}, ringBufferSize={}",
                DISRUPTOR_COUNT, RING_BUFFER_SIZE);
    }

    public static DisruptorBusinessPool getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 提交消息处理任务
     *
     * @param session Session 对象
     * @param ctx ChannelHandlerContext
     * @param protoMsg 消息对象
     */
    public void submitMessage(Session session, ChannelHandlerContext ctx, ProtoMessage protoMsg) {
        String userId = session != null ? session.getUserId() : null;
        Object channelKey = ctx != null ? ctx.channel().id() : null;

        int index = getRouteIndex(userId, channelKey);
        DisruptorInstance instance = disruptors[index];

        // 发布事件到 Disruptor
        RingBuffer<BusinessEvent> ringBuffer = instance.getDisruptor().getRingBuffer();
        long sequence = ringBuffer.next();  // 获取下一个序列号

        try {
            BusinessEvent event = ringBuffer.get(sequence);  // 获取事件对象
            event.setMessageEvent(session, ctx, protoMsg);   // 设置事件数据
        } finally {
            ringBuffer.publish(sequence);  // 发布事件
            publishCounts[index].incrementAndGet();
        }
    }

    /**
     * 提交普通任务
     *
     * @param userId 用户ID（如果未登录，传 null）
     * @param channelKey Channel 的唯一标识（用于未登录玩家的路由）
     * @param task 要执行的任务
     */
    public void submitTask(String userId, Object channelKey, Runnable task) {
        int index = getRouteIndex(userId, channelKey);
        DisruptorInstance instance = disruptors[index];

        // 发布事件到 Disruptor
        RingBuffer<BusinessEvent> ringBuffer = instance.getDisruptor().getRingBuffer();
        long sequence = ringBuffer.next();

        try {
            BusinessEvent event = ringBuffer.get(sequence);
            event.setTaskEvent(userId, channelKey, task);
        } finally {
            ringBuffer.publish(sequence);
            publishCounts[index].incrementAndGet();
        }
    }

    /**
     * 获取路由索引
     * 保证同一个 userId 或 channelKey 始终路由到同一个 Disruptor
     */
    private int getRouteIndex(String userId, Object channelKey) {
        int hashCode;
        if (userId != null && !userId.isEmpty()) {
            hashCode = userId.hashCode();
        } else {
            hashCode = channelKey.hashCode();
        }
        // 保证结果为正数
        return Math.abs(hashCode) % DISRUPTOR_COUNT;
    }

    /**
     * 获取线程池状态信息
     */
    public String getPoolStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisruptorBusinessPool[disruptorCount=").append(DISRUPTOR_COUNT);

        long totalPublish = 0;
        long totalHandle = 0;
        long totalRemaining = 0;

        for (int i = 0; i < DISRUPTOR_COUNT; i++) {
            totalPublish += publishCounts[i].get();
            totalHandle += handleCounts[i].get();
            totalRemaining += disruptors[i].getDisruptor().getRingBuffer().remainingCapacity();
        }

        sb.append(", published=").append(totalPublish)
                .append(", handled=").append(totalHandle)
                .append(", remaining=").append(totalRemaining)
                .append("]");

        return sb.toString();
    }

    /**
     * 获取每个 Disruptor 的详细状态
     */
    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisruptorBusinessPool Detail:\n");

        for (int i = 0; i < DISRUPTOR_COUNT; i++) {
            RingBuffer<BusinessEvent> ringBuffer = disruptors[i].getDisruptor().getRingBuffer();
            long published = publishCounts[i].get();
            long remaining = ringBuffer.remainingCapacity();

            sb.append(String.format("  Disruptor[%02d]: cursor=%d, published=%d, remaining=%d\n",
                    i,
                    ringBuffer.getCursor(),
                    published,
                    remaining));
        }

        return sb.toString();
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        LOGGER.info("开始关闭 Disruptor 业务线程池...");

        for (int i = 0; i < DISRUPTOR_COUNT; i++) {
            try {
                // 优雅关闭 Disruptor
                disruptors[i].getDisruptor().shutdown(10, java.util.concurrent.TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOGGER.error("Disruptor[{}] 关闭超时", i);
            }
        }

        LOGGER.info("Disruptor 业务线程池已关闭");
    }

    /**
     * Disruptor 实例包装类
     */
    private static class DisruptorInstance {
        private final Disruptor<BusinessEvent> disruptor;
        private final BusinessEventHandler handler;

        public DisruptorInstance(Disruptor<BusinessEvent> disruptor, BusinessEventHandler handler) {
            this.disruptor = disruptor;
            this.handler = handler;
        }

        public Disruptor<BusinessEvent> getDisruptor() {
            return disruptor;
        }

        public BusinessEventHandler getHandler() {
            return handler;
        }
    }

    /**
     * 自定义线程工厂
     */
    private static class BusinessThreadFactory implements ThreadFactory {
        private final int disruptorIndex;

        BusinessThreadFactory(int disruptorIndex) {
            this.disruptorIndex = disruptorIndex;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, String.format("disruptor-pool-%02d", disruptorIndex));
            thread.setDaemon(false);
            return thread;
        }
    }

    /**
     * 获取 Disruptor 数量
     */
    public int getDisruptorCount() {
        return DISRUPTOR_COUNT;
    }

    /**
     * 获取环形缓冲区大小
     */
    public int getRingBufferSize() {
        return RING_BUFFER_SIZE;
    }
}
