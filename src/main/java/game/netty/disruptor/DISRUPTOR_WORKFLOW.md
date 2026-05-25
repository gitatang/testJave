# Disruptor 任务执行流程详解

## 1. 整体架构流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         步骤 1: 接收消息                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│  EventLoop 线程接收到网络消息                                              │
│  - GameDecodeHandler 解码出 ProtoMessage                                 │
│  - 传递给 GameLogicHandler.channelRead0()                                │
└─────────────────────────────────────────────────────────────────────────┘
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         步骤 2: 路由计算                                  │
└─────────────────────────────────────────────────────────────────────────┘
         GameLogicHandler.channelRead0()
         ↓
         获取 session.getUserId()  // 例如: "player001"
         ↓
         DisruptorBusinessPool.submitMessage(session, ctx, protoMsg)
         ↓
         计算路由索引:
         int index = Math.abs("player001".hashCode()) % 16;
         // 假设计算结果为 3，路由到 Disruptor[3]
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                      步骤 3: 发布事件到 RingBuffer                        │
└─────────────────────────────────────────────────────────────────────────┘
         DisruptorBusinessPool.submitMessage()
         ↓
         获取 disruptors[3] 的 RingBuffer
         ↓
         long sequence = ringBuffer.next();
         // 获取下一个可用序列号，例如: 1234
         ↓
         BusinessEvent event = ringBuffer.get(sequence);
         // 从环形数组获取预分配的 Event 对象
         ↓
         event.setMessageEvent(session, ctx, protoMsg);
         // 填充数据到 Event 对象
         ↓
         ringBuffer.publish(sequence);
         // 🔥 关键：发布事件，通知消费者

┌─────────────────────────────────────────────────────────────────────────┐
│                      步骤 4: Disruptor 内部处理                           │
└─────────────────────────────────────────────────────────────────────────┘
         RingBuffer 内部机制:
         ↓
         1. 更新 cursor 序列号到 1234
         2. 通过 SequenceBarrier 通知等待的消费者
         3. 唤醒 BusinessEventHandler 线程
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    步骤 5: 消费者处理事件                                 │
└─────────────────────────────────────────────────────────────────────────┘
         BusinessEventHandler.onEvent()
         (运行在 disruptor-pool-03 线程中)
         ↓
         接收到事件:
         - sequence = 1234
         - event = BusinessEvent{eventType=MESSAGE_HANDLE, ...}
         ↓
         switch (event.getEventType()) {
             case MESSAGE_HANDLE:
                 handleMessage(event);  // 处理消息
                 break;
         }
                                ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    步骤 6: 执行业务逻辑                                   │
└─────────────────────────────────────────────────────────────────────────┘
         BusinessEventHandler.handleMessage()
         ↓
         1. 提取信息:
            - Session session = event.getSession();
            - ProtoMessage protoMsg = event.getProtoMessage();
            - byte type = protoMsg.getType();
         ↓
         2. 获取消息处理器:
            MessageHandler<?> handler = MessageRegistry.getHandler(version, type);
            // 例如: 获取到 LoginReqV2Handler
         ↓
         3. 反射调用处理器:
            Method handleMethod = handler.getClass()
                .getMethod("handler", ChannelHandlerContext.class, typedMsg.getClass());
            handleMethod.invoke(handler, ctx, typedMsg);
         ↓
         4. 进入你的业务代码:
            LoginReqV2Handler.handler(ctx, LoginReqV2 msg) {
                // 🎯 这里执行你的业务逻辑
                String username = msg.getUsername();
                sessionManager.bindUser(session, username);
                session.sendMessage(...);
            }
         ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                    步骤 7: 清理和复用                                     │
└─────────────────────────────────────────────────────────────────────────┘
         BusinessEventHandler.onEvent() finally 块
         ↓
         event.clear();
         // 清空 Event 对象的数据
         // Event 对象返回到 RingBuffer，可以被下次复用
```

## 2. 核心数据结构

### RingBuffer（环形缓冲区）

```
RingBuffer<BusinessEvent> (大小 = 1024)

┌──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐
│  0   │  1   │  2   │  3   │  4   │  5   │ ... │ 1231│ 1232│ 1233│
│Event │Event │Event │Event │Event │Event │     │Event │Event │Event │
└──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘
        ↑                              ↑           ↑
      cursor                        published    next
      (1234)                        (1234)     (1235)

预分配的 BusinessEvent 对象:
- 所有 1024 个 Event 对象在启动时就创建好了
- 不会在运行过程中频繁创建/销毁
- 通过 clear() 方法复用，减少 GC 压力
```

### Disruptor 实例数组

```
DisruptorBusinessPool.disruptors[16]

┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
│Disruptor[0] │Disruptor[1] │Disruptor[2] │Disruptor[3] │Disruptor[4] │...
│线程: pool-00│线程: pool-01│线程: pool-02│线程: pool-03│线程: pool-04│
│Ring: 1024   │Ring: 1024   │Ring: 1024   │Ring: 1024   │Ring: 1024   │
└─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘

每个 Disruptor 独立运行，互不干扰
player001 → hashCode % 16 → 3 → Disruptor[3] → pool-03 线程处理
player002 → hashCode % 16 → 7 → Disruptor[7] → pool-07 线程处理
player003 → hashCode % 16 → 3 → Disruptor[3] → pool-03 线程处理 (与player001同线程)
```

## 3. 详细代码执行流程

### 3.1 生产者（发布事件）

```java
// 1. 计算路由
int index = Math.abs(userId.hashCode()) % 16;  // 例如: 3

// 2. 获取对应的 RingBuffer
RingBuffer<BusinessEvent> ringBuffer = disruptors[3].getDisruptor().getRingBuffer();

// 3. 获取下一个序列号（CAS 操作，无锁）
long sequence = ringBuffer.next();  // 例如: 1234

// 4. 获取预分配的 Event 对象
BusinessEvent event = ringBuffer.get(sequence);  // 直接从数组获取，不创建新对象

// 5. 填充数据
event.setMessageEvent(session, ctx, protoMsg);
// 等价于:
// event.eventType = EventType.MESSAGE_HANDLE;
// event.session = session;
// event.ctx = ctx;
// event.protoMessage = protoMsg;
// event.userId = session.getUserId();

// 6. 发布事件（关键步骤）
ringBuffer.publish(sequence);
// 内部做了什么:
// - 更新 cursor: cursor.set(sequence);
// - 通知等待的消费者: sequenceBarrier.alert();
// - 唤醒消费者线程 (如果使用等待策略)
```

### 3.2 消费者（处理事件）

```java
// BusinessEventHandler 线程在 Disruptor 启动时创建并持续运行

// Disruptor 启动过程:
Disruptor<BusinessEvent> disruptor = new Disruptor<>(
    new BusinessEventFactory(),     // 创建 Event 对象的工厂
    1024,                            // RingBuffer 大小
    new BusinessThreadFactory(3),    // 线程工厂，创建 "disruptor-pool-03" 线程
    ProducerType.MULTI,              // 多生产者模式
    new BlockingWaitStrategy()       // 等待策略
);

// 设置事件处理器
disruptor.handleEventsWith(new BusinessEventHandler("disruptor-3"));

// 启动 Disruptor
disruptor.start();  // 🔥 此时消费者线程开始运行

// 消费者线程的主循环（Disruptor 框架内部实现）:
while (disruptor.isRunning()) {
    // 1. 等待可用的事件
    long availableSequence = sequenceBarrier.waitFor(nextSequence);
    // 例如: 等待到 sequence 1234 可用

    // 2. 批量处理事件
    for (long seq = nextSequence; seq <= availableSequence; seq++) {
        BusinessEvent event = ringBuffer.get(seq);
        // 🔥 调用你的事件处理器
        handler.onEvent(event, seq, seq == availableSequence);
    }

    // 3. 更新已处理的序列号
    nextSequence = availableSequence + 1;
}

// 你的 onEvent 方法被调用:
@Override
public void onEvent(BusinessEvent event, long sequence, boolean endOfBatch) {
    try {
        // 处理事件
        handleMessage(event);
    } finally {
        // 清空事件，便于复用
        event.clear();
        // 等价于:
        // event.eventType = null;
        // event.session = null;
        // event.ctx = null;
        // event.protoMessage = null;
        // ...
    }
}
```

### 3.3 业务逻辑执行

```java
private void handleMessage(BusinessEvent event) {
    // 1. 提取数据
    Session session = event.getSession();
    ProtoMessage protoMsg = event.getProtoMessage();

    // 2. 获取消息类型
    byte type = protoMsg.getType();  // 例如: 0x11

    // 3. 从注册表获取处理器
    MessageHandler<?> handler = MessageRegistry.getHandler(version, type);
    // 例如: 返回 LoginReqV2Handler 实例

    // 4. 获取消息对象
    MessageLite rawMsg = protoMsg.getMessage();
    // 例如: UserProto.LoginReqV2 实例

    // 5. 类型转换
    UserProto.LoginReqV2 typedMsg = (UserProto.LoginReqV2) rawMsg;

    // 6. 反射调用你的业务处理器
    Method handleMethod = LoginReqV2Handler.class
        .getMethod("handler", ChannelHandlerContext.class, UserProto.LoginReqV2.class);

    // 🔥 这里进入你的业务代码
    handleMethod.invoke(handlerInstance, ctx, typedMsg);

    // 等价于直接调用:
    // handlerInstance.handler(ctx, typedMsg);
}

// 你的业务处理器:
public class LoginReqV2Handler implements MessageHandler<UserProto.LoginReqV2> {
    @Override
    public void handler(ChannelHandlerContext ctx, UserProto.LoginReqV2 msg) {
        // 🎯🎯🎯 这里执行你的业务逻辑 🎯🎯🎯
        String username = msg.getUsername();
        String password = msg.getPassword();

        // 绑定用户
        sessionManager.bindUser(session, username);

        // 发送响应
        session.sendMessage((byte) 2, (byte) 0x12, 0, responseBuilder.build());

        // 广播消息
        sessionManager.broadcast((byte) 1, (byte) 0x20, 0, broadcastMsg);
    }
}
```

## 4. 关键时序图

```
时间轴 →

EventLoop线程        RingBuffer          disruptor-pool-03线程        业务代码
    │                    │                        │                    │
    │ channelRead0()     │                        │                    │
    │                    │                        │                    │
    │ submitMessage()    │                        │                    │
    ├───────────────────>│                        │                    │
    │ ringBuffer.next()  │                        │                    │
    │<───────────────────┤                        │                    │
    │ sequence=1234      │                        │                    │
    │                    │                        │                    │
    │ get(1234)          │                        │                    │
    │<───────────────────┤                        │                    │
    │ event              │                        │                    │
    │                    │                        │                    │
    │ setMessageEvent()  │                        │                    │
    ├───────────────────>│                        │                    │
    │                    │ [Event填充数据]        │                    │
    │                    │                        │                    │
    │ publish(1234)      │                        │                    │
    ├───────────────────>│                        │                    │
    │                    │ cursor=1234            │                    │
    │                    │ alert() ──────────────>│                    │
    │                    │                        │ onEvent()          │
    │                    │                        │ handleMessage()    │
    │                    │                        │ ├──────────────────>│
    │                    │                        │                    │ handler()
    │                    │                        │                    │ 业务逻辑
    │                    │                        │                    │<───────────┤
    │                    │                        │ clear()            │
    │                    │                        │                    │
    │ 完成               │                        │ 等待下一个事件       │
    │                    │                        │                    │
```

## 5. 性能优势详解

### 5.1 为什么 Disruptor 这么快？

**传统 BlockingQueue:**
```
┌──────────────┐     lock      ┌──────────────┐
│  生产者线程   │ ──────────>  │  队列         │
│              │   阻塞等待    │              │
└──────────────┘              └──────────────┘
      ↑                             ↓
      │                         lock      ┌──────────────┐
      └──────────────── 阻塞等待 ──────>  │  消费者线程   │
                                              │              │
                                              └──────────────┘

问题:
1. 锁竞争严重
2. 线程阻塞/唤醒开销大
3. 频繁创建/销毁对象
4. 缓存不友好（链表结构）
```

**Disruptor:**
```
┌──────────────┐     CAS      ┌──────────────────────────┐
│  生产者线程   │ ──────────>  │  RingBuffer (环形数组)    │
│              │   无锁       │  - 预分配内存              │
└──────────────┘              │  - 缓存友好                │
      ↑                       │  - 无锁设计                │
      │                       └──────────────────────────┘
      │                                 ↑
      │                        sequence │
      │                                 │
      │                        ┌──────────────────────────┐
      └───────────────────────│  消费者线程 (一直运行)     │
                               │  - 等待可用序列           │
                               │  - 批量处理               │
                               └──────────────────────────┘

优势:
1. 无锁竞争（CAS 操作）
2. 无线程阻塞（一直运行）
3. 对象复用（GC 压力小）
4. 缓存友好（数组结构）
5. 批处理（提高吞吐量）
```

### 5.2 性能对比数据

| 指标 | BlockingQueue | Disruptor | 提升 |
|------|--------------|-----------|------|
| 吞吐量 | 1-5M ops/s | 10-50M ops/s | **10-100x** |
| 延迟 (99%) | 1-5ms | 10-100μs | **10-100x** |
| GC 压力 | 高 | 低 | **显著降低** |

## 6. 调试和监控

### 6.1 添加日志查看执行流程

```java
// 在 BusinessEventHandler.onEvent() 中添加日志
@Override
public void onEvent(BusinessEvent event, long sequence, boolean endOfBatch) {
    long startTime = System.nanoTime();
    try {
        LOGGER.debug("[{}] 开始处理事件: {}, sequence={}",
            Thread.currentThread().getName(),
            event.getEventType(),
            sequence);

        handleMessage(event);

        long endTime = System.nanoTime();
        LOGGER.debug("[{}] 处理完成, 耗时={}μs",
            Thread.currentThread().getName(),
            (endTime - startTime) / 1000);

    } catch (Exception e) {
        LOGGER.error("处理事件异常", e);
    } finally {
        event.clear();
    }
}
```

### 6.2 查看线程状态

```java
// 查看所有 Disruptor 线程
Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
allThreads.entrySet().stream()
    .filter(e -> e.getKey().getName().startsWith("disruptor-pool-"))
    .forEach(e -> {
        Thread thread = e.getKey();
        StackTraceElement[] stack = e.getValue();
        System.out.println(thread.getName() + ": " + thread.getState());
        for (StackTraceElement element : stack) {
            System.out.println("\t" + element);
        }
    });
```

### 6.3 监控 RingBuffer 状态

```java
DisruptorBusinessPool pool = DisruptorBusinessPool.getInstance();

// 查看总体状态
System.out.println(pool.getPoolStatus());
// DisruptorBusinessPool[disruptorCount=16, published=1000000, handled=999998, remaining=1024]

// 查看详细状态
System.out.println(pool.getDetailedStatus());
// DisruptorBusinessPool Detail:
//   Disruptor[00]: cursor=12345, published=12345, remaining=1024
//   Disruptor[01]: cursor=23456, published=23456, remaining=1024
//   ...
```

## 7. 常见问题

### Q1: 如何确保同一玩家的消息顺序？
**A:** 通过 userId 路由到固定的 Disruptor，每个 Disruptor 单线程处理，保证顺序。

### Q2: 如果一个玩家的消息处理很慢，会影响其他玩家吗？
**A:** 不会。不同玩家的消息路由到不同的 Disruptor，互不影响。

### Q3: RingBuffer 满了怎么办？
**A:** Disruptor 会阻塞生产者（根据等待策略），直到有空位。可以通过增大 RingBuffer 或提高消费者速度来解决。

### Q4: 如何调整性能？
**A:**
- 增加 DISRUPTOR_COUNT（更多 Disruptor，更多并行度）
- 增大 RING_BUFFER_SIZE（更大的缓冲区）
- 更换 WaitStrategy（不同策略影响延迟和吞吐量）

希望这个详细的流程解释能帮助你理解 Disruptor 的工作原理！