package game.netty.handler;

import com.lmax.disruptor.RingBuffer;
import game.disruptor.DisruptorHolder;
import game.disruptor.MoveEvent;
import game.protocol.MoveRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class GameServerHandler  extends SimpleChannelInboundHandler<MoveRequest> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MoveRequest msg) throws Exception {
		RingBuffer<MoveEvent> ringBuffer = DisruptorHolder.RING_BUFFER;

		long seq = ringBuffer.next(); // 申请槽位
		try {
			MoveEvent event = ringBuffer.get(seq);
			event.playerId = msg.playerId;
			event.mapId = msg.mapId;
			event.x = msg.x;
			event.y = msg.y;
			event.ctx = ctx;
		} finally {
			ringBuffer.publish(seq); // ✅ 必须 publish
		}
	}
}
