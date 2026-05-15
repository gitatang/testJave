package game.disruptor;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

@Data
public class MoveEvent {
	public long playerId;
	public int mapId;
	public int x;
	public int y;
	public ChannelHandlerContext ctx; // 写回用
}
