package game.protocol;

import game.netty.enums.MessageType;

public abstract class MessageBase implements Message{
	protected byte version = 1;
	protected MessageType type;
	protected long requestId;

	@Override
	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}



	public void setType(MessageType type) {
		this.type = type;
	}

	@Override
	public byte getType() {
		return type.getType();
	}
	@Override
	public long getRequestId() {
		return requestId;
	}

	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}
}
