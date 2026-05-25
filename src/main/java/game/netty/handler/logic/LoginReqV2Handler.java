package game.netty.handler.logic;

import com.game.UserProto;
import game.netty.session.Session;
import game.netty.session.SessionManager;
import game.protocol.MessageHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录请求处理器 V2
 * 展示如何使用 Session 和广播功能
 */
public class LoginReqV2Handler implements MessageHandler<UserProto.LoginReqV2> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginReqV2Handler.class);
    private static final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public Class<UserProto.LoginReqV2> messageClass() {
        return UserProto.LoginReqV2.class;
    }

    @Override
    public void handler(ChannelHandlerContext ctx, UserProto.LoginReqV2 msg) {
        Session session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            LOGGER.error("Session 不存在，无法处理登录");
            return;
        }

        String username = msg.getUsername();
        String password = msg.getPassword();

        LOGGER.info("收到登录请求, sessionId={}, username={}", session.getSessionId(), username);

        // TODO: 这里应该验证用户名和密码
        // 为了演示，我们简单处理：只要请求就认为登录成功

        // 绑定用户到 Session
        sessionManager.bindUser(session, username);

        // 构建登录响应
		UserProto.LoginReqV2.Builder builder = UserProto.LoginReqV2.newBuilder();
		builder.setUsername(username);
		builder.setPassword(password);


        // 发送登录响应给客户端
        // 假设响应类型是 LOGIN_RES_V2（需要在 MessageType 中定义）
        session.sendMessage((byte) 2, (byte) 0x12, 0, builder.build());

        LOGGER.info("用户登录成功, username={}, sessionId={}", username, session.getSessionId());

        // 示例：广播消息给所有在线用户
        broadcastUserLogin(username);

        // 示例：获取在线用户列表
        var onlineUsers = sessionManager.getOnlineUsers();
        LOGGER.info("当前在线用户数: {}", onlineUsers.size());
    }

    /**
     * 广播用户登录消息
     */
    private void broadcastUserLogin(String username) {
        UserProto.ChatMessage broadcastMsg = UserProto.ChatMessage.newBuilder()
                .setSender("系统")
                .setContent("用户 " + username + " 上线了")
                .setTimestamp(System.currentTimeMillis())
                .build();

        // 广播给所有在线用户
        sessionManager.broadcast((byte) 1, (byte) 0x20, 0, broadcastMsg);
    }
}