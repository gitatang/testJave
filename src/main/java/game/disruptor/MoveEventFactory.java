package game.disruptor;

import com.lmax.disruptor.EventFactory;

public class MoveEventFactory implements EventFactory<MoveEvent> {
    @Override
    public MoveEvent newInstance() {
        return new MoveEvent();
    }
}

