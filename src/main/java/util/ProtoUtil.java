package util;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtoUtil {

	private static final Map<Class<?>, Method> PARSE_METHODS = new ConcurrentHashMap<>();

	/* ========== 反序列化 ========== */

	public static <T extends MessageLite> T read(
			ByteBuf buf, Class<T> clazz) throws Exception {

		Method method = PARSE_METHODS.computeIfAbsent(
				clazz, c -> findParseFrom(c));

		try (ByteBufInputStream in = new ByteBufInputStream(buf)) {
			return (T) method.invoke(null, in);
		}
	}

	private static <T> Method findParseFrom(Class<T> clazz) {
		try {
			return clazz.getMethod("parseFrom", InputStream.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/* ========== 序列化 ========== */

	public static void write(MessageLite msg, ByteBuf buf) {
		CodedOutputStream out = CodedOutputStream.newInstance(
				new ByteBufOutputStream(buf));
		try {
			msg.writeTo(out);
			out.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
