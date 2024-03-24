package apps.netty.push.handler.process;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 设备上线消息处理
 * 
 * @author mengxuanliang
 * 
 */
public class DeviceOnlineProcessor extends AbstractHandleProcessor<DecPushProtoc.DeviceOnline> {

	private static Logger logger = LoggerFactory.getLogger(DeviceOnlineProcessor.class);
	
	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		// 获取设备id
		String deviceId = this.getProcessObject().getDeviceId();
		 
		// 设备上线
		Map<String,String> returnMap = this.applicationContext.online(ctx.channel(),deviceId);
		
		DecPushProtoc.ResultCode resultCode =  DecPushProtoc.ResultCode.SUCCESS;
		if(returnMap.containsKey("errorMessage")){
			resultCode =  DecPushProtoc.ResultCode.FAILED;
		}
		
		DecPushProtoc.PushPojo onlineResult = this.applicationContext.createCommandDeviceOnLineResult(deviceId, resultCode,returnMap.get("errorMessage"));
		logger.info("writeAndFlush DeviceOnoffResult:=======================\n" + onlineResult);
		ctx.writeAndFlush(onlineResult);
		
		if(resultCode ==  DecPushProtoc.ResultCode.FAILED){
			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
		}else {
			try{
				applicationContext.sendOfflineMessageToDevice(deviceId);
			}catch(Exception e){
				logger.error("exception",e);
			}
		}
		
		return null;
	}
}
