package apps.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 设备下线消息处理
 * 
 * @author mengxuanliang
 * 
 */
public class DeviceOfflineProcessor extends AbstractHandleProcessor<DecPushProtoc.DeviceOffline> {

	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		 
		// 获取设备id
		String deviceId = this.getProcessObject().getDeviceId();
		
		// 设备上线
		applicationContext.offline(deviceId);
		
		DecPushProtoc.ResultCode resultCode =  DecPushProtoc.ResultCode.SUCCESS;
		
		return this.applicationContext.createCommandDeviceOffLineResult(deviceId, resultCode,"");
	}
}
