package game.netty.disruptor;

import com.google.protobuf.MessageLite;
import game.netty.handler.logic.ProtoMessage;
import game.netty.session.Session;
import game.netty.session.SessionManager;
import game.protocol.MessageHandler;
import game.protocol.MessageRegistry;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Disruptor 事件处理器
 * 在业务线程中处理业务逻辑
 *
 * 特点：
 * 1. 单线程处理，保证同一 Disruptor 中的消息顺序执行
 * 2. 无锁设计，性能极高
 * 3. 使用环形数组，GC 压力小
 */
public class BusinessEventHandler implements com.lmax.disruptor.EventHandler<BusinessEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessEventHandler.class);
    private static final SessionManager sessionManager = SessionManager.getInstance();

    private final String disruptorName;

    public BusinessEventHandler(String disruptorName) {
        this.disruptorName = disruptorName;
    }

    @Override
    public void onEvent(BusinessEvent event, long sequence, boolean endOfBatch) throws Exception {
        try {
            switch (event.getEventType()) {
                case MESSAGE_HANDLE:
                    handleMessage(event);
                    break;
                case TASK_EXECUTE:
                    handleTask(event);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(event);
                    break;
                default:
                    LOGGER.warn("未知的事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            LOGGER.error("处理事件异常, disruptor={}, event={}",
                    disruptorName, event, e);
        } finally {
            // 清空事件数据，便于对象复用
            event.clear();
        }
    }

    /**
     * 处理消息事件
     */
    private void handleMessage(BusinessEvent event) {
        Session session = event.getSession();
        ChannelHandlerContext ctx = event.getCtx();
        ProtoMessage protoMsg = event.getProtoMessage();

        if (session == null) {
            LOGGER.warn("Session 为空，无法处理消息");
            return;
        }

        try {
            byte version = protoMsg.getVersion();
            byte type = protoMsg.getType();
            MessageLite rawMsg = protoMsg.getMessage();

            LOGGER.debug("处理消息, disruptor={}, sessionId={}, userId={}, version={}, type={}",
                    disruptorName, session.getSessionId(), session.getUserId(), version, type);

            MessageHandler<?> handler = MessageRegistry.getHandler(version, type);
            if (handler == null) {
                LOGGER.error("找不到消息处理器, version={}, type={}", version, type);
                return;
            }

            // 类型校验 + 安全转换
            if (!handler.messageClass().isInstance(rawMsg)) {
                LOGGER.error("消息类型不匹配, expect={}, actual={}",
                        handler.messageClass(), rawMsg.getClass());
                return;
            }

            // 强制转换（安全）
            MessageLite typedMsg = handler.messageClass().cast(rawMsg);

            // 反射调用 handle
            Method handleMethod = handler.getClass()
                    .getMethod("handler", ChannelHandlerContext.class, typedMsg.getClass());
            handleMethod.invoke(handler, ctx, typedMsg);

        } catch (Exception e) {
            LOGGER.error("处理业务逻辑异常, disruptor={}, sessionId={}, userId={}",
                    disruptorName, session.getSessionId(), session.getUserId(), e);
        }
    }

    /**
     * 处理任务事件
     */
    private void handleTask(BusinessEvent event) {
        Runnable task = event.getTask();
        if (task != null) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("执行任务异常, disruptor={}, userId={}",
                        disruptorName, event.getUserId(), e);
            }
        }
    }

    /**
     * 处理心跳事件
     */
    private void handleHeartbeat(BusinessEvent event) {
        // 预留心跳处理逻辑
        LOGGER.debug("处理心跳事件, disruptor={}, userId={}",
                disruptorName, event.getUserId());
    }
}
