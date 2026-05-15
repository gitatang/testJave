package game.protocol;

import game.netty.enums.MessageType;
import game.netty.handler.LoginReqHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRegistry {
	private static final Map<MessageMeta, Class<? extends Message>> MSG_CLASS = new ConcurrentHashMap<>();
	private static final Map<MessageMeta, MessageHandler<?>> HANDLERS = new ConcurrentHashMap<>();

	public static <T extends Message> void register(byte type, Class<T> clazz, MessageHandler<T> handler) {
		register((byte) 1, clazz, handler);
	}

	public static <T extends Message> void register(byte version, byte type, Class<T> clazz, MessageHandler<T> handler) {
		MessageMeta key = new MessageMeta(version, type);
		MSG_CLASS.put(key, clazz);
		HANDLERS.put(key, handler);
	}


	/* ========== 获取 ========== */

	public static Class<? extends Message> getMessageClass(byte version, byte type) {
		return MSG_CLASS.get(new MessageMeta(version, type));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Message> MessageHandler<T> getHandler(
			byte version, byte type) {
		return (MessageHandler<T>) HANDLERS.get(new MessageMeta(version, type));
	}

	public record MessageMeta(byte version, byte type) {}
	static {
		register(MessageType.LOGIN_REQ.getType(), LoginReq.class, new LoginReqHandler());
	}
}
