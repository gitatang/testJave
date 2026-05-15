package game.netty.handler;

import game.protocol.MessageBase;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import util.JsonUtil;

public class GameEncodeHandler extends MessageToByteEncoder<MessageBase> {
	@Override
	protected void encode(ChannelHandlerContext ctx, MessageBase messageBase, ByteBuf out) throws Exception {
		JsonUtil.write(messageBase, out);
	}
}
