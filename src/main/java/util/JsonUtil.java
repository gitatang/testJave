package util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonUtil {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public static <T> T read(ByteBuf buf, Class<T> clazz) throws IOException {
		try (ByteBufInputStream in = new ByteBufInputStream(buf)) {
			return MAPPER.readValue((InputStream) in, clazz);
		}
	}

	/**
	 * 将对象写入已有的 ByteBuf（推荐：使用 Channel 的 allocator）
	 */
	public static void write(Object obj, ByteBuf buf) throws IOException {
		try (ByteBufOutputStream out = new ByteBufOutputStream(buf)) {
			MAPPER.writeValue((OutputStream) out, obj);
		}
	}

}
