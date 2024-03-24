package com.netty.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.netty.client.android.NettyServerManager;
import com.netty.client.android.dao.Device;
import com.netty.client.android.handler.MessageObject;
import com.netty.client.android.handler.NettyProcessorHandler;
import com.netty.client.android.service.RemoteService;
import com.netty.client.consts.SystemConsts;
import com.netty.client.context.ApplicationContextClient;
import apps.netty.server.protoc.DecPushProtoc;
import apps.netty.server.protoc.DecPushProtoc.PushPojo;

/**
 * 客户端消息处理Handler
 * 
 * @author mengxuanliang
 * 
 */
public class PushMessageHandler extends SimpleChannelInboundHandler<DecPushProtoc.PushPojo> {
	private ApplicationContextClient applicationContextClient;

	private NettyServerManager mConnectionManager;
	private Handler mHandler;

	public PushMessageHandler(NettyServerManager connectionManager, Handler handler) {
		this.mConnectionManager = connectionManager;
		this.mHandler = handler;
		this.applicationContextClient = RemoteService.getApplicationContextClient();
	}

	/**
	 * 此方法会在连接到服务器后被调用
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// 激活后发送设备注册请求
		applicationContextClient.setCtx(ctx);
		// applicationContextClient.sendRegistrationMessage(null);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PushPojo t) throws Exception {
		try {
			if (SystemConsts.isDebug)
				Log.i(getClass().getName(), "channelRead0:" + t.toString());
		} catch (Exception e) {
			System.out.println("channelRead0:======================\n" + t);
		}
	}

	private void channelReadMe(ChannelHandlerContext ctx, PushPojo t) {
		try {
			if (SystemConsts.isDebug) {
				Log.i(getClass().getName(), "channelReadMe:" + t.toString());
			}
		} catch (Exception e) {
			System.out.println("channelReadMe:======================\n" + e.toString());
		}
		if (t != null) {
			DecPushProtoc.Type type = t.getType();
			int what = -1;
			Object obj = null;
			switch (type) {
			case DEVICE_REGISTRATION_RESULT:
				// 註冊結果消息
				DecPushProtoc.DeviceRegistrationResult registrationResult = t.getDeviceRegistrationResult();
				if (SystemConsts.isDebug)
					Log.i("regResult:msg--", registrationResult.toString());
				what = NettyProcessorHandler.REGISTRATION_RESULT;
				obj = registrationResult;
				break;
			case DEVICE_ONLINE_RESULT:
				// 上下线结果消息
				DecPushProtoc.DeviceOnLineResult deviceOnoffResult = t.getDeviceOnLineResult();
				if (deviceOnoffResult != null) {
					// 上线结果消息
					what = NettyProcessorHandler.DEVICE_ONLINE_RESULT;
					obj = deviceOnoffResult.getResultCode();

					if (deviceOnoffResult.getResultCode() == DecPushProtoc.ResultCode.SUCCESS) {
						// 更新设备信息
						Device device = applicationContextClient.getDeviceInfoByAppPackage(mConnectionManager.getContext().getPackageName());
						if (device != null) {
							device.setIsOnline(ApplicationContextClient.DEVICE_ONLINE);
							applicationContextClient.saveOrUpdateDevice(device);
						}
					}
				}
				break;
			case DEVICE_OFFLINE_RESULT:
				// 上下线结果消息
				DecPushProtoc.DeviceOffLineResult deviceOffLineResult = t.getDeviceOffLineResult();
				if (deviceOffLineResult != null) {
					// 下线结果消息
					what = NettyProcessorHandler.DEVICE_OFFLINE_RESULT;
					obj = deviceOffLineResult.getResultCode();
					 
					if (deviceOffLineResult.getResultCode() == DecPushProtoc.ResultCode.SUCCESS) {
						// 更新设备信息
						Device device = applicationContextClient.getDeviceInfoByAppPackage(mConnectionManager.getContext().getPackageName());
						if (device != null) {
							// 删除设备信息 不会发送消息了 （客户端中设备 在线状态与服务端设备状态时不一样的：客户端设备下线标识需要向发送登陆消息 ，上线成功后不需要重复发送登陆请求，而不需要发送消息的时候需要将客户端service数据缓存清除
							// ，服务端设备上下线仅更新状态内容，不会删除信息）
							//applicationContextClient.deleteDeviceInfo(device);
							device.setIsOnline(ApplicationContextClient.DEVICE_OFFLINE);
							applicationContextClient.saveOrUpdateDevice(device);
						}
					}
					break;
				}
				break;
			case DEVICE_BINDING_RESULT:
				// 绑定结果消息
				DecPushProtoc.DeviceBindingResult deviceBindingResult = t.getDeviceBindingResult();
				if (deviceBindingResult != null) {
					if (SystemConsts.isDebug)
						Log.i("绑定结果消息", deviceBindingResult.getResultCode() + ", " + deviceBindingResult.getAlias());
					what = NettyProcessorHandler.DEVICE_BINDING_RESULT;
					obj = deviceBindingResult.getResultCode();
					break;
				}
				break;
			case PUSH_MESSAGE:
				// 消息处理
				DecPushProtoc.PushMessage commandMessage = t.getPushMessage();
				if (commandMessage != null) {
					// 判断是否需要发送回执消息
					if (commandMessage.getIsNeedReceipt()) {
						Device deviceInfo = applicationContextClient.getDeviceInfoByAppPackage(mConnectionManager.getContext().getPackageName());
						//Device deviceInfo = null;
						// 收到消息 需要通知服务器 发送消息回执内容
						applicationContextClient.sendReceiptMessage(deviceInfo, commandMessage.getMsgId());
					}
					what = NettyProcessorHandler.MESSAGE;
					obj = commandMessage;
				}
				break;
			default:
				if (SystemConsts.isDebug)
					Log.i("handler", "default");
				break;
			}
			if (SystemConsts.isDebug)
				Log.i(getClass().getName(), "what=" + what);
			if (mHandler != null) {
				MessageObject mo = new MessageObject();
				mo.setAppPackage(mConnectionManager.getContext().getPackageName());
				mo.setObj(obj);
				// handler sendMessage
				Message message = mHandler.obtainMessage();
				message.what = what;
				message.obj = mo;
				mHandler.sendMessage(message);
			}
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
		if (obj instanceof PushPojo) {
			channelReadMe(ctx, (DecPushProtoc.PushPojo) obj);
		}
	}

	/**
	 * 捕捉到异常
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		if (SystemConsts.isDebug)
			Log.i(getClass().getName(), cause.getMessage());
		cause.printStackTrace();
		/**
		 * 异常关闭连接启动重连操作
		 */
		// 首先更新连接状态
		mConnectionManager.setConnectState(NettyServerManager.CONNECT_EXCEPTION);
		// 更新设备状态 信息为离线
		applicationContextClient.offlineAllDevices();
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (SystemConsts.isDebug)
			Log.i(getClass().getName(), "channelInactive");
		/**
		 * 异常关闭连接启动重连操作
		 */
		// 首先更新连接状态
		mConnectionManager.setConnectState(NettyServerManager.CONNECT_CLOSED);
		// 更新设备状态 信息为离线
		applicationContextClient.offlineAllDevices();
		ctx.close();
	}

}
