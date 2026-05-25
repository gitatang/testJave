package game.netty.handler.logic;

import com.google.protobuf.MessageLite;
import game.netty.session.Session;
import game.netty.session.SessionManager;
import game.netty.thread.DisruptorBusinessPool;
import game.protocol.MessageHandler;
import game.protocol.MessageRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 游戏逻辑处理器（Disruptor 版本）
 *
 * 职责：
 * 1. 将接收到的消息分发到 Disruptor 业务线程池进行处理
 * 2. 保证同一玩家的消息顺序执行（通过 userId 路由到同一个 Disruptor）
 * 3. EventLoop 线程只负责消息的读取，不执行业务逻辑
 *
 * 架构：
 * EventLoop 线程 → GameLogicHandler → DisruptorBusinessPool（按玩家路由）→ BusinessEventHandler
 *
 * 性能优势：
 * - Disruptor 无锁设计，性能极高
 * - 环形数组，GC 压力小
 * - 预分配内存，减少对象创建
 * - 相比 BlockingQueue 性能提升 10-100 倍
 */
public class GameLogicHandler extends SimpleChannelInboundHandler<ProtoMessage> {
    private static final Logger log = LoggerFactory.getLogger(GameLogicHandler.class);
    private static final DisruptorBusinessPool disruptorPool = DisruptorBusinessPool.getInstance();
    private static final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtoMessage protoMsg) throws Exception {
        // 获取当前会话
        Session session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            log.warn("会话不存在，关闭连接, channel={}", ctx.channel().id().asShortText());
            ctx.close();
            return;
        }

        // 更新心跳时间（在 EventLoop 线程中执行，快速）
        session.updateHeartbeat();

        // 从 EventLoop 线程切换到 Disruptor 业务线程池执行业务逻辑
        // 关键：根据 userId 路由，保证同一玩家的消息顺序执行
        disruptorPool.submitMessage(session, ctx, protoMsg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接, channel={}", ctx.channel().id().asShortText());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开, channel={}", ctx.channel().id().asShortText());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("连接异常, channel={}, error={}",
                ctx.channel().id().asShortText(), cause.getMessage(), cause);
        ctx.close();
    }
}