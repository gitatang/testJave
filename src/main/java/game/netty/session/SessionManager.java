package game.netty.session;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Session 管理器，负责管理所有客户端连接会话
 */
public class SessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager INSTANCE = new SessionManager();

    // sessionId -> Session
    private final Map<Long, Session> sessionMap = new ConcurrentHashMap<>();
    // userId -> Session (假设一个用户只能有一个连接)
    private final Map<String, Session> userSessionMap = new ConcurrentHashMap<>();
    // Channel -> Session
    private final Map<Channel, Session> channelSessionMap = new ConcurrentHashMap<>();

    // 心跳检查定时器
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "session-heartbeat-checker");
        thread.setDaemon(true);
        return thread;
    });

    private SessionManager() {
        // 启动心跳检查，每30秒检查一次
        startHeartbeatCheck();
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /**
     * 添加会话
     */
    public Session addSession(Channel channel) {
        Session session = new Session(channel);
        sessionMap.put(session.getSessionId(), session);
        channelSessionMap.put(channel, session);
        LOGGER.info("新会话创建, sessionId={}, channel={}", session.getSessionId(), channel.id().asShortText());
        return session;
    }

    /**
     * 移除会话
     */
    public void removeSession(Session session) {
        if (session == null) {
            return;
        }

        sessionMap.remove(session.getSessionId());
        channelSessionMap.remove(session.getChannel());

        if (session.getUserId() != null) {
            userSessionMap.remove(session.getUserId());
        }

        LOGGER.info("会话移除, sessionId={}, userId={}", session.getSessionId(), session.getUserId());
    }

    /**
     * 根据 Channel 获取 Session
     */
    public Session getSession(Channel channel) {
        return channelSessionMap.get(channel);
    }

    /**
     * 根据 sessionId 获取 Session
     */
    public Session getSession(long sessionId) {
        return sessionMap.get(sessionId);
    }

    /**
     * 根据 userId 获取 Session
     */
    public Session getSessionByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    /**
     * 绑定用户
     */
    public void bindUser(Session session, String userId) {
        if (session == null || userId == null) {
            return;
        }

        // 如果该用户已有会话，先踢掉旧会话
        Session oldSession = userSessionMap.get(userId);
        if (oldSession != null && oldSession != session) {
            LOGGER.info("用户重复登录，踢出旧会话, userId={}, oldSessionId={}, newSessionId={}",
                    userId, oldSession.getSessionId(), session.getSessionId());
            oldSession.close();
        }

        session.setUserId(userId);
        userSessionMap.put(userId, session);
        LOGGER.info("用户绑定到会话, userId={}, sessionId={}", userId, session.getSessionId());
    }

    /**
     * 广播消息给所有会话
     */
    public void broadcast(byte version, byte type, long requestId, com.google.protobuf.MessageLite message) {
        Collection<Session> sessions = sessionMap.values();
        int successCount = 0;
        int failCount = 0;

        for (Session session : sessions) {
            if (session.isActive()) {
                session.sendMessage(version, type, requestId, message);
                successCount++;
            } else {
                failCount++;
            }
        }

        LOGGER.debug("广播消息完成, 总会话数={}, 成功={}, 失败={}", sessions.size(), successCount, failCount);
    }

    /**
     * 向指定用户列表发送消息（群发）
     */
    public void sendToUsers(byte version, byte type, long requestId, com.google.protobuf.MessageLite message, List<String> userIds) {
        int successCount = 0;
        int failCount = 0;

        for (String userId : userIds) {
            Session session = userSessionMap.get(userId);
            if (session != null && session.isActive()) {
                session.sendMessage(version, type, requestId, message);
                successCount++;
            } else {
                failCount++;
            }
        }

        LOGGER.debug("群发消息完成, 目标用户数={}, 成功={}, 失败={}", userIds.size(), successCount, failCount);
    }

    /**
     * 获取所有在线用户ID
     */
    public List<String> getOnlineUsers() {
        return userSessionMap.keySet().stream()
                .filter(userId -> {
                    Session session = userSessionMap.get(userId);
                    return session != null && session.isActive();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取在线会话数量
     */
    public int getOnlineSessionCount() {
        return (int) sessionMap.values().stream()
                .filter(Session::isActive)
                .count();
    }

    /**
     * 启动心跳检查
     */
    private void startHeartbeatCheck() {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long timeoutMillis = 120000; // 2分钟超时

                List<Session> timeoutSessions = sessionMap.values().stream()
                        .filter(session -> session.isTimeout(timeoutMillis))
                        .collect(Collectors.toList());

                for (Session session : timeoutSessions) {
                    LOGGER.warn("会话超时, 关闭连接, sessionId={}, userId={}, 最后心跳时间={}",
                            session.getSessionId(), session.getUserId(), session.getLastHeartbeatTime());
                    session.close();
                }

            } catch (Exception e) {
                LOGGER.error("心跳检查异常", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 关闭所有会话
     */
    public void closeAll() {
        LOGGER.info("关闭所有会话, 总会话数={}", sessionMap.size());
        sessionMap.values().forEach(Session::close);
        sessionMap.clear();
        userSessionMap.clear();
        channelSessionMap.clear();
        heartbeatScheduler.shutdown();
    }

    /**
     * 关闭 SessionManager
     */
    public void shutdown() {
        closeAll();
        try {
            heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("关闭 SessionManager 时被中断", e);
            Thread.currentThread().interrupt();
        }
    }
}