package game.protocol;


import lombok.Data;

@Data
public class MoveRequest extends MessageBase {
	public long playerId;
	public int mapId;
	public int x;
	public int y;
}
