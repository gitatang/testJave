# Java 面试题集

## 目录

1. [Java 基础](#java-基础)
2. [集合框架](#集合框架)
3. [多线程与并发](#多线程与并发)
4. [JVM](#jvm)
5. [IO/NIO](#ionio)
6. [设计模式](#设计模式)
7. [Spring 框架](#spring-框架)
8. [数据库](#数据库)
9. [算法与数据结构](#算法与数据结构)
10. [系统设计](#系统设计)

---

## Java 基础

### 1. Java 面向对象特征

**封装**
- 将数据和方法包装在类中，通过访问修饰符控制访问权限
- 优点：隐藏实现细节、提高安全性、便于修改

**继承**
- 子类继承父类的属性和方法
- Java 只支持单继承，但支持多层继承
- 使用 `extends` 关键字

**多态**
- 同一个行为具有多个不同表现形式
- 实现方式：方法重写、方法重载、接口实现
- 三个必要条件：继承、重写、父类引用指向子类对象

**抽象**
- 将一类事物的共同特征提取出来
- 实现方式：抽象类、接口

### 2. Java 基本数据类型

| 类型 | 大小 | 包装类 | 默认值 |
|------|------|--------|--------|
| byte | 8位 | Byte | 0 |
| short | 16位 | Short | 0 |
| int | 32位 | Integer | 0 |
| long | 64位 | Long | 0L |
| float | 32位 | Float | 0.0f |
| double | 64位 | Double | 0.0d |
| char | 16位 | Character | '\u0000' |
| boolean | - | Boolean | false |

### 3. == 和 equals 的区别

- **==**：比较内存地址（引用是否指向同一对象）
  - 基本类型：比较值
  - 引用类型：比较地址

- **equals()**：比较内容
  - 默认比较地址（Object 类）
  - String、Integer 等类重写了 equals() 方法，比较值

### 4. String、StringBuffer、StringBuilder 区别

| 特性 | String | StringBuffer | StringBuilder |
|------|--------|--------------|---------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全 | 安全（synchronized） | 不安全 |
| 性能 | 低 | 中 | 高 |
| 使用场景 | 常量、少量数据 | 多线程操作字符串 | 单线程大量操作 |

### 5. 重写与重载

**重写（Override）**
- 子类重新定义父类的方法
- 方法名、参数列表、返回类型必须相同
- 访问权限不能更严格
- 不能抛出新的或更广的检查异常

**重载（Overload）**
- 同一个类中多个同名方法
- 参数列表必须不同（个数、类型、顺序）
- 返回类型可以不同
- 访问修饰符可以不同

### 6. 接口与抽象类

**相同点**
- 都不能被实例化
- 都可以包含抽象方法
- 都可以有默认实现（Java 8+ 接口支持 default 方法）

**区别**

| 特性 | 抽象类 | 接口 |
|------|--------|------|
| 继承 | 单继承 | 多实现 |
| 构造方法 | 有 | 无 |
| 成员变量 | 各种类型 | public static final |
| 方法 | 抽象/具体 | 抽象/default/static |
| 访问修饰符 | 各种 | public |

### 7. 深拷贝与浅拷贝

**浅拷贝**
- 复制对象本身，但引用类型成员共享内存
- 使用 `clone()` 方法实现 Cloneable 接口

**深拷贝**
- 完全复制所有引用类型成员
- 实现方式：序列化、手动复制所有字段

### 8. Java 异常体系

```
Throwable
├── Error（错误，程序无法处理）
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── ...
└── Exception（异常，程序可以处理）
    ├── RuntimeException（运行时异常，非检查异常）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── ClassCastException
    │   └── ...
    └── 检查异常（编译时必须处理）
        ├── IOException
        ├── SQLException
        └── ...
```

**异常处理关键字**
- `try`：包含可能抛出异常的代码
- `catch`：捕获并处理异常
- `finally`：无论是否异常都执行
- `throw`：手动抛出异常
- `throws`：声明方法可能抛出的异常

### 9. 泛型

**优点**
- 编译时类型检查
- 避免类型转换
- 代码复用

**通配符**
- `<?>`：任意类型
- `<? extends T>`：T 或其子类（上限）
- `<? super T>`：T 或其父类（下限）

**类型擦除**
- 泛型只在编译期存在，运行时会被擦除
- 不能创建泛型数组
- 不能重载泛型方法

### 10. 反射

**定义**
- 在运行时动态获取类的信息并操作其属性和方法

**主要 API**
- `Class`：表示类和接口
- `Field`：表示类的成员变量
- `Method`：表示类的方法
- `Constructor`：表示构造器

**使用场景**
- 框架开发（Spring、MyBatis）
- 动态代理
- 注解处理

---

## 集合框架

### 1. 集合体系结构

```
Collection (接口)
├── List (接口，有序、可重复)
│   ├── ArrayList（数组实现，查询快，增删慢）
│   ├── LinkedList（链表实现，增删快，查询慢）
│   └── Vector（数组实现，线程安全，性能低）
├── Set (接口，无序、不可重复)
│   ├── HashSet（哈希表实现，O(1)）
│   ├── LinkedHashSet（保持插入顺序）
│   └── TreeSet（红黑树实现，有序）
└── Queue (接口，队列)
    ├── LinkedList
    ├── PriorityQueue（优先队列）
    └── ArrayDeque（双端队列）

Map (接口，键值对)
├── HashMap（哈希表，线程不安全）
├── ConcurrentHashMap（分段锁，线程安全）
├── Hashtable（同步，线程安全，性能低）
├── TreeMap（红黑树，有序）
└── LinkedHashMap（保持插入/访问顺序）
```

### 2. ArrayList 与 LinkedList 区别

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层结构 | 动态数组 | 双向链表 |
| 查询 | O(1) | O(n) |
| 插入/删除 | O(n) | O(1)（已知位置） |
| 内存占用 | 较少 | 较多（额外指针） |
| 扩容 | 1.5倍 | 无需扩容 |

### 3. HashMap 原理（重点）

**数据结构**
- JDK 1.8 之前：数组 + 链表
- JDK 1.8+：数组 + 链表 + 红黑树（链表长度 > 8 且数组长度 > 64）

**核心参数**
- 初始容量：16
- 负载因子：0.75
- 扩容阈值：capacity * loadFactor
- 树化阈值：8
- 退化阈值：6

**put 过程**
1. 计算 key 的 hash 值
2. 确定数组索引
3. 如果位置为空，直接插入
4. 如果位置有值，遍历链表/红黑树
5. 找到相同 key，覆盖 value
6. 没找到，插入新节点
7. 判断是否需要树化
8. 判断是否需要扩容

**为什么容量是 2 的幂次方**
- 通过 `(n - 1) & hash` 计算索引
- 2 的幂次方减 1 的二进制全是 1，确保 hash 值分布均匀
- 提高位运算效率

**HashMap 线程安全问题**
- JDK 1.7：扩容时可能产生死循环
- JDK 1.8：数据覆盖问题
- 解决方案：ConcurrentHashMap、Collections.synchronizedMap()

### 4. ConcurrentHashMap 原理

**JDK 1.7**
- 分段锁（Segment）
- 每个 Segment 相当于一个小 HashMap
- 并发度等于 Segment 数量

**JDK 1.8**
- 抛弃 Segment
- 使用 CAS + synchronized
- 锁粒度更细（数组节点）
- 性能更高

### 5. HashSet 原理

- 基于 HashMap 实现
- value 是固定的 Object（PRESENT）
- add() 时将元素作为 key，PRESENT 作为 value

### 6. TreeMap 原理

- 基于红黑树实现
- key 必须实现 Comparable 或传入 Comparator
- 有序存储（自然序或自定义序）

### 7. Comparable 与 Comparator

**Comparable**
- `java.lang.Comparable`
- 在类内部实现
- 自然排序
- 方法：`compareTo()`

**Comparator**
- `java.util.Comparator`
- 外部比较器
- 自定义排序
- 方法：`compare()`

---

## 多线程与并发

### 1. 线程创建方式

**1. 继承 Thread 类**
```java
class MyThread extends Thread {
    @Override
    public void run() {
        // 线程代码
    }
}
```

**2. 实现 Runnable 接口**
```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        // 线程代码
    }
}
```

**3. 实现 Callable 接口**
```java
class MyCallable implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
```

**4. 使用线程池**
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(new MyRunnable());
```

### 2. 线程状态

```
NEW（新建）
    ↓ start()
RUNNABLE（运行）
    ↓ 等待锁
BLOCKED（阻塞）
    ↓ 获取锁
RUNNABLE
    ↓ wait()/sleep()/join()
WAITING/TIMED_WAITING（等待）
    ↓ 被通知/时间到/结束
RUNNABLE
    ↓ run()结束
TERMINATED（终止）
```

### 3. wait() 与 sleep() 区别

| 特性 | wait() | sleep() |
|------|--------|---------|
| 类 | Object | Thread |
| 锁释放 | 释放 | 不释放 |
| 使用位置 | 同步代码块 | 任意位置 |
| 唤醒 | notify()/notifyAll() | 时间到/interrupt() |
| 必须捕获异常 | 否 | 是 |

### 4. synchronized 关键字

**作用范围**
- 实例方法：锁当前对象
- 静态方法：锁当前类的 Class 对象
- 代码块：锁指定对象

**特点**
- 自动加锁、释放锁
- 可重入
- 非公平锁
- 性能相对较低（JDK 1.6 后优化）

### 5. Lock 接口（ReentrantLock）

**与 synchronized 区别**
| 特性 | synchronized | ReentrantLock |
|------|--------------|---------------|
| 用法 | 关键字 | API |
| 锁释放 | 自动 | 手动（finally） |
| 公平性 | 非公平 | 可选 |
| 条件变量 | wait/notify | Condition |
| 可中断 | 否 | 是 |
| 尝试获取锁 | 否 | 是 |

**使用示例**
```java
ReentrantLock lock = new ReentrantLock();
Condition condition = lock.newCondition();

lock.lock();
try {
    // 业务代码
    condition.await(); // 等待
    condition.signal(); // 通知
} finally {
    lock.unlock();
}
```

### 6. volatile 关键字

**作用**
- 保证可见性（JMM 主内存与工作内存同步）
- 禁止指令重排序
- 不保证原子性

**适用场景**
- 状态标志位
- 单例模式（双重检查锁）
- 轻量级同步

### 7. CAS（Compare And Swap）

**原理**
- 比较并交换
- 期望值、原值、新值
- 如果原值等于期望值，更新为新值
- 如果不等，重试（自旋）

**优点**
- 无锁
- 高并发性能好

**缺点**
- ABA 问题（解决：AtomicStampedReference）
- 自旋时间长浪费 CPU
- 只能保证一个变量原子性

### 8. AQS（AbstractQueuedSynchronizer）

**原理**
- 基于 CLH 队列锁
- state 变量（0-未获取，>0-已获取）
- Node 节点封装线程

**实现类**
- ReentrantLock
- CountDownLatch
- Semaphore
- CyclicBarrier

### 9. 线程池

**核心参数**
1. corePoolSize：核心线程数
2. maximumPoolSize：最大线程数
3. keepAliveTime：非核心线程存活时间
4. workQueue：任务队列
5. threadFactory：线程工厂
6. handler：拒绝策略

**执行流程**
1. 线程数 < corePoolSize，创建新线程
2. 线程数 >= corePoolSize，加入队列
3. 队列已满且线程数 < maximumPoolSize，创建新线程
4. 线程数 >= maximumPoolSize，执行拒绝策略

**拒绝策略**
- AbortPolicy（默认）：抛异常
- CallerRunsPolicy：调用者执行
- DiscardPolicy：丢弃
- DiscardOldestPolicy：丢弃最老任务

**线程池类型**
- FixedThreadPool：固定大小
- CachedThreadPool：可缓存
- SingleThreadExecutor：单线程
- ScheduledThreadPool：定时任务

### 10. ThreadLocal

**原理**
- 线程局部变量
- 每个 Thread 有一个 ThreadLocalMap
- key 是 ThreadLocal 对象，value 是值

**内存泄漏**
- ThreadLocal 的 key 是弱引用，会被 GC
- 但 value 是强引用，可能泄漏
- 使用后必须 remove()

### 11. 死锁

**四个必要条件**
1. 互斥：资源独占
2. 持有并等待：持有资源同时等待其他资源
3. 不可抢占：资源不能被强制抢占
4. 循环等待：形成等待环路

**预防**
- 破坏其中一个条件
- 统一加锁顺序
- 设置超时

### 12. 并发工具类

**CountDownLatch**
- 计数器，减到 0 时唤醒等待线程
- 场景：并行任务汇总

**CyclicBarrier**
- 栅栏，线程都到达时执行
- 场景：多线程计算，最后汇总

**Semaphore**
- 信号量，控制并发数
- 场景：限流

**Exchanger**
- 交换数据
- 场景：两线程数据交换

---

## JVM

### 1. JVM 内存结构

```
┌─────────────────────────────────┐
│          线程私有                │
├─────────────┬───────────────────┤
│ 程序计数器   │ 记录当前执行位置    │
│ 虚拟机栈     │ 方法调用、局部变量  │
│ 本地方法栈   │ Native 方法        │
├─────────────┴───────────────────┤
│          线程共享                │
├─────────────────────────────────┤
│           堆                    │
│    对象实例、数组                │
├─────────────────────────────────┤
│          方法区                 │
│    类信息、常量、静态变量         │
└─────────────────────────────────┘
```

### 2. 垃圾回收算法

**标记-清除**
- 标记需要回收的对象
- 直接清除
- 缺点：产生内存碎片

**复制算法**
- 将存活对象复制到另一块
- 适合新生代
- 优点：无碎片
- 缺点：内存利用率低

**标记-整理**
- 标记后整理
- 适合老年代
- 优点：无碎片

**分代收集**
- 新生代：复制算法
- 老年代：标记-整理/标记-清除

### 3. 垃圾收集器

**Serial**
- 单线程
- STW（Stop The World）
- 适合小内存

**Parallel**
- 多线程
- 关注吞吐量

**CMS（Concurrent Mark Sweep）**
- 并发标记清除
- 关注低延迟
- 已废弃（JDK 9）/移除（JDK 14）

**G1（Garbage First）**
- 区域化
- 可预测停顿
- 默认 GC（JDK 9+）

**ZGC**
- 超低延迟（< 10ms）
- 支持大内存
- 生产可用（JDK 15）

### 4. 类加载机制

**加载过程**
1. 加载：读取字节码
2. 验证：确保安全性
3. 准备：分配内存、初始化零值
4. 解析：符号引用转直接引用
5. 初始化：执行静态代码块

**类加载器**
- Bootstrap ClassLoader：启动类加载器（核心类库）
- Extension ClassLoader：扩展类加载器（ext 目录）
- Application ClassLoader：应用类加载器（classpath）

**双亲委派模型**
- 子类加载器委托给父类加载器
- 父类加载不到才自己加载
- 优点：安全、避免重复加载

### 5. JVM 参数

**内存相关**
- `-Xms`：初始堆大小
- `-Xmx`：最大堆大小
- `-Xmn`：新生代大小
- `-XX:MetaspaceSize`：元空间大小
- `-XX:MaxMetaspaceSize`：最大元空间

**GC 相关**
- `-XX:+UseG1GC`：使用 G1
- `-XX:MaxGCPauseMillis`：最大停顿时间
- `-XX:+PrintGCDetails`：打印 GC 详情

**性能分析**
- `-XX:+HeapDumpOnOutOfMemoryError`：OOM 时 dump
- `-XX:HeapDumpPath`：dump 路径

### 6. 性能调优

**调优目标**
- 吞吐量：运行时间 / (运行时间 + GC 时间)
- 延迟：GC 停顿时间
- 内存占用

**调优步骤**
1. 监控：发现问题
2. 分析：找到瓶颈
3. 调整：修改参数
4. 验证：确认效果

**常见问题**
- OOM（内存溢出）
- 内存泄漏
- CPU 高
- GC 频繁

---

## IO/NIO

### 1. IO 分类

**按流向**
- 输入流：读取数据
- 输出流：写入数据

**按单位**
- 字节流：InputStream/OutputStream
- 字符流：Reader/Writer

**按角色**
- 节点流：直接操作数据源
- 处理流：包装其他流

### 2. BIO、NIO、AIO 区别

| 特性 | BIO | NIO | AIO |
|------|-----|-----|-----|
| 模型 | 同步阻塞 | 同步非阻塞 | 异步非阻塞 |
| IO | 面向流 | 面向缓冲区 | 面向缓冲区 |
| 线程 | 每连接一线程 | 单线程多连接 | 回调方式 |
| 适用 | 连接数少 | 连接数多 | 连接数多 |

### 3. NIO 核心组件

**Buffer（缓冲区）**
- 存储数据
- 类型：ByteBuffer、CharBuffer、IntBuffer 等
- 位置、限制、容量、标记

**Channel（通道）**
- 数据传输通道
- 类型：FileChannel、SocketChannel、ServerSocketChannel
- 双向读写

**Selector（选择器）**
- 多路复用
- 单线程管理多个 Channel
- select() 查询就绪 Channel

### 4. 零拷贝

**传统 IO**
4 次上下文切换、4 次数据复制

**零拷贝**
- mmap：内存映射
- sendfile：2 次上下文切换、2 次复制
- 应用：Kafka、Netty

---

## 设计模式

### 1. 创建型模式

**单例模式**
- 饿汉式：类加载时初始化
- 懒汉式：延迟加载
- 双重检查锁：DCL
- 静态内部类：推荐
- 枚举：最安全

**工厂模式**
- 简单工厂
- 工厂方法
- 抽象工厂

**建造者模式**
- 分步骤创建复杂对象

**原型模式**
- 克隆对象

### 2. 结构型模式

**代理模式**
- 静态代理
- 动态代理（JDK、CGLIB）

**适配器模式**
- 接口转换

**装饰器模式**
- 动态添加功能

**外观模式**
- 简化接口

**桥接模式**
- 分离抽象与实现

**组合模式**
- 树形结构

**享元模式**
- 对象共享

### 3. 行为型模式

**观察者模式**
- 事件驱动
- 发布订阅

**策略模式**
- 算法封装
- 可互换

**模板方法模式**
- 算法骨架

**责任链模式**
- 请求传递

**命令模式**
- 请求封装

**状态模式**
- 状态转换

**迭代器模式**
- 遍历集合

**中介者模式**
- 解耦交互

**备忘录模式**
- 状态保存

**解释器模式**
- 语法解析

**访问者模式**
- 操作分离

---

## Spring 框架

### 1. IOC（控制反转）

**概念**
- 对象创建权交给 Spring 容器
- 依赖注入（DI）

**注入方式**
- 构造器注入（推荐）
- Setter 注入
- 字段注入（不推荐）

**作用域**
- singleton（默认）：单例
- prototype：多例
- request：请求级别
- session：会话级别

### 2. AOP（面向切面编程）

**概念**
- 横切关注点分离
- 动态代理实现

**术语**
- 切面：横切逻辑
- 连接点：执行点
- 切点：匹配连接点
- 通知：增强逻辑
- 目标对象：被代理对象
- 织入：创建代理对象

**通知类型**
- @Before：前置
- @After：后置
- @AfterReturning：返回
- @AfterThrowing：异常
- @Around：环绕

### 3. Bean 生命周期

1. 实例化
2. 属性赋值
3. BeanNameAware、BeanFactoryAware 等
4. BeanPostProcessor.before
5. @PostConstruct、init-method
6. BeanPostProcessor.after
7. 使用
8. @PreDestroy、destroy-method

### 4. 事务管理

**传播行为**
- REQUIRED：有则加入，无则创建
- REQUIRES_NEW：总是创建新事务
- NESTED：嵌套事务
- SUPPORTS：有则加入，无则非事务
- NOT_SUPPORTED：非事务执行
- MANDATORY：必须有事务
- NEVER：不能有事务

**隔离级别**
- DEFAULT：数据库默认
- READ_UNCOMMITTED：读未提交
- READ_COMMITTED：读已提交
- REPEATABLE_READ：可重复读
- SERIALIZABLE：串行化

### 5. Spring MVC

**执行流程**
1. 请求 → DispatcherServlet
2. HandlerMapping 查找 Controller
3. 执行 Controller 方法
4. 返回 ModelAndView
5. ViewResolver 解析视图
6. 渲染视图
7. 响应

**核心组件**
- DispatcherServlet：前端控制器
- HandlerMapping：处理器映射
- HandlerAdapter：处理器适配器
- ViewResolver：视图解析器

### 6. Spring Boot

**自动配置原理**
- @SpringBootApplication
- @EnableAutoConfiguration
- spring.factories
- 条件注解（@Conditional）

**Starter 原理**
- 自动依赖
- 自动配置

---

## 数据库

### 1. 索引

**索引类型**
- 主键索引
- 唯一索引
- 普通索引
- 组合索引
- 全文索引

**索引结构**
- B+树：MySQL InnoDB
- Hash：Memory 引擎
- 全文索引：FULLTEXT

**索引优化**
- 最左前缀原则
- 覆盖索引
- 索引下推
- 避免索引失效

### 2. 事务

**ACID**
- 原子性（Atomicity）
- 一致性（Consistency）
- 隔离性（Isolation）
- 持久性（Durability）

**隔离级别**
- 读未提交
- 读已提交（Oracle 默认）
- 可重复读（MySQL 默认）
- 串行化

**并发问题**
- 脏读
- 不可重复读
- 幻读

### 3. 锁

**锁类型**
- 共享锁（S锁）：读锁
- 排他锁（X锁）：写锁

**锁粒度**
- 表锁
- 行锁
- 间隙锁
- 临键锁

**MVCC（多版本并发控制）**
- Read View
- Undo Log
- 无锁读写

### 4. SQL 优化

**原则**
- 避免 SELECT *
- 合理使用索引
- 避免 LIKE '%xx%'
- 避免子查询，使用 JOIN
- 避免 OR，使用 UNION
- 避免字段计算
- 使用 LIMIT 分页

**Explain 分析**
- type：访问类型
- rows：扫描行数
- Extra：额外信息

### 5. 分库分表

**垂直分库**
- 按业务拆分

**水平分表**
- 按数据量拆分
- 分片键选择

**分库分表策略**
- Range：范围
- Hash：哈希
- 地理位置分片

**问题**
- 分布式事务
- 跨库 JOIN
- 全局唯一 ID

### 6. 主从复制

**原理**
1. 主库写 binlog
2. 从库读 binlog 写 relay log
3. 从库执行 relay log

**模式**
- 一主一从
- 一主多从
- 双主复制
- 级联复制

### 7. Redis

**数据类型**
- String：字符串
- Hash：哈希表
- List：列表
- Set：集合
- ZSet：有序集合

**持久化**
- RDB：快照
- AOF：日志

**场景**
- 缓存
- 分布式锁
- 计数器
- 排行榜
- 消息队列

---

## 算法与数据结构

### 1. 时间复杂度

- O(1)：常数
- O(log n)：对数
- O(n)：线性
- O(n log n)：线性对数
- O(n²)：平方
- O(2ⁿ)：指数
- O(n!)：阶乘

### 2. 数据结构

**线性结构**
- 数组
- 链表
- 栈
- 队列

**树**
- 二叉树
- 二叉搜索树
- 平衡树（AVL）
- 红黑树
- B 树、B+ 树
- 堆

**图**
- 邻接矩阵
- 邻接表

**哈希**
- 哈希表
- 解决冲突：拉链法、开放寻址法

### 3. 常见算法

**排序**
- 冒泡排序：O(n²)
- 选择排序：O(n²)
- 插入排序：O(n²)
- 快速排序：O(n log n)
- 归并排序：O(n log n)
- 堆排序：O(n log n)

**查找**
- 二分查找：O(log n)
- 哈希查找：O(1)
- BFS/DFS

**动态规划**
- 斐波那契
- 背包问题
- 最长公共子序列

**贪心**
- 霍夫曼编码
- 最小生成树

**回溯**
- N 皇后
- 全排列

---

## 系统设计

### 1. 设计原则

**SOLID**
- 单一职责
- 开闭原则
- 里氏替换
- 接口隔离
- 依赖倒置

**DDD（领域驱动设计）**
- 领域模型
- 领域服务
- 聚合根
- 值对象
- 仓储

### 2. 高并发

**缓存**
- 本地缓存
- 分布式缓存
- 缓存策略（穿透、击穿、雪崩）

**异步**
- 消息队列
- 异步编排

**限流**
- 令牌桶
- 漏桶
- 固定窗口
- 滑动窗口

**降级**
- 自动降级
- 手动降级

**熔断**
- 熔断器模式
- Hystrix、Sentinel

### 3. 高可用

**集群**
- 负载均衡
- 故障转移

**主从**
- 主备切换

**多活**
- 同城双活
- 异地多活

### 4. 分布式

**CAP**
- 一致性
- 可用性
- 分区容错
- 只能同时满足两个

**BASE**
- 基本可用
- 软状态
- 最终一致性

**分布式事务**
- 2PC（两阶段提交）
- 3PC（三阶段提交）
- TCC（Try、Confirm、Cancel）
- 本地消息表
- Saga

**分布式锁**
- Redis SETNX
- Redisson
- Zookeeper

**分布式 ID**
- UUID
- 雪花算法
- 数据库自增

**服务注册与发现**
- Eureka
- Nacos
- Consul
- Zookeeper

**配置中心**
- Spring Cloud Config
- Nacos
- Apollo

**消息队列**
- Kafka
- RocketMQ
- RabbitMQ

**RPC**
- Dubbo
- gRPC
- Feign

### 5. 微服务

**服务拆分**
- 按业务拆分
- 按数据拆分

**服务治理**
- 服务注册
- 负载均衡
- 熔断降级
- 限流

**服务监控**
- 日志
- 指标
- 链路追踪

**网关**
- 路由
- 鉴权
- 限流
- 熔断

---

## 附录：高频面试题

### Redis 相关
1. Redis 为什么快？
2. Redis 持久化方式
3. 缓存穿透、击穿、雪崩
4. Redis 分布式锁
5. Redis 数据淘汰策略

### MySQL 相关
1. InnoDB 与 MyISAM 区别
2. 索引失效场景
3. 事务隔离级别
4. MVCC 原理
5. 分库分表策略

### 并发相关
1. 线程池参数
2. ConcurrentHashMap 原理
3. volatile 关键字
4. CAS 与 AQS
5. 死锁条件

### JVM 相关
1. 垃圾回收算法
2. 类加载机制
3. JVM 内存模型
4. GC Root
5. 调优经验

### Spring 相关
1. IOC 原理
2. AOP 原理
3. Bean 生命周期
4. 事务传播行为
5. Spring Boot 自动配置

---

**最后更新：2026-03-03**
