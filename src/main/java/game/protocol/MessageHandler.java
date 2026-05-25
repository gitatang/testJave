package game.protocol;

import com.google.protobuf.MessageLite;
import io.netty.channel.ChannelHandlerContext;

public interface MessageHandler<T extends MessageLite> {
	/** 返回消息类（用于类型校验） */
	Class<T> messageClass();
	void handler(ChannelHandlerContext ctx, T msg);


}
