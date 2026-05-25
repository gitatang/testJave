package game.netty.handler;

import game.netty.handler.logic.ProtoMessage;
import game.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Method;
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
		long requestId = byteBuf.readLong();
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
			Class<? extends com.google.protobuf.MessageLite> msgClass =
					MessageRegistry.getMessageClass(version, type);
			if (msgClass == null) {
				throw new IllegalStateException(
						"Unknown message version=" + version + ", type=" + type);
			}
			// 使用 Proto 解析
			com.google.protobuf.MessageLite msg = parseProto(slicedBuf, msgClass);
			// 包装成 ProtoMessage
			ProtoMessage<?> protoMsg = new ProtoMessage<>(version, type, requestId, msg,msgClass);
			out.add(protoMsg);

		} catch (Exception e) {
			LOGGER.error("消息解码失败，长度: {}", bodyLength, e);
		} finally {
			//  无论如何都推进读指针（关键）
			byteBuf.skipBytes(bodyLength);
		}
	}

	private com.google.protobuf.MessageLite parseProto(ByteBuf buf, Class<? extends com.google.protobuf.MessageLite> clazz) throws Exception {
		// 使用 ByteBufInputStream 避免拷贝
		try (ByteBufInputStream input = new ByteBufInputStream(buf)) {
			// 获取 parseFrom 方法
			Method method = clazz.getMethod("parseFrom", InputStream.class);
			return (com.google.protobuf.MessageLite) method.invoke(null, input);
		}
	}

}
