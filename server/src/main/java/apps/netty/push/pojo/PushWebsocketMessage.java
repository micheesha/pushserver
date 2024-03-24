package apps.netty.push.pojo;

import java.util.Date;

public class PushWebsocketMessage {
	
	private String _id;
	
	private String from;
	
    public String text;
    
    public String title;
    
    private String uid;
    private boolean isNeedOfflineSent;
	
	private String status;
	
	private Date pushTime;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public boolean isNeedOfflineSent() {
		return isNeedOfflineSent;
	}

	public void setNeedOfflineSent(boolean isNeedOfflineSent) {
		this.isNeedOfflineSent = isNeedOfflineSent;
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

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	
	 
	
	
	
	
	
}
