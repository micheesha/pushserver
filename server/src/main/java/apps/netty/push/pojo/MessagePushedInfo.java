package apps.netty.push.pojo;

import java.util.Date;

/**
 * 已经推送消息信息
 * @author mengxuanliang
 *
 */
public class MessagePushedInfo {
	private String _id;
	private String msgId ;
	private String deviceId ;
	private String status ;
	private Date pushTime ;
	private Date receiptTime;
	
	private String alias;
	
	private boolean isNeedReceipt;
	
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getPushTime() {
		return pushTime;
	}
	public void setPushTime(Date pushTime) {
		this.pushTime = pushTime;
	}
	public Date getReceiptTime() {
		return receiptTime;
	}
	public void setReceiptTime(Date receiptTime) {
		this.receiptTime = receiptTime;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public boolean isNeedReceipt() {
		return isNeedReceipt;
	}
	public void setNeedReceipt(boolean isNeedReceipt) {
		this.isNeedReceipt = isNeedReceipt;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	
}
