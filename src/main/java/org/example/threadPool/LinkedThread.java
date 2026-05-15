package org.example.threadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

public class LinkedThread {

	static class Task {
		CompletableFuture<String> future;
		String requestData;

		public CompletableFuture<String> getFuture() {
			return future;
		}

		public void setFuture(CompletableFuture<String> future) {
			this.future = future;
		}

		public String getRequestData() {
			return requestData;
		}

		public void setRequestData(String requestData) {
			this.requestData = requestData;
		}

		@Override
		public String toString() {
			return "Task{" +
					"future=" + future +
					", requestData='" + requestData + '\'' +
					'}';
		}
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(LinkedThread.class);
	private static final ArrayBlockingQueue<Task> queue = new ArrayBlockingQueue<>(9999);

	static {
		new Thread(()->{
			try {
				while (true){
					Task take = queue.take();
					String requestData = take.getRequestData();
					LOGGER.info("take==== :{}",requestData);
					String concat = requestData.concat(", finish");
					take.getFuture().complete(concat);
				}

			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		},"consumer").start();
	}

	public static void main(String[] args) throws InterruptedException {

		for (int i = 0; i < 100; i++) {
			String get = "发送 " + i;
			new Thread(()->{
				Task task = new Task();
				task.setRequestData(get);
				CompletableFuture<String> future = new CompletableFuture<>();
				task.setFuture(future);
				queue.offer(task);
//				LOGGER.info("offer task :{}",task);
				future.whenComplete((r,e)->{
					if(null != e){
						LOGGER.error("处理完成失败 ",e);
					}else {
						LOGGER.info("处理完成：{}",r);
					}

				});


			}).start();

		}

		Thread.sleep(9999999999999999L);
	}




}
