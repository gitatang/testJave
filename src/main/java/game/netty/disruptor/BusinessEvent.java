package game.netty.disruptor;

import game.netty.handler.logic.ProtoMessage;
import game.netty.session.Session;
import io.netty.channel.ChannelHandlerContext;

/**
 * Disruptor 业务事件
 *
 * 设计理念：
 * 1. 使用对象池复用 Event 对象，减少 GC 压力
 * 2. 每个 Event 包含处理业务逻辑所需的所有信息
 * 3. 通过 userId 路由到不同的 Disruptor 实例，保证顺序性
 */
public class BusinessEvent {
    // 事件类型
    private EventType eventType;
    // Session 信息
    private Session session;
    // ChannelHandlerContext（用于获取 channel）
    private ChannelHandlerContext ctx;
    // ProtoMessage 消息
    private ProtoMessage protoMessage;
    // 业务任务（Runnable）
    private Runnable task;
    // 用户ID（用于路由）
    private String userId;
    // Channel Key（用于路由，未登录时使用）
    private Object channelKey;
    // 时间戳
    private long timestamp;

    public enum EventType {
        MESSAGE_HANDLE,    // 处理消息
        TASK_EXECUTE,      // 执行任务
        HEARTBEAT          // 心跳检测
    }

    /**
     * 清空事件数据（用于对象池复用）
     */
    public void clear() {
        this.eventType = null;
        this.session = null;
        this.ctx = null;
        this.protoMessage = null;
        this.task = null;
        this.userId = null;
        this.channelKey = null;
        this.timestamp = 0;
    }

    /**
     * 设置消息处理事件
     */
    public void setMessageEvent(Session session, ChannelHandlerContext ctx, ProtoMessage protoMessage) {
        this.eventType = EventType.MESSAGE_HANDLE;
        this.session = session;
        this.ctx = ctx;
        this.protoMessage = protoMessage;
        this.userId = session != null ? session.getUserId() : null;
        this.channelKey = ctx != null ? ctx.channel().id() : null;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 设置任务执行事件
     */
    public void setTaskEvent(String userId, Object channelKey, Runnable task) {
        this.eventType = EventType.TASK_EXECUTE;
        this.userId = userId;
        this.channelKey = channelKey;
        this.task = task;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public EventType getEventType() {
        return eventType;
    }

    public Session getSession() {
        return session;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public ProtoMessage getProtoMessage() {
        return protoMessage;
    }

    public Runnable getTask() {
        return task;
    }

    public String getUserId() {
        return userId;
    }

    public Object getChannelKey() {
        return channelKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "BusinessEvent{" +
                "eventType=" + eventType +
                ", userId='" + userId + '\'' +
                ", sessionId=" + (session != null ? session.getSessionId() : "null") +
                ", timestamp=" + timestamp +
                '}';
    }
}