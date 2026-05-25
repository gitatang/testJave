package game.netty.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家消息顺序性测试
 *
 * 测试目的：验证同一个玩家的消息是否按顺序执行
 */
public class SequentialTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialTest.class);

    public static void main(String[] args) throws InterruptedException {
        testPlayerSequential();
        testMultiPlayerParallel();
        testBusinessThreadPool();
    }

    /**
     * 测试1：同一个玩家的消息顺序执行
     */
    private static void testPlayerSequential() throws InterruptedException {
        LOGGER.info("\n========== 测试1：同一玩家消息顺序性 ==========");

        BusinessThreadPool pool = BusinessThreadPool.getInstance();
        String userId = "player001";
        CountDownLatch latch = new CountDownLatch(10);
        List<Integer> executionOrder = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // 快速提交10个任务
        for (int i = 0; i < 10; i++) {
            final int taskNum = i;
            pool.submit(userId, userId, () -> {
                try {
                    executionOrder.add(taskNum);
                    LOGGER.info("执行任务 {}, thread={}", taskNum, Thread.currentThread().getName());
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 验证顺序
        boolean isSequential = true;
        for (int i = 0; i < executionOrder.size(); i++) {
            if (executionOrder.get(i) != i) {
                isSequential = false;
                break;
            }
        }

        LOGGER.info("执行顺序: {}", executionOrder);
        LOGGER.info("是否顺序执行: {}", isSequential ? "✓ 是" : "✗ 否");
        LOGGER.info("成功执行: {}/{}", successCount.get(), 10);

        if (!isSequential) {
            LOGGER.error("测试失败：消息未按顺序执行！");
        }
    }

    /**
     * 测试2：不同玩家的消息并行执行
     */
    private static void testMultiPlayerParallel() throws InterruptedException {
        LOGGER.info("\n========== 测试2：不同玩家消息并行性 ==========");

        BusinessThreadPool pool = BusinessThreadPool.getInstance();
        String userId1 = "player001";
        String userId2 = "player002";

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger executionTime1 = new AtomicInteger(0);
        AtomicInteger executionTime2 = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // 玩家1的任务（模拟耗时操作）
        pool.submit(userId1, userId1, () -> {
            try {
                LOGGER.info("玩家1开始执行, thread={}", Thread.currentThread().getName());
                Thread.sleep(1000); // 模拟耗时操作
                executionTime1.set((int)(System.currentTimeMillis() - startTime));
                LOGGER.info("玩家1执行完成, 耗时={}ms", executionTime1.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // 玩家2的任务（同时提交，应该并行执行）
        pool.submit(userId2, userId2, () -> {
            try {
                LOGGER.info("玩家2开始执行, thread={}", Thread.currentThread().getName());
                Thread.sleep(1000); // 模拟耗时操作
                executionTime2.set((int)(System.currentTimeMillis() - startTime));
                LOGGER.info("玩家2执行完成, 耗时={}ms", executionTime2.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        latch.await(3, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;

        LOGGER.info("总耗时: {}ms", totalTime);
        LOGGER.info("是否并行执行: {}", totalTime < 1800 ? "✓ 是" : "✗ 否");

        if (totalTime >= 1800) {
            LOGGER.error("测试失败：玩家消息未并行执行！");
        }
    }

    /**
     * 测试3：业务线程池详细状态
     */
    private static void testBusinessThreadPool() throws InterruptedException {
        LOGGER.info("\n========== 测试3：业务线程池状态 ==========");

        BusinessThreadPool pool = BusinessThreadPool.getInstance();

        LOGGER.info("线程数量: {}", pool.getThreadCount());
        LOGGER.info("线程池状态: {}", pool.getPoolStatus());

        // 提交一些任务
        CountDownLatch latch = new CountDownLatch(100);
        for (int i = 0; i < 100; i++) {
            final int taskNum = i;
            String userId = "player" + (i % 10); // 10个不同玩家
            pool.submit(userId, userId, () -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        LOGGER.info("\n任务执行完成后状态:");
        LOGGER.info("线程池状态: {}", pool.getPoolStatus());
        LOGGER.info("\n详细状态:");
        LOGGER.info(pool.getDetailedStatus());
    }

    /**
     * 测试4：路由一致性测试
     */
    private static void testRouteConsistency() {
        LOGGER.info("\n========== 测试4：路由一致性 ==========");

        BusinessThreadPool pool = BusinessThreadPool.getInstance();
        String userId = "test_user";

        // 多次查询同一个 userId 应该路由到同一个线程
        int threadCount = pool.getThreadCount();
        List<Integer> routes = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            int route = Math.abs(userId.hashCode()) % threadCount;
            routes.add(route);
        }

        boolean allSame = routes.stream().allMatch(r -> r.equals(routes.get(0)));

        LOGGER.info("用户ID: {}", userId);
        LOGGER.info("路由结果: {}", routes);
        LOGGER.info("所有路由是否一致: {}", allSame ? "✓ 是" : "✗ 否");

        if (!allSame) {
            LOGGER.error("测试失败：路由不一致！");
        }
    }
}