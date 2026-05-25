package game.netty.disruptor;

import game.netty.handler.logic.ProtoMessage;
import game.netty.session.Session;
import game.netty.thread.DisruptorBusinessPool;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disruptor 执行流程可视化演示
 *
 * 这个类展示了从消息接收到业务逻辑执行的完整流程
 * 包括每个步骤的详细日志输出
 */
public class DisruptorVisualizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorVisualizer.class);

    /**
     * 模拟完整的消息处理流程
     */
    public static void demonstrateFlow() {
        LOGGER.info("\n" + "=".repeat(80));
        LOGGER.info("Disruptor 消息处理流程演示");
        LOGGER.info("=".repeat(80));

        // ==================== 步骤 1: 接收消息 ====================
        LOGGER.info("\n【步骤 1】EventLoop 线程接收到网络消息");
        LOGGER.info("  ├─ 线程: nioEventLoopGroup-3-1");
        LOGGER.info("  ├─ 操作: GameDecodeHandler 解码 ProtoMessage");
        LOGGER.info("  └─ 结果: ProtoMessage{version=2, type=0x11, requestId=123}");

        // ==================== 步骤 2: 路由计算 ====================
        LOGGER.info("\n【步骤 2】GameLogicHandler 计算路由");
        String userId = "player001";
        int hashCode = Math.abs(userId.hashCode());
        int disruptorCount = 16;
        int routeIndex = hashCode % disruptorCount;

        LOGGER.info("  ├─ 用户ID: {}", userId);
        LOGGER.info("  ├─ HashCode: {}", hashCode);
        LOGGER.info("  ├─ Disruptor数量: {}", disruptorCount);
        LOGGER.info("  └─ 路由结果: Disruptor[{}]", routeIndex);

        // ==================== 步骤 3: 发布事件 ====================
        LOGGER.info("\n【步骤 3】发布事件到 RingBuffer[{}]", routeIndex);
        LOGGER.info("  ├─ 操作: ringBuffer.next()");
        LOGGER.info("  ├─ 获取序列号: 1234");
        LOGGER.info("  ├─ 获取Event对象: ringBuffer.get(1234)");
        LOGGER.info("  ├─ 填充Event数据:");
        LOGGER.info("  │   ├─ eventType = MESSAGE_HANDLE");
        LOGGER.info("  │   ├─ session = Session{sessionId=1001, userId='player001'}");
        LOGGER.info("  │   └─ protoMessage = ProtoMessage{...}");
        LOGGER.info("  └─ 操作: ringBuffer.publish(1234) 🔥");

        // ==================== 步骤 4: Disruptor 内部处理 ====================
        LOGGER.info("\n【步骤 4】Disruptor[{}] 内部处理", routeIndex);
        LOGGER.info("  ├─ 更新 cursor: 1233 → 1234");
        LOGGER.info("  ├─ 通知 SequenceBarrier");
        LOGGER.info("  └─ 唤醒消费者线程: disruptor-pool-{:02d}", routeIndex);

        // ==================== 步骤 5: 消费者处理事件 ====================
        LOGGER.info("\n【步骤 5】BusinessEventHandler.onEvent()");
        LOGGER.info("  ├─ 执行线程: disruptor-pool-{:02d}", routeIndex);
        LOGGER.info("  ├─ 接收事件:");
        LOGGER.info("  │   ├─ sequence = 1234");
        LOGGER.info("  │   ├─ endOfBatch = true");
        LOGGER.info("  │   └─ event = BusinessEvent{eventType=MESSAGE_HANDLE, ...}");
        LOGGER.info("  └─ 判断事件类型: MESSAGE_HANDLE");

        // ==================== 步骤 6: 执行业务逻辑 ====================
        LOGGER.info("\n【步骤 6】执行业务逻辑 handleMessage()");
        LOGGER.info("  ├─ 提取数据:");
        LOGGER.info("  │   ├─ session = Session{sessionId=1001, userId='player001'}");
        LOGGER.info("  │   ├─ protoMessage = UserProto.LoginReqV2{...}");
        LOGGER.info("  │   └─ messageType = 0x11");
        LOGGER.info("  ├─ 获取处理器: MessageRegistry.getHandler(2, 0x11)");
        LOGGER.info("  │   └─ 返回: LoginReqV2Handler");
        LOGGER.info("  ├─ 反射调用:");
        LOGGER.info("  │   └─ LoginReqV2Handler.handler(ctx, LoginReqV2)");
        LOGGER.info("  └─ 进入业务代码 🎯");

        // ==================== 步骤 7: 业务处理器执行 ====================
        LOGGER.info("\n【步骤 7】LoginReqV2Handler 执行业务逻辑");
        LOGGER.info("  ├─ 解析请求:");
        LOGGER.info("  │   ├─ username = 'player001'");
        LOGGER.info("  │   └─ password = '******'");
        LOGGER.info("  ├─ 执行操作:");
        LOGGER.info("  │   ├─ sessionManager.bindUser(session, 'player001')");
        LOGGER.info("  │   ├─ 构建响应: LoginResV2{code=200, ...}");
        LOGGER.info("  │   ├─ 发送响应: session.sendMessage(...)");
        LOGGER.info("  │   └─ 广播消息: sessionManager.broadcast(...)");
        LOGGER.info("  └─ 业务逻辑完成 ✓");

        // ==================== 步骤 8: 清理和复用 ====================
        LOGGER.info("\n【步骤 8】清理 Event 对象");
        LOGGER.info("  ├─ event.clear()");
        LOGGER.info("  ├─ 清空数据:");
        LOGGER.info("  │   ├─ eventType = null");
        LOGGER.info("  │   ├─ session = null");
        LOGGER.info("  │   ├─ protoMessage = null");
        LOGGER.info("  │   └─ ...");
        LOGGER.info("  └─ Event 对象返回 RingBuffer，等待下次复用 ♻️");

        // ==================== 总结 ====================
        LOGGER.info("\n" + "=".repeat(80));
        LOGGER.info("流程总结");
        LOGGER.info("=".repeat(80));
        LOGGER.info("  总耗时: ~1-5ms (取决于业务逻辑复杂度)");
        LOGGER.info("  线程切换: 1 次 (EventLoop → disruptor-pool-03)");
        LOGGER.info("  对象创建: 0 次 (全部使用预分配对象)");
        LOGGER.info("  锁竞争: 0 次 (CAS 无锁操作)");
        LOGGER.info("  顺序保证: ✓ (同一玩家消息在同一 Disruptor 中顺序执行)");
        LOGGER.info("\n" + "=".repeat(80));
    }

    /**
     * 展示多个玩家的并行处理
     */
    public static void demonstrateParallelProcessing() {
        LOGGER.info("\n" + "=".repeat(80));
        LOGGER.info("多玩家并行处理演示");
        LOGGER.info("=".repeat(80));

        String[] players = {"player001", "player002", "player003", "player004"};
        int[] routes = new int[players.length];

        LOGGER.info("\n【路由计算】");
        for (int i = 0; i < players.length; i++) {
            routes[i] = Math.abs(players[i].hashCode()) % 16;
            LOGGER.info("  {} → Disruptor[{}] → thread=disruptor-pool-{:02d}",
                    players[i], routes[i], routes[i]);
        }

        LOGGER.info("\n【并行执行】");
        LOGGER.info("  ├─ player001 和 player003 路由到同一个 Disruptor[{}]", routes[0]);
        LOGGER.info("  │   └─ 顺序执行: player001_msg1 → player003_msg1 → player001_msg2");
        LOGGER.info("  ├─ player002 路由到 Disruptor[{}]", routes[1]);
        LOGGER.info("  │   └─ 独立执行，与其他玩家并行");
        LOGGER.info("  └─ player004 路由到 Disruptor[{}]", routes[3]);
        LOGGER.info("      └─ 独立执行，与其他玩家并行");

        LOGGER.info("\n【性能优势】");
        LOGGER.info("  ✓ 同一玩家: 消息严格顺序执行");
        LOGGER.info("  ✓ 不同玩家: 消息并行执行，互不影响");
        LOGGER.info("  ✓ 充分利用多核 CPU");

        LOGGER.info("=".repeat(80));
    }

    /**
     * 展示 RingBuffer 的工作原理
     */
    public static void demonstrateRingBuffer() {
        LOGGER.info("\n" + "=".repeat(80));
        LOGGER.info("RingBuffer 工作原理演示");
        LOGGER.info("=".repeat(80));

        LOGGER.info("\n【RingBuffer 结构】(大小 = 1024)");
        LOGGER.info("  ┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐");
        LOGGER.info("  │  0   │  1   │  2   │  3   │  4   │ ... │ 1022 │ 1023 │");
        LOGGER.info("  │Event │Event │Event │Event │Event │     │Event │Event │");
        LOGGER.info("  └──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘");
        LOGGER.info("    ↑                                            ↑");
        LOGGER.info("  cursor                                          buffer end");
        LOGGER.info("   (当前)                                        (边界)");

        LOGGER.info("\n【预分配优势】");
        LOGGER.info("  ├─ 启动时创建 1024 个 BusinessEvent 对象");
        LOGGER.info("  ├─ 运行过程中不再创建新对象");
        LOGGER.info("  ├─ 通过 clear() 复用对象");
        LOGGER.info("  └─ GC 压力显著降低");

        LOGGER.info("\n【环形特性】");
        LOGGER.info("  ├─ 序列号递增: 0, 1, 2, ..., 1023, 1024, 1025, ...");
        LOGGER.info("  ├─ 数组索引: sequence & 1023 (位运算取模)");
        LOGGER.info("  │   sequence=1024 → index=0");
        LOGGER.info("  │   sequence=1025 → index=1");
        LOGGER.info("  │   sequence=1026 → index=2");
        LOGGER.info("  └─ 自动循环，无需重置");

        LOGGER.info("\n【无锁设计】");
        LOGGER.info("  ├─ 生产者获取序列: CAS 操作，无锁");
        LOGGER.info("  ├─ 消费者等待: SequenceBarrier，无锁");
        LOGGER.info("  └─ 性能提升: 10-100 倍");

        LOGGER.info("=".repeat(80));
    }

    public static void main(String[] args) {
        demonstrateFlow();
        demonstrateParallelProcessing();
        demonstrateRingBuffer();
    }
}