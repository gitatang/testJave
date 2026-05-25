package game.netty.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Disruptor 性能测试
 *
 * 对比测试：
 * 1. Disruptor 性能测试
 * 2. BlockingQueue 性能测试（对比）
 * 3. 顺序性验证测试
 */
public class DisruptorPerformanceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorPerformanceTest.class);

    public static void main(String[] args) throws InterruptedException {
        LOGGER.info("========================================");
        LOGGER.info("Disruptor 性能测试");
        LOGGER.info("========================================");

        // 测试1：Disruptor 顺序性测试
        testDisruptorSequential();

        // 测试2：Disruptor 并行性测试
        testDisruptorParallel();

        // 测试3：Disruptor 吞吐量测试
        testDisruptorThroughput();

        // 测试4：Disruptor vs BlockingQueue 对比测试
        testDisruptorVsBlockingQueue();

        LOGGER.info("========================================");
        LOGGER.info("所有测试完成");
        LOGGER.info("========================================");
    }

    /**
     * 测试1：Disruptor 顺序性测试
     */
    private static void testDisruptorSequential() throws InterruptedException {
        LOGGER.info("\n========== 测试1：Disruptor 顺序性测试 ==========");

        DisruptorBusinessPool pool = DisruptorBusinessPool.getInstance();
        String userId = "player_seq_test";

        CountDownLatch latch = new CountDownLatch(100);
        List<Integer> executionOrder = new ArrayList<>();

        // 快速提交100个任务
        for (int i = 0; i < 100; i++) {
            final int taskNum = i;
            pool.submitTask(userId, userId, () -> {
                synchronized (executionOrder) {
                    executionOrder.add(taskNum);
                }
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);

        // 验证顺序
        boolean isSequential = true;
        for (int i = 0; i < executionOrder.size(); i++) {
            if (executionOrder.get(i) != i) {
                isSequential = false;
                break;
            }
        }

        LOGGER.info("执行任务数: {}/100", executionOrder.size());
        LOGGER.info("是否顺序执行: {}", isSequential ? "✓ 是" : "✗ 否");

        if (!isSequential) {
            LOGGER.error("测试失败：消息未按顺序执行！");
            LOGGER.error("执行顺序: {}", executionOrder.subList(0, Math.min(20, executionOrder.size())));
        }
    }

    /**
     * 测试2：Disruptor 并行性测试
     */
    private static void testDisruptorParallel() throws InterruptedException {
        LOGGER.info("\n========== 测试2：Disruptor 并行性测试 ==========");

        DisruptorBusinessPool pool = DisruptorBusinessPool.getInstance();
        String user1 = "player_parallel_1";
        String user2 = "player_parallel_2";

        CountDownLatch latch = new CountDownLatch(2);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        // 玩家1的任务（模拟耗时操作）
        pool.submitTask(user1, user1, () -> {
            LOGGER.info("玩家1开始执行, thread={}", Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("玩家1执行完成, 耗时={}ms", System.currentTimeMillis() - startTime.get());
            latch.countDown();
        });

        // 玩家2的任务（同时提交，应该并行执行）
        pool.submitTask(user2, user2, () -> {
            LOGGER.info("玩家2开始执行, thread={}", Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            LOGGER.info("玩家2执行完成, 耗时={}ms", System.currentTimeMillis() - startTime.get());
            latch.countDown();
        });

        latch.await(3, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime.get();

        LOGGER.info("总耗时: {}ms", totalTime);
        LOGGER.info("是否并行执行: {}", totalTime < 1800 ? "✓ 是" : "✗ 否");
    }

    /**
     * 测试3：Disruptor 吞吐量测试
     */
    private static void testDisruptorThroughput() throws InterruptedException {
        LOGGER.info("\n========== 测试3：Disruptor 吞吐量测试 ==========");

        DisruptorBusinessPool pool = DisruptorBusinessPool.getInstance();
        int taskCount = 100000;
        int playerCount = 100;

        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        LOGGER.info("开始提交 {} 个任务，涉及 {} 个玩家...", taskCount, playerCount);

        // 多线程提交任务
        ExecutorService submitExecutor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < taskCount; i++) {
            final int taskNum = i;
            String userId = "player_throughput_" + (taskNum % playerCount);

            submitExecutor.submit(() -> {
                pool.submitTask(userId, userId, () -> {
                    // 模拟简单业务逻辑
                    int sum = 0;
                    for (int j = 0; j < 100; j++) {
                        sum += j;
                    }
                    latch.countDown();
                });
            });
        }

        submitExecutor.shutdown();
        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime.get();

        long throughput = (taskCount * 1000) / totalTime;
        LOGGER.info("总耗时: {}ms", totalTime);
        LOGGER.info("吞吐量: {} 任务/秒", throughput);
        LOGGER.info("平均延迟: {} μs", (totalTime * 1000) / taskCount);

        // 打印详细状态
        LOGGER.info("\n{}", pool.getDetailedStatus());
    }

    /**
     * 测试4：Disruptor vs BlockingQueue 对比测试
     */
    private static void testDisruptorVsBlockingQueue() throws InterruptedException {
        LOGGER.info("\n========== 测试4：性能对比测试 ==========");

        int taskCount = 10000;

        // 测试 Disruptor
        LOGGER.info("\n--- 测试 Disruptor ---");
        long disruptorTime = testDisruptor(taskCount);
        LOGGER.info("Disruptor 耗时: {}ms, 吞吐量: {} 任务/秒",
                disruptorTime, (taskCount * 1000) / disruptorTime);

        // 测试 BlockingQueue
        LOGGER.info("\n--- 测试 BlockingQueue ---");
        long queueTime = testBlockingQueue(taskCount);
        LOGGER.info("BlockingQueue 耗时: {}ms, 吞吐量: {} 任务/秒",
                queueTime, (taskCount * 1000) / queueTime);

        // 对比结果
        double improvement = ((double)(queueTime - disruptorTime) / queueTime) * 100;
        LOGGER.info("\n性能提升: {}%", improvement > 0 ? String.format("%.2f", improvement) : "0");
    }

    /**
     * 测试 Disruptor 性能
     */
    private static long testDisruptor(int taskCount) throws InterruptedException {
        DisruptorBusinessPool pool = DisruptorBusinessPool.getInstance();
        CountDownLatch latch = new CountDownLatch(taskCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            String userId = "player_perf_" + (i % 100);
            pool.submitTask(userId, userId, () -> {
                // 模拟简单业务逻辑
                int sum = 0;
                for (int j = 0; j < 10; j++) {
                    sum += j;
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 测试 BlockingQueue 性能
     */
    private static long testBlockingQueue(int taskCount) throws InterruptedException {
        int threadCount = Math.max(Runtime.getRuntime().availableProcessors() * 2, 8);
        ExecutorService[] executors = new ExecutorService[threadCount];

        for (int i = 0; i < threadCount; i++) {
            executors[i] = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(false);
                return t;
            });
        }

        CountDownLatch latch = new CountDownLatch(taskCount);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < taskCount; i++) {
            String userId = "player_perf_" + (i % 100);
            int index = Math.abs(userId.hashCode()) % threadCount;

            executors[index].execute(() -> {
                // 模拟简单业务逻辑
                int sum = 0;
                for (int j = 0; j < 10; j++) {
                    sum += j;
                }
                latch.countDown();
            });
        }

        latch.await(30, TimeUnit.SECONDS);

        // 关闭线程池
        for (ExecutorService executor : executors) {
            executor.shutdown();
        }

        return System.currentTimeMillis() - startTime;
    }
}