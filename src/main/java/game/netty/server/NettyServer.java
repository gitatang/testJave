package game.netty.server;

import game.netty.handler.GameDecodeHandler;
import game.netty.handler.GameEncodeHandler;
import game.netty.handler.connection.SessionManagementHandler;
import game.netty.handler.logic.GameLogicHandler;
import game.netty.session.SessionManager;
import game.netty.thread.DisruptorBusinessPool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Netty 服务器
 *
 * 架构设计：
 * 1. EventLoop 线程只负责 IO 操作（连接、读写）
 * 2. 业务逻辑使用 Disruptor 高性能线程池处理
 * 3. Session 管理器负责管理所有客户端连接
 * 4. 支持消息广播功能
 * 5. 保证同一玩家消息顺序执行
 *
 * 性能优势：
 * - Disruptor 无锁设计，性能比 BlockingQueue 高 10-100 倍
 * - 环形数组，GC 压力小
 * - 预分配内存，减少对象创建
 */
public class NettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = 6000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.warn("无效的端口号参数，使用默认端口 6000");
            }
        }

        NettyServer server = new NettyServer(port);

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("接收到关闭信号，开始关闭服务器...");
            server.shutdown();
        }));

        try {
            server.start();
            LOGGER.info("服务器启动成功，按 Ctrl+C 停止服务器");

            // 阻塞主线程，保持服务器运行
            server.serverChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("服务器被中断", e);
            Thread.currentThread().interrupt();
        } finally {
            server.shutdown();
        }
    }

    /**
     * 启动服务器
     */
    public void start() throws InterruptedException {
        // 使用 Epoll（Linux）或 NIO（跨平台）
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            LOGGER.info("使用 Epoll EventLoopGroup");
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            LOGGER.info("使用 NIO EventLoopGroup");
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 空闲检测：60秒没有读操作就触发事件
                            pipeline.addLast("idleStateHandler", new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));

                            // Session 管理（必须放在第一个）
                            pipeline.addLast("sessionManagement", new SessionManagementHandler());

                            // 编解码器
                            pipeline.addLast("decode", new GameDecodeHandler());
                            pipeline.addLast("encode", new GameEncodeHandler());

                            // 业务逻辑处理器
                            pipeline.addLast("gameLogic", new GameLogicHandler());
                        }
                    });

            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            LOGGER.info("=================================================");
            LOGGER.info("Netty 服务器启动成功!");
            LOGGER.info("端口: {}", port);
            LOGGER.info("传输协议: TCP");
            LOGGER.info("架构模式: IO线程与业务线程分离（Disruptor）");
            LOGGER.info("Disruptor数量: {}", DisruptorBusinessPool.getInstance().getDisruptorCount());
            LOGGER.info("环形缓冲区大小: {}", DisruptorBusinessPool.getInstance().getRingBufferSize());
            LOGGER.info("=================================================");
        } catch (Exception e) {
            LOGGER.error("服务器启动失败", e);
            shutdown();
            throw e;
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        LOGGER.info("开始关闭服务器...");

        // 关闭 Server Channel
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                LOGGER.error("关闭 Server Channel 时被中断", e);
                Thread.currentThread().interrupt();
            }
        }

        // 关闭 Session Manager
        SessionManager.getInstance().shutdown();

        // 关闭 Disruptor 业务线程池
        DisruptorBusinessPool.getInstance().shutdown();

        // 关闭 EventLoopGroup
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }

        LOGGER.info("服务器已关闭");
    }

    /**
     * 获取服务器状态信息
     */
    public String getStatus() {
        return String.format(
                "Server[port=%d, onlineSessions=%d, disruptorPool=%s]",
                port,
                SessionManager.getInstance().getOnlineSessionCount(),
                DisruptorBusinessPool.getInstance().getPoolStatus()
        );
    }
}
