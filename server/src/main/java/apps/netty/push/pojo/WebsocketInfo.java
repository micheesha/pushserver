package apps.netty.push.pojo;

import java.util.Date;

/**
 * 设备信息
 * 
 * @author mengxuanliang
 * 
 */
public class WebsocketInfo {
	
	private String _id;
	
	private String uid ;
	private String isOnline ;
	private Date onlineTime ;
	private Date offlineTime ;
	
	private String websocketServerAddress;

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getIsOnline() {
		return isOnline;
	}

	public void setIsOnline(String isOnline) {
		this.isOnline = isOnline;
	}

	public Date getOnlineTime() {
		return onlineTime;
	}

	public void setOnlineTime(Date onlineTime) {
		this.onlineTime = onlineTime;
	}

	public Date getOfflineTime() {
		return offlineTime;
	}

	public void setOfflineTime(Date offlineTime) {
		this.offlineTime = offlineTime;
	}

	public String getWebsocketServerAddress() {
		return websocketServerAddress;
	}

	public void setWebsocketServerAddress(String websocketServerAddress) {
		this.websocketServerAddress = websocketServerAddress;
	}
	
	
	
	
	
}
