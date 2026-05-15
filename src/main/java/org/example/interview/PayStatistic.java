package org.example.interview;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class PayStatistic {
	private final static int Ali = 1;
	private final static int WeiXin = 2;
	private final static int Douyin = 3;

	private final static ConcurrentHashMap<Long,LongAdder> minuteTotalStatisticMap = new ConcurrentHashMap();

	private final static ConcurrentHashMap<Long,LongAdder> minuteAliStatisticMap = new ConcurrentHashMap();

	private final static ConcurrentHashMap<Long,LongAdder> minuteWinXinStatisticMap = new ConcurrentHashMap();

	private final static ConcurrentHashMap<Long,LongAdder> minuteDouyinStatisticMap = new ConcurrentHashMap();

	private final static LongAdder totalPayNum = new LongAdder();

	private final static LongAdder aliPayNum = new LongAdder();

	private final static LongAdder winXinPayNum = new LongAdder();

	private final static LongAdder douYinPayNum = new LongAdder();

	private static void pay(int payType,int payNum,long payTime) throws InterruptedException {
		Thread.sleep(10000);
		// 先判断这笔订单 是否是这一分钟的数据
		long now = System.currentTimeMillis();
		long minuteTimestampUTC = toMinuteTimestampUTC(now);
		long minuteTimestampUTCPay = toMinuteTimestampUTC(payTime);
		if(minuteTimestampUTC == minuteTimestampUTCPay){
			//如果是 直接添加
			switch (payType){
				case Ali: aliPayNum.add(payNum);
					break;
				case WeiXin:winXinPayNum.add(payNum);
					break;
				case Douyin:douYinPayNum.add(payNum);
					break;
				default:
					System.err.println("type error "+ payType);
					return;
			}
			totalPayNum.add(payNum);
		}

		minuteTotalStatisticMap.computeIfAbsent(minuteTimestampUTCPay,k -> new LongAdder()
				).add(payNum);
		switch (payType){
			case Ali: minuteAliStatisticMap.computeIfAbsent(minuteTimestampUTCPay,k -> new LongAdder()
			).add(payNum);
				break;
			case WeiXin:minuteWinXinStatisticMap.computeIfAbsent(minuteTimestampUTCPay,k -> new LongAdder()
			).add(payNum);
				break;
			case Douyin:minuteDouyinStatisticMap.computeIfAbsent(minuteTimestampUTCPay,k -> new LongAdder()
			).add(payNum);
				break;

		}




	}

	public static long toMinuteTimestampUTC(long timestamp) {
		return Math.floorDiv(timestamp, 60000) * 60000;
	}

	public static void main(String[] args) throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			pay(Ali, new Random().nextInt(),System.currentTimeMillis());
		}

		for (int i = 0; i < 100; i++) {
			pay(WeiXin, new Random().nextInt(),System.currentTimeMillis());
		}

		for (int i = 0; i < 100; i++) {
			pay(Douyin, new Random().nextInt(),System.currentTimeMillis());
		}

		Thread.sleep(1000000000);
	}

}
