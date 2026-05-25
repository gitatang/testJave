package game.netty.handler.logic;

import com.google.protobuf.MessageLite;

public class ProtoMessage<T extends MessageLite> {
	private final byte version;
	private final byte type;
	private final long requestId;
	private final T message;
	private final Class clazz;

	public ProtoMessage(byte version, byte type, long requestId, T message,Class clazz) {
		this.version = version;
		this.type = type;
		this.requestId = requestId;
		this.message = message;
		this.clazz = clazz;
	}

	public byte getVersion() {
		return version;
	}

	public byte getType() {
		return type;
	}

	public long getRequestId() {
		return requestId;
	}

	public T getMessage() {
		return message;
	}

	public Class getClazz() {
		return clazz;
	}
}
