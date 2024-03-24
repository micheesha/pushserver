package apps.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 客户端心跳处理
 * 
 * @author mengxuanliang
 * 
 */
public class HeartbeatProcessor extends AbstractHandleProcessor<DecPushProtoc.HeartBeat> {

	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		
		String deviceId = this.getProcessObject().getDeviceId();
		
		applicationContext.refreshHeart(deviceId,ctx.channel());
		 
		return null;
	}

}
