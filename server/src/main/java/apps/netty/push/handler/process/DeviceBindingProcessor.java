package apps.netty.push.handler.process;

import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;
/**
 * 设备上线消息处理
 * 
 * @author mengxuanliang
 * 
 */
public class DeviceBindingProcessor extends AbstractHandleProcessor<DecPushProtoc.DeviceBinding> {

	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		// 获取设备id
		String alias = this.getProcessObject().getAlias();
		String registrationId = this.getProcessObject().getRegistrationId();
		String deviceId = this.getProcessObject().getDeviceId();
		// 设备上线
		//boolean b = this.applicationContext.online(deviceId);
		
		Map<String,String> returnMap= this.applicationContext.bindingDevice(registrationId,alias);
		
		DecPushProtoc.PushPojo resultMessage= applicationContext.createBindDeviceResultMessage(returnMap,deviceId,registrationId,alias);
		return resultMessage;
	}
}
