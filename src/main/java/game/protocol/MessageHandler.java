package game.protocol;

import io.netty.channel.ChannelHandlerContext;

public interface MessageHandler<T extends Message> {

	void handler(ChannelHandlerContext ctx, T msg);
}
