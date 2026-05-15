package game.protocol;

import game.netty.enums.MessageType;

public class LoginReqV2 extends MessageBase {
	private String username;
	private String password;
	private String deviceId;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public LoginReqV2(String username, String password, String deviceId) {
		this.username = username;
		this.password = password;
		this.deviceId = deviceId;
	}

	public LoginReqV2() {
		setType(MessageType.LOGIN_REQ_V2);
	}
	public LoginReqV2(long requestId) {
		setType(MessageType.LOGIN_REQ_V2);
		setRequestId(requestId);
	}
}
