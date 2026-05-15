package game.netty.handler.logic;

import game.protocol.Message;
import game.protocol.MessageHandler;
import game.protocol.MessageRegistry;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class GameLogicHandler extends SimpleChannelInboundHandler<Message> {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameLogicHandler.class);
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
		MessageHandler<Message> handler =
				MessageRegistry.getHandler(msg.getVersion(), msg.getType());

		if (handler == null) {
			log.warn("No handler for {}_{}", msg.getVersion(), msg.getType());
			return;
		}

		try {
			handler.handler(ctx, msg);
		} catch (Exception e) {
			log.error("Handle error", e);
		}
	}
}
