package org.example.threadPool.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {
	private final static Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
	public static void main(String[] args) throws InterruptedException {
		serverStart();
	}

	private static void serverStart() throws InterruptedException {

		EventLoopGroup bossGroup;
		EventLoopGroup workGroup;
		ServerBootstrap bootstrap = new ServerBootstrap();
		if(Epoll.isAvailable()){
			bossGroup = new EpollEventLoopGroup(1);
			workGroup = new EpollEventLoopGroup();
		}else {
			bossGroup = new NioEventLoopGroup(1);
			workGroup = new NioEventLoopGroup();
		}
		bootstrap.group(bossGroup, workGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG,128)
				.childOption(ChannelOption.SO_KEEPALIVE,true)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						// 自定义 handler
						ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
						ch.pipeline().addLast("decode",new StringDecoder(CharsetUtil.UTF_8));
						ch.pipeline().addLast("encode",new StringEncoder(CharsetUtil.UTF_8));
						ch.pipeline().addLast(new LoggingHandler());
						ch.pipeline().addLast(new NettyServerHandler());
					}
				});

		ChannelFuture f = bootstrap.bind(6000).sync();
		LOGGER.info("Netty server started on port {}" , 6000);
		f.channel().closeFuture().sync();

	}
}
