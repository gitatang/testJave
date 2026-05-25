package game.protocol;

import com.game.UserProto;
import com.google.protobuf.MessageLite;
import game.netty.enums.MessageType;
import game.netty.handler.logic.LoginReqHandler;
import game.netty.handler.logic.LoginReqV2Handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageRegistry {
	private static final Map<MessageMeta, Class<? extends MessageLite>> MSG_CLASS = new ConcurrentHashMap<>();
	private static final Map<MessageMeta, MessageHandler<?>> HANDLERS = new ConcurrentHashMap<>();

	public static <T extends MessageLite> void register(byte type, Class<T> clazz, MessageHandler<T> handler) {
		register((byte) 1,type, clazz, handler);
	}



	public static <T extends MessageLite> void register(byte version, byte type, Class<T> clazz, MessageHandler<T> handler) {
		MessageMeta key = new MessageMeta(version, type);
		MSG_CLASS.put(key, clazz);
		HANDLERS.put(key, handler);
	}


	/* ========== 获取 ========== */

	public static Class<? extends MessageLite> getMessageClass(byte version, byte type) {
		return MSG_CLASS.get(new MessageMeta(version, type));
	}

	@SuppressWarnings("unchecked")
	public static <T extends MessageLite> MessageHandler<T> getHandler(
			byte version, byte type) {
		return (MessageHandler<T>) HANDLERS.get(new MessageMeta(version, type));
	}

	public record MessageMeta(byte version, byte type) {}
	static {
		register(MessageType.LOGIN_REQ.getType(), UserProto.LoginReq.class, new LoginReqHandler());
		register((byte)2, MessageType.LOGIN_REQ_V2.getType(), UserProto.LoginReqV2.class, new LoginReqV2Handler());
	}


}
