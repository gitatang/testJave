package game.netty.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * BusinessEvent 工厂类
 * 用于 Disruptor 预分配 Event 对象
 */
public class BusinessEventFactory implements EventFactory<BusinessEvent> {
    @Override
    public BusinessEvent newInstance() {
        return new BusinessEvent();
    }
}