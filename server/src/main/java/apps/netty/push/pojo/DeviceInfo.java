package apps.netty.push.pojo;

import java.util.Date;

/**
 * 设备信息
 * 
 * @author mengxuanliang
 * 
 */
public class DeviceInfo {
	
	private String _id;
	
	private String regId ;
	private Long userId ;
	private Long appId ;
	private String appKey ;
	private String appPackage;
	private String isOnline ;
	private String deviceId ;
	private String imei ;
	private String channel ;
	private Date onlineTime ;
	private Date offlineTime ;
	
	private String tag;//组，标签
	private String alias;//别名
	
	private String tcpServerAddress;
	
	private String status;
	
	public String getRegId() {
		return regId;
	}
	public void setRegId(String regId) {
		this.regId = regId;
	}
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public Long getAppId() {
		return appId;
	}
	public void setAppId(Long appId) {
		this.appId = appId;
	}
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
	}
	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel;
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
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String getIsOnline() {
		return isOnline;
	}
	public void setIsOnline(String isOnline) {
		this.isOnline = isOnline;
	}
	public String getTcpServerAddress() {
		return tcpServerAddress;
	}
	public void setTcpServerAddress(String tcpServerAddress) {
		this.tcpServerAddress = tcpServerAddress;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getAppPackage() {
		return appPackage;
	}
	public void setAppPackage(String appPackage) {
		this.appPackage = appPackage;
	}
	@Override
	public String toString() {
		return "DeviceInfo [_id=" + _id + ", regId=" + regId + ", userId=" + userId + ", appId=" + appId + ", appKey="
				+ appKey + ", appPackage=" + appPackage + ", isOnline=" + isOnline + ", deviceId=" + deviceId
				+ ", imei=" + imei + ", channel=" + channel + ", onlineTime=" + onlineTime + ", offlineTime="
				+ offlineTime + ", tag=" + tag + ", alias=" + alias + ", tcpServerAddress=" + tcpServerAddress
				+ ", status=" + status + "]";
	}
	
	
	
	
}
