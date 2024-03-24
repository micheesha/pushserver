package apps.netty.push.handler.process.factory;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import io.netty.channel.ChannelHandlerContext;
import apps.netty.push.handler.process.DeviceBindingProcessor;
import apps.netty.push.handler.process.DeviceOfflineProcessor;
import apps.netty.push.handler.process.DeviceOnlineProcessor;
import apps.netty.push.handler.process.HeartbeatProcessor;
import apps.netty.push.handler.process.IHandleProcessor;
import apps.netty.push.handler.process.MessageReceiptProcessor;
import apps.netty.push.handler.process.RegistrationProcessorNew;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 请求处理工厂接口实现类
 * 
 * @author mengxuanliang
 * @param <T>
 * 
 */
@Service("handleProcessorFactory")
@Scope("singleton")
public class HandleProcessorFactory implements IHandleProcessorFactory {
	// 处理器集合

	private static Map<DecPushProtoc.Type, IHandleProcessor> processors = new HashMap<DecPushProtoc.Type, IHandleProcessor>();

	@Override
	public IHandleProcessor findHandleProcessor(ChannelHandlerContext ctx, Object msg) {
		if (msg == null) {
			return null;
		}

		if (msg instanceof DecPushProtoc.PushPojo) {

			DecPushProtoc.PushPojo pushMessage = (DecPushProtoc.PushPojo) msg;
			DecPushProtoc.Type type = pushMessage.getType();
			if (type == null) {
				return null;
			}

			IHandleProcessor handleProcessor = null;
			synchronized (processors) {
				handleProcessor = processors.get(type);
				Object obj = null;
				boolean isNew = handleProcessor == null ? true : false;
				switch (type) {
					case HEART_BEAT:
						// 心跳处理
						obj = pushMessage.getHeartBeat();
						if (handleProcessor == null) {
							handleProcessor = new HeartbeatProcessor();
						}
						break;
					case DEVICE_ONLINE:
						// 设备上线
						obj = pushMessage.getDeviceOnline();
						if (handleProcessor == null) {
							handleProcessor = new DeviceOnlineProcessor();
						}
						break;
					case DEVICE_OFFLINE:
						// 设备下线
						obj = pushMessage.getDeviceOffline();
						if (handleProcessor == null) {
							handleProcessor = new DeviceOfflineProcessor();
						}
						break;
					case DEVICE_REGISTRATION:
						// 设备注册
						obj = pushMessage.getDeviceRegistration();
						if (handleProcessor == null) {
							//handleProcessor = new RegistrationProcessor();
							handleProcessor = new RegistrationProcessorNew();
						}
						break;
					case PUSH_MESSAGE_RECEIPT:
						// 消息回执
						obj = pushMessage.getPushMessageReceipt();
						if (handleProcessor == null) {
							handleProcessor = new MessageReceiptProcessor();
						}
						break;
					case DEVICE_BINDING:
						// 消息回执
						obj = pushMessage.getDeviceBinding();
						if (handleProcessor == null) {
							handleProcessor = new DeviceBindingProcessor();
						}
						break;
					default:
						break;
				}
				if (isNew) {
					// 设置处理器集合
					processors.put(type, handleProcessor);
				}
				handleProcessor.updateObject(obj);
			}
			return handleProcessor;
		}
		return null;
	}
}
