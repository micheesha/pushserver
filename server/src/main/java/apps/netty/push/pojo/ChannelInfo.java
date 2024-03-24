package apps.netty.push.pojo;

import java.util.List;

import io.netty.channel.Channel;

/**
 * 渠道信息
* 
* @类名称：ChannelInfo   
* @类描述：   
* @创建人：mengxuanliang   
* @创建时间：2014-10-13 下午3:16:13
*
 */
public class ChannelInfo {
	// 心跳时间
	private Long heartTime;

	private Channel channel;

	public Long getHeartTime() {
		return heartTime;
	}

	public void setHeartTime(Long heartTime) {
		this.heartTime = heartTime;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	
	
}
