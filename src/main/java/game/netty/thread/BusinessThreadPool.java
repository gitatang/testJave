package game.netty.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务逻辑线程池（玩家顺序化版本）
 *
 * 设计理念：
 * 1. 全局使用多个业务线程处理不同玩家的消息
 * 2. 同一个玩家的消息始终路由到同一个线程（根据 userId 取模）
 * 3. 保证同一个玩家的消息顺序执行
 * 4. 不同玩家的消息可以并行处理
 *
 * 路由策略：
 * - 根据 userId.hashCode() % 线程数 来决定路由到哪个线程
 * - 未登录的玩家根据 channelId.hashCode() % 线程数 来路由
 */
public class BusinessThreadPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessThreadPool.class);

    // 业务线程数，建议设置为 CPU 核心数的 2-4 倍
    private static final int THREAD_COUNT = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
    // 每个线程的队列容量
    private static final int QUEUE_CAPACITY_PER_THREAD = 1000;

    // 线程池数组，每个线程有独立的队列
    private final ExecutorService[] executors;
    // 用于统计的数组
    private final AtomicInteger[] taskCounts;

/**
 * 静态内部类Holder，用于实现单例模式
 * 这种方式利用了JVM类加载机制来保证线程安全
 */
    private static class Holder {
    // 静态final变量，保证INSTANCE的唯一性和不可变性
        static final BusinessThreadPool INSTANCE = new BusinessThreadPool();
    }

    private BusinessThreadPool() {
        this.executors = new ExecutorService[THREAD_COUNT];
        this.taskCounts = new AtomicInteger[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadIndex = i;
            taskCounts[i] = new AtomicInteger(0);

            // 每个线程使用单线程的 ThreadPoolExecutor，保证顺序执行
            executors[i] = new ThreadPoolExecutor(
                    1,  // 核心线程数
                    1,  // 最大线程数
                    0L, // 空闲线程存活时间
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD),
                    new BusinessThreadFactory(threadIndex),
                    new BusinessRejectedExecutionHandler(threadIndex)
            );

            // 允许核心线程超时（可选）
            // ((ThreadPoolExecutor) executors[i]).allowCoreThreadTimeOut(true);
        }

        LOGGER.info("业务线程池初始化完成, threadCount={}, queueCapacityPerThread={}",
                THREAD_COUNT, QUEUE_CAPACITY_PER_THREAD);
    }

    public static BusinessThreadPool getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 提交任务到业务线程池
     * 根据 userId 路由到对应的线程，保证同一玩家的消息顺序
     *
     * @param userId 用户ID（如果未登录，传 null）
     * @param channelKey Channel 的唯一标识（用于未登录玩家的路由）
     * @param task 要执行的任务
     */
    public void submit(String userId, Object channelKey, Runnable task) {
        int index = getRouteIndex(userId, channelKey);
        executors[index].execute(new TaskWrapper(task, index));
    }

    /**
     * 提交带返回值的任务
     */
    public <T> Future<T> submit(String userId, Object channelKey, Callable<T> task) {
        int index = getRouteIndex(userId, channelKey);
        return executors[index].submit(new CallableWrapper<>(task, index));
    }

    /**
     * 获取路由索引
     * 保证同一个 userId 或 channelKey 始终路由到同一个线程
     */
    private int getRouteIndex(String userId, Object channelKey) {
        int hashCode;
        if (userId != null && !userId.isEmpty()) {
            hashCode = userId.hashCode();
        } else {
            hashCode = channelKey.hashCode();
        }
        // 保证结果为正数
        return Math.abs(hashCode) % THREAD_COUNT;
    }

    /**
     * 获取线程池状态信息
     */
    public String getPoolStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusinessThreadPool[threadCount=").append(THREAD_COUNT).append(", ");

        int totalActive = 0;
        int totalQueue = 0;
        int totalCompleted = 0;

        for (int i = 0; i < THREAD_COUNT; i++) {
            if (executors[i] instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) executors[i];
                totalActive += executor.getActiveCount();
                totalQueue += executor.getQueue().size();
                totalCompleted += executor.getCompletedTaskCount();
            }
        }

        sb.append("active=").append(totalActive)
                .append(", queue=").append(totalQueue)
                .append(", completed=").append(totalCompleted)
                .append("]");

        return sb.toString();
    }

    /**
     * 获取每个线程的详细状态
     */
    public String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusinessThreadPool Detail:\n");

        for (int i = 0; i < THREAD_COUNT; i++) {
            if (executors[i] instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor executor = (ThreadPoolExecutor) executors[i];
                sb.append(String.format("  Thread[%02d]: active=%d, queue=%d, completed=%d\n",
                        i,
                        executor.getActiveCount(),
                        executor.getQueue().size(),
                        executor.getCompletedTaskCount()));
            }
        }

        return sb.toString();
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        LOGGER.info("开始关闭业务线程池...");
        for (int i = 0; i < THREAD_COUNT; i++) {
            executors[i].shutdown();
        }

        // 等待所有任务完成
        for (int i = 0; i < THREAD_COUNT; i++) {
            try {
                if (!executors[i].awaitTermination(10, TimeUnit.SECONDS)) {
                    executors[i].shutdownNow();
                    if (!executors[i].awaitTermination(10, TimeUnit.SECONDS)) {
                        LOGGER.error("业务线程[{}] 未完全关闭", i);
                    }
                }
            } catch (InterruptedException e) {
                executors[i].shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("业务线程池已关闭");
    }

    /**
     * 自定义线程工厂
     */
    private static class BusinessThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final int threadIndex;

        BusinessThreadFactory(int threadIndex) {
            this.threadIndex = threadIndex;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, String.format("business-pool-%02d", threadIndex));
            thread.setDaemon(false);
            return thread;
        }
    }

    /**
     * 自定义拒绝策略
     */
    private static class BusinessRejectedExecutionHandler implements RejectedExecutionHandler {
        private final int threadIndex;

        BusinessRejectedExecutionHandler(int threadIndex) {
            this.threadIndex = threadIndex;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            LOGGER.warn("业务线程[{}]队列已满, queue={}, 在调用线程中执行任务",
                    threadIndex, e.getQueue().size());
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * 任务包装器，用于统计
     */
    private class TaskWrapper implements Runnable {
        private final Runnable task;
        private final int threadIndex;

        TaskWrapper(Runnable task, int threadIndex) {
            this.task = task;
            this.threadIndex = threadIndex;
        }

        @Override
        public void run() {
            try {
                task.run();
            } finally {
                taskCounts[threadIndex].incrementAndGet();
            }
        }
    }

    /**
     * Callable 包装器
     */
    private class CallableWrapper<T> implements Callable<T> {
        private final Callable<T> task;
        private final int threadIndex;

        CallableWrapper(Callable<T> task, int threadIndex) {
            this.task = task;
            this.threadIndex = threadIndex;
        }

        @Override
        public T call() throws Exception {
            try {
                return task.call();
            } finally {
                taskCounts[threadIndex].incrementAndGet();
            }
        }
    }

    /**
     * 获取线程数
     */
    public int getThreadCount() {
        return THREAD_COUNT;
    }

    /**
     * 检查是否为当前线程（避免不必要的线程切换）
     */
    public boolean isInBusinessThread(String userId, Object channelKey) {
        int index = getRouteIndex(userId, channelKey);
        String threadName = Thread.currentThread().getName();
        return threadName.equals(String.format("business-pool-%02d", index));
    }
}