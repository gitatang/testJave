package game.netty.handler.connection;

import game.netty.session.Session;
import game.netty.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接管理 Handler
 * 负责在连接建立和断开时管理 Session
 * 在 Pipeline 中应该放在第一个位置
 */
public class SessionManagementHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagementHandler.class);
    private static final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 创建新会话
        Session session = sessionManager.addSession(ctx.channel());
        LOGGER.info("客户端连接成功, sessionId={}, channel={}, remoteAddress={}",
                session.getSessionId(),
                ctx.channel().id().asShortText(),
                ctx.channel().remoteAddress());

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 移除会话
        Session session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            LOGGER.info("客户端断开连接, sessionId={}, userId={}, channel={}",
                    session.getSessionId(), session.getUserId(), ctx.channel().id().asShortText());
            sessionManager.removeSession(session);
        } else {
            LOGGER.warn("断开连接时找不到会话, channel={}", ctx.channel().id().asShortText());
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("连接发生异常, channel={}, error={}",
                ctx.channel().id().asShortText(), cause.getMessage(), cause);

        Session session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            LOGGER.error("异常会话信息, sessionId={}, userId={}",
                    session.getSessionId(), session.getUserId());
        }

        ctx.close();
    }

    /**
     * 用户触发的事件（如心跳超时）
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 这里可以处理 IdleStateHandler 触发的事件
        if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
            LOGGER.warn("连接空闲超时, channel={}", ctx.channel().id().asShortText());
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}