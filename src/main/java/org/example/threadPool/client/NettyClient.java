package org.example.threadPool.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClient {
	private final static Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);
	private  static NettyClient client;
	public final static NettyClient getInstance(){
		return client;
	}
	private final Channel channel;

	public NettyClient(Channel channel){
		this.channel = channel;
	}
	public static void main(String[] args) {
		clientStart();
	}

	private static void clientStart(){
		Bootstrap bootstrap = new Bootstrap();
		EventLoopGroup eventLoopGroup;
		if(Epoll.isAvailable()){
			eventLoopGroup = new EpollEventLoopGroup();
		}else {
			eventLoopGroup = new NioEventLoopGroup();
		}
		bootstrap.group(eventLoopGroup)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
						ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
						ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
						ch.pipeline().addLast(new ClientHandler());
					}
				});
		ChannelFuture connect = bootstrap.connect("127.0.0.1", 6000);

		client = new NettyClient(connect.channel());
		LOGGER.info("Netty server started on port {}" , 6000);
	}

	public   void sendMessage(String msg){
		this.channel.writeAndFlush(msg);
	}
}
