package apps.netty.push.handler.process;

import java.util.Map;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 设备注册消息请求
 * 
 * @author mengxuanliang
 * 
 */
public class RegistrationProcessorNew extends AbstractHandleProcessor<DecPushProtoc.DeviceRegistration> {

	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		// 服务器解析注册数据请求
		// 判断注册是否成功
		DecPushProtoc.DeviceRegistration registration = this.getProcessObject();
		Map<String,Object> returnMap = applicationContext.registDeviceNew(ctx.channel(), registration);
		
		DecPushProtoc.PushPojo pushMessage= applicationContext.createCommandRegistrationResultNew(registration.getAppKey(), registration.getAppPackage(), returnMap);
		
		if(returnMap.containsKey("errorCode") || returnMap.containsKey("errorMessage")){
			ctx.writeAndFlush(pushMessage);
			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
			return null;
		}else {
			return applicationContext.createCommandRegistrationResultNew(registration.getAppKey(), registration.getAppPackage(), returnMap);
		}
		
	}
}
