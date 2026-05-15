package game.protocol;

public interface Message {
	byte getVersion();
	byte getType();
	long getRequestId();
}
