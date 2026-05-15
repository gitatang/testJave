package game.protocol;

import game.netty.enums.MessageType;

public class LoginReq extends MessageBase {
	private String username;
	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public LoginReq(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public LoginReq() {
		setType(MessageType.LOGIN_REQ);
	}

	public LoginReq(long requestId){
		setType(MessageType.LOGIN_REQ);
		setRequestId(requestId);
	}
}
