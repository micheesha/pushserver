package apps.netty.push.pojo;

import java.util.Date;

public class PushMessageInfo {
	
	private String _id;//mongodb id
	private String title;
	private String content;
	private int messageType;
	private String status;
	private String alias;//推送用
	private String deviceId;
	private boolean isNeedReceipt;
	
	private Date pushTime;
	private Date receiptReceivedTime;
	private Date createdDate;
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public int getMessageType() {
		return messageType;
	}
	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public boolean isNeedReceipt() {
		return isNeedReceipt;
	}
	public void setNeedReceipt(boolean isNeedReceipt) {
		this.isNeedReceipt = isNeedReceipt;
	}
	public Date getPushTime() {
		return pushTime;
	}
	public void setPushTime(Date pushTime) {
		this.pushTime = pushTime;
	}
	public Date getReceiptReceivedTime() {
		return receiptReceivedTime;
	}
	public void setReceiptReceivedTime(Date receiptReceivedTime) {
		this.receiptReceivedTime = receiptReceivedTime;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	
	
	
}
