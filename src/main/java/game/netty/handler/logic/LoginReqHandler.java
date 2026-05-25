package game.netty.handler.logic;

import com.game.UserProto;
import game.protocol.MessageHandler;
import io.netty.channel.ChannelHandlerContext;

public class LoginReqHandler implements MessageHandler<UserProto.LoginReq> {

	@Override
	public Class<UserProto.LoginReq> messageClass() {
		return UserProto.LoginReq.class;
	}

	@Override
	public void handler(ChannelHandlerContext ctx, UserProto.LoginReq msg) {

	}
}
