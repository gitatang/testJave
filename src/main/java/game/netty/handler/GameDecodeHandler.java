package game.netty.handler;

import game.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.JsonUtil;

import java.util.List;
import java.util.function.BiFunction;

public class GameDecodeHandler extends MessageToMessageDecoder<ByteBuf> {
	private final static Logger LOGGER = LoggerFactory.getLogger(GameDecodeHandler.class);
	private static final short MAGIC = (short) 0xCAFE;
	private static BiFunction dispatch;
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) {
		if (byteBuf.readableBytes() < 8) {
			return;
		}

		byteBuf.markReaderIndex();
		short magic = byteBuf.readShort();
		if(magic != MAGIC){
			ctx.close();
			return;
		}
		byte version = byteBuf.readByte();
		byte type = byteBuf.readByte();
		int bodyLength = byteBuf.readInt();

		if (bodyLength <= 0 || bodyLength > byteBuf.readableBytes()) {
			byteBuf.resetReaderIndex();
			LOGGER.debug("等待更多数据... 需要 {} 字节，现有 {} 字节",
					bodyLength, byteBuf.readableBytes());
			return;
		}

		// slice 不会修改原 ByteBuf
		ByteBuf slicedBuf = byteBuf.slice(byteBuf.readerIndex(), bodyLength);
		try  {


			Class<? extends Message> clazz =
					MessageRegistry.getMessageClass(version, type);


			if (clazz == null) {
				throw new IllegalStateException(
						"Unknown message version=" + version + ", type=" + type);
			}

			Message message = JsonUtil.read(slicedBuf, clazz);
			if(null != message){
				out.add(message);
			}

		} catch (Exception e) {
			LOGGER.error("消息解码失败，长度: {}", bodyLength, e);
		} finally {
			//  无论如何都推进读指针（关键）
			byteBuf.skipBytes(bodyLength);
		}
	}
}
