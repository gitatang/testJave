package game.netty.handler;

import game.protocol.LoginReq;
import game.protocol.MessageHandler;
import io.netty.channel.ChannelHandlerContext;

public class LoginReqHandler implements MessageHandler<LoginReq> {


	@Override
	public void handler(ChannelHandlerContext ctx, LoginReq msg) {

	}
}
