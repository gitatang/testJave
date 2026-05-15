package org.example.threadPool.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClientHandler extends SimpleChannelInboundHandler<String> {

	private final static Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws InterruptedException {
		LOGGER.info("连接到服务器: {}", ctx.channel().remoteAddress());
		// 连接建立后可以发送欢迎消息
		ctx.writeAndFlush("Hello Server!\n");
//		for (int i = 0; i < 100; i++) {
//			int finalI = i;
//			new Thread(() -> {
//				ctx.writeAndFlush("发送个一个计算结果给我" + finalI + "\n");
//				LOGGER.info("发送个一个计算结果给我 {}", finalI);
//			}, "name_" + finalI).start();
//
//
//		}

//		Thread.sleep(2000);

		ctx.writeAndFlush("notifyAll \n");

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		LOGGER.info("与服务器断开连接");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOGGER.info("发生异常: {} ", cause.getMessage());
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
		LOGGER.info("收到服务器的消息 {}", s);
	}
}
