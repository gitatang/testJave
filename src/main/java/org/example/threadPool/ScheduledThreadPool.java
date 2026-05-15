package org.example.threadPool;

import java.util.concurrent.ArrayBlockingQueue;

public class ScheduledThreadPool {

	public static void main(String[] args) throws InterruptedException {
		ArrayBlockingQueue<Integer> queues = new ArrayBlockingQueue<>(100);
		queues.offer(1);
		queues.offer(2);
		queues.offer(3);

		Integer take = queues.take();
		System.out.println(take);
		Integer take1 = queues.take();
		System.out.println(take1);

		Integer take2 = queues.take();
		System.out.println(take2);

	}



}
