package org.example.interview;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class PayStatisticV1 {

	private static final int PAY_TYPES = 3;//支付类型数量
	private static final int SLOTS = 60;//槽位
	private static final int INTERVAL_MS = 1000;//1秒一个槽

	private final LongAdder[][] slots = new LongAdder[SLOTS][PAY_TYPES];
	private volatile int currentSlotIndex = 0;
	private volatile long currentSlotStartTime = System.currentTimeMillis();

	public PayStatisticV1(){
		for (int i = 0; i < SLOTS; i++) {
			for (int j = 0; j < PAY_TYPES; j++) {
				slots[i][j] = new LongAdder();
			}
		}
	}

	public void pay(int payType,long amount){
		long now = System.currentTimeMillis();
		int slotIndex = (int) ((now / INTERVAL_MS) % SLOTS);

		// 处理分钟切换
		if (slotIndex != currentSlotIndex) {
			rotateSlots(slotIndex);
		}

		slots[slotIndex][payType].add(amount);
	}


	private synchronized void rotateSlots(int newIndex) {
		// 归档当前分钟数据
		archiveCurrentMinute();
		// 清除将要覆盖的槽位（一分钟前的数据）
//		clearSlot(newIndex);
		currentSlotIndex = newIndex;
	}

	private void archiveCurrentMinute() {
		// 归档逻辑：汇总当前分钟数据并发送到存储
		Map<Integer, Long> minuteData = new HashMap<>();
		for (int type = 0; type < PAY_TYPES; type++) {
			long total = 0;
			for (int i = 0; i < SLOTS; i++) {
				total += slots[i][type].sumThenReset();
			}
			minuteData.put(type, total);
		}
		// 异步存储到DB/消息队列
//		saveToStorage(minuteData);
	}

	public Map<Integer, Long> getLastMinuteStats() {
		// 实时汇总最近60秒数据
		Map<Integer, Long> result = new HashMap<>();
		for (int type = 0; type < PAY_TYPES; type++) {
			long total = 0;
			for (int i = 0; i < SLOTS; i++) {
				total += slots[i][type].sum();
			}
			result.put(type, total);
		}
		return result;
	}
}
