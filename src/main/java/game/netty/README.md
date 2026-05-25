# Netty 游戏服务器框架

## 架构设计

本框架采用 **IO 线程与业务线程分离** 的架构模式，并保证**玩家消息顺序执行**：

```
┌─────────────────────────────────────────────────────────────────┐
│                         Netty Server                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐      ┌─────────────────────────────────┐     │
│  │ Boss Group   │      │ Worker Group (IO线程)            │     │
│  │  (1线程)     │  →   │  - 接受连接                      │     │
│  │ 接受连接     │      │  - 读写数据                      │     │
│  └──────────────┘      │  - 不处理业务逻辑                │     │
│                        └─────────────────────────────────┘     │
│                                        ↓                        │
│                        ┌─────────────────────────────────┐     │
│                        │ GameLogicHandler                │     │
│                        │ - 按 userId 路由                │     │
│                        └─────────────────────────────────┘     │
│                                        ↓                        │
│                        ┌─────────────────────────────────┐     │
│                        │ BusinessThreadPool              │     │
│                        │ ┌─────────┐ ┌─────────┐        │     │
│                        │ │线程池[0]│ │线程池[1]│ ...    │     │
│                        │ │单线程队列│ │单线程队列│        │     │
│                        │ └─────────┘ └─────────┘        │     │
│                        │                                 │     │
│                        │ 路由策略：                      │     │
│                        │ userId.hashCode() % 线程数      │     │
│                        │                                 │     │
│                        │ 保证：                          │     │
│                        │ ✓ 同玩家消息顺序执行            │     │
│                        │ ✓ 不同玩家可并行执行            │     │
│                        └─────────────────────────────────┘     │
│                                        ↓                        │
│                    ┌──────────────────────────────────┐       │
│                    │  Session Manager                 │       │
│                    │  - 会话管理                       │       │
│                    │  - 消息广播                       │       │
│                    └──────────────────────────────────┘       │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## 核心特性

### 1. 玩家消息顺序性保证 ⭐

**问题**：在网络游戏中，同一个玩家的消息必须严格按顺序执行，否则会出现严重问题：
- 移动指令：先发 A→B，后发 B→C，如果反序执行会变成 A→C→B
- 战斗指令：先攻击，后使用技能，如果反序会违反游戏逻辑
- 交易指令：先下单，后付款，如果反序会导致资金问题

**解决方案**：
- 根据玩家 ID 进行路由，保证同一玩家的所有消息都路由到同一个业务线程
- 每个业务线程使用单线程队列，保证消息顺序执行
- 不同玩家的消息可以并行执行，提高吞吐量

**路由策略**：
```java
// 根据 userId 计算路由索引
int threadIndex = Math.abs(userId.hashCode()) % threadCount;

// 同一个 userId 始终路由到同一个线程
businessThreadPool.submit(userId, channelKey, task);
```

### 2. EventLoop 与业务线程分离
- **EventLoop 线程**：只负责 IO 操作（连接、读写），不被业务逻辑阻塞
- **业务线程池**：处理所有业务逻辑，不影响 IO 操作

### 3. Session 管理
- 自动创建和销毁 Session
- 支持用户绑定与解绑
- 支持重复登录踢出旧连接
- 心跳检测与超时处理

### 4. 消息广播
- 向所有在线用户广播
- 向指定用户列表群发
- 自动过滤不活跃会话

### 5. 优雅关闭
- 支持关闭钩子
- 按顺序关闭各个组件
- 等待业务线程池处理完任务

## 使用方法

### 启动服务器

```java
public static void main(String[] args) {
    int port = 6000;
    NettyServer server = new NettyServer(port);

    // 添加关闭钩子
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        server.shutdown();
    }));

    try {
        server.start();
        server.serverChannel.closeFuture().sync();
    } catch (InterruptedException e) {
        server.shutdown();
    }
}
```

### 实现消息处理器

```java
public class YourHandler implements MessageHandler<YourProtoMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(YourHandler.class);
    private static final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public Class<YourProtoMessage> messageClass() {
        return YourProtoMessage.class;
    }

    @Override
    public void handler(ChannelHandlerContext ctx, YourProtoMessage msg) {
        // 获取当前 Session（在业务线程中执行）
        Session session = sessionManager.getSession(ctx.channel());

        // 处理业务逻辑...
        // 注意：这里在业务线程中执行，与同一玩家的其他消息是顺序执行的

        // 发送响应
        session.sendMessage(version, type, requestId, response);
    }
}
```

### Session 使用示例

#### 1. 发送消息给单个用户

```java
Session session = sessionManager.getSessionByUserId("username");
if (session != null && session.isActive()) {
    session.sendMessage(version, type, requestId, message);
}
```

#### 2. 广播消息给所有在线用户

```java
sessionManager.broadcast(version, type, requestId, message);
```

#### 3. 群发消息给指定用户列表

```java
List<String> userIds = List.of("user1", "user2", "user3");
sessionManager.sendToUsers(version, type, requestId, message, userIds);
```

#### 4. 获取在线用户列表

```java
List<String> onlineUsers = sessionManager.getOnlineUsers();
```

#### 5. 绑定用户到 Session

```java
Session session = sessionManager.getSession(channel);
sessionManager.bindUser(session, "username");
// 绑定后，该玩家的消息将按 userId 路由
```

### 注册消息处理器

在 `MessageRegistry` 中注册：

```java
static {
    register(
        MessageType.YOUR_MESSAGE.getType(),  // 消息类型
        YourProtoMessage.class,              // Proto 消息类
        new YourHandler()                    // 处理器实例
    );
}
```

## 核心组件详解

### BusinessThreadPool（业务线程池）

**设计特点**：
- 多个单线程的 ExecutorService 数组
- 根据 userId 路由到对应的线程
- 保证同一玩家的消息顺序执行
- 不同玩家的消息可并行执行

**配置**：
```java
// 线程数：CPU 核心数 × 2（至少 8 个）
private static final int THREAD_COUNT = Math.max(
    Runtime.getRuntime().availableProcessors() * 2, 8
);

// 每个线程的队列容量
private static final int QUEUE_CAPACITY_PER_THREAD = 1000;
```

**使用示例**：
```java
BusinessThreadPool pool = BusinessThreadPool.getInstance();

// 提交任务（自动按 userId 路由）
pool.submit(userId, channelKey, () -> {
    // 业务逻辑
    // 同一个 userId 的任务会顺序执行
});
```

**状态监控**：
```java
// 获取总体状态
String status = pool.getPoolStatus();
// BusinessThreadPool[threadCount=16, active=8, queue=100, completed=1000]

// 获取详细状态（每个线程）
String detail = pool.getDetailedStatus();
```

### SessionManager（会话管理器）

**主要功能**：
- 会话生命周期管理
- 用户绑定与解绑
- 消息广播与群发
- 心跳检测与超时处理

**心跳配置**：
```java
// 空闲检测：60秒无读操作触发空闲事件
new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS);

// 心跳超时：120秒无心跳自动断开
private long timeoutMillis = 120000;

// 检查频率：每30秒检查一次
scheduleAtFixedRate(..., 30, 30, TimeUnit.SECONDS);
```

## 性能调优

### 1. 业务线程池配置

在 `BusinessThreadPool` 中调整：

```java
// 线程数（建议：CPU 核心数 × 2 到 × 4）
private static final int THREAD_COUNT =
    Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);

// 每个线程的队列容量
private static final int QUEUE_CAPACITY_PER_THREAD = 1000;
```

**调优建议**：
- CPU 密集型：线程数 = CPU 核心数 + 1
- IO 密集型：线程数 = CPU 核心数 × 2
- 游戏（混合型）：线程数 = CPU 核心数 × 2~4

### 2. 心跳超时配置

```java
// 空闲检测时间（NettyServer）
new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS);  // 60秒

// 心跳超时时间（SessionManager）
private long timeoutMillis = 120000;  // 120秒

// 检查频率（SessionManager）
scheduleAtFixedRate(..., 30, 30, TimeUnit.SECONDS);  // 30秒
```

### 3. TCP 参数配置

```java
// NettyServer 中的配置
.option(ChannelOption.SO_BACKLOG, 128)           // 连接队列大小
.option(ChannelOption.SO_REUSEADDR, true)        // 端口重用
.childOption(ChannelOption.SO_KEEPALIVE, true)   // 保持连接
.childOption(ChannelOption.TCP_NODELAY, true)    // 禁用 Nagle 算法
```

## 顺序性验证

框架提供了测试类来验证玩家消息的顺序性：

```java
// 运行测试
java game.netty.thread.SequentialTest
```

**测试内容**：
1. 同一玩家消息顺序性测试
2. 不同玩家消息并行性测试
3. 线程池状态测试
4. 路由一致性测试

**测试结果示例**：
```
========== 测试1：同一玩家消息顺序性 ==========
执行顺序: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
是否顺序执行: ✓ 是
成功执行: 10/10

========== 测试2：不同玩家消息并行性 ==========
总耗时: 1012ms
是否并行执行: ✓ 是
```

## 注意事项

### 1. 业务处理器中的注意事项

**✓ 推荐做法**：
```java
@Override
public void handler(ChannelHandlerContext ctx, YourProtoMessage msg) {
    Session session = sessionManager.getSession(ctx.channel());

    // 处理业务逻辑...
    // 这里的代码与同一玩家的其他消息是顺序执行的

    // 发送响应
    session.sendMessage(version, type, requestId, response);
}
```

**✗ 避免的做法**：
```java
// 不要在业务处理器中执行长时间阻塞操作
@Override
public void handler(ChannelHandlerContext ctx, YourProtoMessage msg) {
    // ✗ 错误：长时间阻塞会延迟同一玩家的其他消息
    Thread.sleep(10000);

    // ✗ 错误：同步等待其他服务响应
    Future<Response> future = someService.call();
    Response response = future.get();  // 阻塞等待
}
```

**正确的异步处理方式**：
```java
@Override
public void handler(ChannelHandlerContext ctx, YourProtoMessage msg) {
    Session session = sessionManager.getSession(ctx.channel());

    // 使用 CompletableFuture 处理异步操作
    CompletableFuture.supplyAsync(() -> {
        return someService.call();
    }).thenAcceptAsync(response -> {
        // 注意：这里需要重新提交到业务线程池
        businessThreadPool.submit(
            session.getUserId(),
            session.getChannel().id(),
            () -> {
                session.sendMessage(version, type, requestId, response);
            }
        );
    });
}
```

### 2. ChannelHandlerContext 的使用

**安全的使用方式**：
```java
// ✓ 安全：只用于获取 channel 和属性
Channel channel = ctx.channel();
AttributeKey<String> key = AttributeKey.valueOf("key");
String value = ctx.attr(key).get();
```

**不安全的使用方式**：
```java
// ✗ 不安全：不要在业务线程中直接写入数据
ctx.writeAndFlush(message);  // 可能导致线程安全问题

// ✓ 正确：使用 Session 发送
session.sendMessage(version, type, requestId, message);
```

### 3. 消息发送的线程安全

`Session.sendMessage()` 方法已经处理了线程安全问题：
```java
public void sendMessage(byte version, byte type, long requestId, MessageLite message) {
    // 自动切换到 EventLoop 线程执行
    channel.eventLoop().execute(() -> {
        channel.writeAndFlush(new ProtoMessage<>(...));
    });
}
```

## 架构优势

### 1. 顺序性保证
- 同一玩家的消息严格按顺序执行
- 避免了游戏逻辑混乱

### 2. 高性能
- EventLoop 线程只负责 IO，不被业务逻辑阻塞
- 不同玩家的消息可并行处理
- 充分利用多核 CPU

### 3. 可扩展性
- 线程数可配置
- 队列容量可调整
- 易于监控和调优

### 4. 可靠性
- 心跳检测自动清理僵尸连接
- 优雅关闭保证数据完整性
- 完善的异常处理

## 项目结构

```
game/
├── netty/
│   ├── handler/
│   │   ├── connection/              # 连接管理
│   │   │   └── SessionManagementHandler.java
│   │   ├── logic/                   # 业务逻辑
│   │   │   ├── GameLogicHandler.java
│   │   │   ├── ProtoMessage.java
│   │   │   └── LoginReqV2Handler.java
│   │   ├── GameDecodeHandler.java
│   │   └── GameEncodeHandler.java
│   ├── server/
│   │   └── NettyServer.java
│   ├── session/
│   │   ├── Session.java             # 会话对象
│   │   └── SessionManager.java      # 会话管理器
│   ├── thread/
│   │   ├── BusinessThreadPool.java  # 业务线程池（顺序化版本）
│   │   └── SequentialTest.java      # 顺序性测试
│   └── README.md                    # 本文档
└── protocol/
    ├── MessageHandler.java
    └── MessageRegistry.java
```

## 扩展建议

1. **分布式支持**：使用 Redis 存储 Session，支持多服务器部署
2. **消息过滤**：敏感词过滤、频率限制等
3. **监控告警**：集成 Prometheus、Grafana 等监控工具
4. **限流降级**：防止恶意连接和消息轰炸
5. **消息加密**：加密传输，保护数据安全
6. **负载均衡**：实现更智能的玩家路由策略