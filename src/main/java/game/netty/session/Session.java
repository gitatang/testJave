package game.netty.session;

import com.google.protobuf.MessageLite;
import game.netty.handler.logic.ProtoMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话对象，封装了客户端连接的上下文信息
 */
public class Session {
    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final long sessionId;
    private final Channel channel;
    private volatile String userId;
    private volatile long lastHeartbeatTime;

    public Session(Channel channel) {
        this.sessionId = ID_GENERATOR.incrementAndGet();
        this.channel = channel;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    /**
     * 发送消息
     */
    public void sendMessage(byte version, byte type, long requestId, MessageLite message) {
        if (channel == null || !channel.isActive()) {
            LOGGER.warn("尝试向不活跃的会话发送消息, sessionId={}", sessionId);
            return;
        }

        channel.eventLoop().execute(() -> {
            try {
                channel.writeAndFlush(new ProtoMessage<>(version, type, requestId, message, message.getClass()))
                        .addListener((ChannelFutureListener) future -> {
                            if (!future.isSuccess()) {
                                LOGGER.error("发送消息失败, sessionId={}, error={}",
                                        sessionId, future.cause().getMessage());
                            }
                        });
            } catch (Exception e) {
                LOGGER.error("发送消息异常, sessionId={}", sessionId, e);
            }
        });
    }

    /**
     * 关闭会话
     */
    public void close() {
        if (channel != null && channel.isActive()) {
            channel.close().addListener(future -> {
                if (future.isSuccess()) {
                    LOGGER.info("会话关闭成功, sessionId={}, userId={}", sessionId, userId);
                } else {
                    LOGGER.warn("会话关闭失败, sessionId={}, userId={}", sessionId, userId);
                }
            });
        }
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    /**
     * 检查是否超时
     */
    public boolean isTimeout(long timeoutMillis) {
        return System.currentTimeMillis() - lastHeartbeatTime > timeoutMillis;
    }

    // Getters and Setters
    public long getSessionId() {
        return sessionId;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", userId='" + userId + '\'' +
                ", active=" + isActive() +
                ", lastHeartbeat=" + lastHeartbeatTime +
                '}';
    }
}