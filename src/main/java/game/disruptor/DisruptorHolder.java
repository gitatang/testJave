package game.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.Executors;

public class DisruptorHolder {
	public static final RingBuffer<MoveEvent> RING_BUFFER;

	static {
		Disruptor<MoveEvent> disruptor =
				new Disruptor<>(
						new MoveEventFactory(),
						1024,
						Executors.defaultThreadFactory()
				);

		disruptor.handleEventsWith(new MoveEventHandler());
		disruptor.start();

		RING_BUFFER = disruptor.getRingBuffer();
	}
}
