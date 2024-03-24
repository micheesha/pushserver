package apps.netty.push.pojo;

import java.util.List;

/**
 * Message for websocket
 *
 */
public class WebsocketMessage {


    public String from;

    public String fromName;

    public String to;

    public String text;
    
    public String title;
    
    private String uid;
    
    private List<String> uidList;
    
    private boolean isNeedOfflineSent;
    
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
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

	public List<String> getUidList() {
		return uidList;
	}

	public void setUidList(List<String> uidList) {
		this.uidList = uidList;
	}

	public boolean isNeedOfflineSent() {
		return isNeedOfflineSent;
	}

	public void setNeedOfflineSent(boolean isNeedOfflineSent) {
		this.isNeedOfflineSent = isNeedOfflineSent;
	}

	
    
    
    
}
