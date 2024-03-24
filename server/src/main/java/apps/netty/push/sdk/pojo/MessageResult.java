package apps.netty.push.sdk.pojo;

import java.io.Serializable;

/**
 * 消息结果
 * 
 * @类名称：MessageResult
 * @类描述：
 * @创建人：mengxuanliang
 * @创建时间：2014-10-30 下午4:25:23
 * 
 */
public class MessageResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3628849782554026441L;
	private String msgId;
	private boolean success;


	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

}
