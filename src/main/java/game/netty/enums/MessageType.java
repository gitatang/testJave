package game.netty.enums;

public enum MessageType {
	HEARTBEAT((byte) 1),
	LOGIN_REQ((byte) 2),
	LOGIN_RESP((byte) 3),
	CHAT((byte) 4),
	LOGIN_REQ_V2((byte) 5);

	private byte type;
	MessageType(byte type) {
		this.type = type;
	}
	public byte getType() {
		return type;
	}
	public static MessageType getMessageType(byte type) {
		for (MessageType messageType : MessageType.values()) {
			if (messageType.getType() == type) {
				return messageType;
			}
		}
		return null;
	}
}