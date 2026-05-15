package org.example.threadPool.Server;


import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {
	private final static Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);
	private Object waitObject = new Object();

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
		ChannelHandler handler = ctx.handler();
		LOGGER.info("NettyServerHandler channelActive handler={}", handler);
	}


	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelInactive();
		ChannelHandler handler = ctx.handler();
		LOGGER.info("NettyServerHandler channelInactive handler={}", handler);
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		super.channelRead(ctx, msg);
		LOGGER.info("channelRead msg={}", msg);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
		LOGGER.info("NettyServerHandler channelRead0 msg={}", s);
		if (s.startsWith("发送个一个计算结果给我")) {
			ChannelPromise channelPromise = channelHandlerContext.newPromise();

			CompletableFuture.supplyAsync(() -> {
				String s1 = s.replace("发送个一个计算结果给我", "");
				return getCalculate(Integer.valueOf(s1));
			}).thenAcceptAsync((i) -> {
				channelHandlerContext.channel().eventLoop().execute(()->{
					channelHandlerContext.writeAndFlush("" + i + "\n");
				});

			}, channelHandlerContext.executor());
		}


	}


	private int getCalculate(int i) {
		return i;
	}


}
