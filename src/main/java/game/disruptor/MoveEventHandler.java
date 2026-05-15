package game.disruptor;

import com.lmax.disruptor.EventHandler;

public class MoveEventHandler implements EventHandler<MoveEvent> {
	@Override
	public void onEvent(MoveEvent event, long l, boolean b) throws Exception {
		// ✅ 这里写“移动校验 / 碰撞检测 / AOI”
		System.out.println("处理玩家移动: playerId=" + event.playerId
				+ ", x=" + event.x + ", y=" + event.y);

		// ✅ 回写客户端（必须回到 Netty 线程）
		event.ctx.channel().eventLoop().execute(() -> {
			event.ctx.writeAndFlush("move ack");
		});
	}
}
