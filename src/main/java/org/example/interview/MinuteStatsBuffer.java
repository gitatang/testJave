package org.example.interview;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

public class MinuteStatsBuffer {

	private final AtomicReference<MinuteStatus> curMinuteStatus = new AtomicReference<>(new MinuteStatus());
	ScheduledExecutorService executorService;
	class MinuteStatus {
		private LongAdder[] payNums = new LongAdder[3];
		private volatile long curMinutes;

		public MinuteStatus(){
			for (int i = 0; i < payNums.length; i++) {
				payNums[i] = new LongAdder();
			}
			curMinutes = System.currentTimeMillis() / 60000;
		}

		public void add(int payType,int payNum){
			payNums[payType].add(payNum);
		}
	}

	public Map<Integer,Long> snapshotAndReset(){
		MinuteStatus oldMinuteStatus = curMinuteStatus.getAndSet(new MinuteStatus());
		Map<Integer,Long> result = new HashMap<>();
		for (int i = 0; i < oldMinuteStatus.payNums.length; i++) {
			result.put(i,oldMinuteStatus.payNums[i].sumThenReset());
		}
		return result;
	}

	public void start(){
		executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.scheduleAtFixedRate(()->{
			Map<Integer, Long> integerLongMap = snapshotAndReset();
			System.out.println(integerLongMap);
		},1,1, TimeUnit.MINUTES);
	}
}
