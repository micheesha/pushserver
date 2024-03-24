package apps.netty.push.pojo;

import java.util.List;

public class MessageInfo {
	
	private String _id;
	private String title;
	private String content;
	private int messageType;
	private String alias;//推送用
	private List<String> aliasList;//推送给多个设备
	private boolean isNeedReceipt;
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
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public List<String> getAliasList() {
		return aliasList;
	}
	public void setAliasList(List<String> aliasList) {
		this.aliasList = aliasList;
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
