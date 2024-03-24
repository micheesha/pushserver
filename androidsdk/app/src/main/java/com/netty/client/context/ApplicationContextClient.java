package com.netty.client.context;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.netty.client.android.dao.Device;
import com.netty.client.android.handler.NettyProcessorHandler;
import com.netty.client.android.listener.INettyHandlerListener;
import com.netty.client.android.listener.RegistrationResultListener;
import com.netty.client.android.service.PushDbService;
import com.netty.client.consts.SystemConsts;
import com.netty.client.utils.Md5Util;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 客户端调用
 *
 * @author mengxuanliang
 *
 */
public class ApplicationContextClient {

	// 设备在线状态
	public static final int DEVICE_ONLINE = 1;
	public static final int DEVICE_OFFLINE = 0;

	// 是否关闭状态
	public static boolean isClosed = false;
	private ChannelHandlerContext ctx;
	private Map<String, Device> deviceInfos = new HashMap<String, Device>();

	// 保存handler 回调Listener
	@SuppressWarnings("rawtypes")
	private Map<String, Map<Integer, INettyHandlerListener>> nettyHandlerListeners = new HashMap<String, Map<Integer, INettyHandlerListener>>();

	private PushDbService pushDbService;
	private Context mContext;

	public ApplicationContextClient(Context context) {
		this.mContext = context;
		this.pushDbService = PushDbService.getInstance(context);
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public void writeAndFlush(DecPushProtoc.PushPojo pushMessage) {
		if (this.ctx != null) {
			this.ctx.writeAndFlush(pushMessage);
		}
	}

	public Map<String, Device> getDeviceInfos() {
		if (this.pushDbService != null && (deviceInfos == null || deviceInfos.isEmpty())) {
			Map<String, Device> map = this.pushDbService.queryDevicesForMap();
			if (map != null && map.size() > 0) {
				deviceInfos.putAll(map);
			}
		}
		return deviceInfos;
	}

	/**
	 * 根据app包名获取设备信息
	 *
	 * @param appPackage
	 * @return
	 */
	public Device getDeviceInfoByAppPackage(String appPackage) {
		Map<String, Device> map = this.getDeviceInfos();
		if (map != null && !map.isEmpty()) {
			return map.get(appPackage);
		}
		return null;
	}

	/**
	 * 註冊設備
	 *
	 * @param device 设备信息
	 * @return
	 */
	public void saveOrUpdateDevice(Device device) {
		if (device != null) {
		    if (!deviceInfos.containsKey(device.getAppPackage()))
			    deviceInfos.put(device.getAppPackage(), device);
			this.pushDbService.saveOrUpdateDevice(device);
		}
	}

	/**
	 * 生成Device对象
	 *
	 * @param appKey
	 * @param appPackage
	 * @return
	 */

	public Device makeDevice(String appKey, String appPackage) {

		Device deviceInfo = this.getDeviceInfoByAppPackage(appPackage);
		if (deviceInfo == null || (deviceInfo.getAppKey() != null && !deviceInfo.getAppKey().equals(appKey))) {
			/*TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			String imei = null;
			if (tm != null) {
				try {
					imei = tm.getDeviceId();
				} catch (Exception e) {
					imei = "";
				}
			}*/
			String model = Build.MODEL;
			/*String macAddress = null;
			WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = (null == wifiMgr ? null : wifiMgr.getConnectionInfo());
			if (null != info) {
				macAddress = info.getMacAddress();
			}*/

			String deviceId = Md5Util.toMD5(SystemConsts.CHANNEL + appKey + model + appPackage);
			if (deviceInfo == null) {
				deviceInfo = new Device();
			}
			deviceInfo.setAppKey(appKey);
			deviceInfo.setAppPackage(appPackage);
			deviceInfo.setDeviceId(deviceId);
			deviceInfo.setImei(model);
			deviceInfo.setIsOnline(DEVICE_OFFLINE);
		}
		return deviceInfo;
	}

	/**
	 * 删除设备
	 * 
	 * @param
	 */
	public void deleteDeviceInfo(Device deviceInfo) {
		if (deviceInfo != null) {
			this.pushDbService.deleteDevice(deviceInfo);
			// 删除缓存内容
			deviceInfos.remove(deviceInfo.getAppPackage());
		}
	}

	public void offlineAllDevices() {
		if (deviceInfos != null && !deviceInfos.isEmpty()) {
			List<Device> list = new ArrayList<Device>();
			Iterator<Map.Entry<String, Device>> iterator = deviceInfos.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String, Device> entry = iterator.next();
				Device device = entry.getValue();
				if (device != null) {
					device.setIsOnline(DEVICE_OFFLINE);
					list.add(device);
				}
			}
			this.pushDbService.saveOrUpdateDevices(list);
			list = null;
		}

	}

	@SuppressWarnings("rawtypes")
	public void registListener(String appPackage, Integer type, INettyHandlerListener listener) {
		if (listener != null) {
			Map<Integer, INettyHandlerListener> mNettyHandlerListeners = nettyHandlerListeners.get(appPackage);
			//INettyHandlerListener nettyHandlerListener = null;
			if (mNettyHandlerListeners == null) {
				mNettyHandlerListeners = new HashMap<>();
				nettyHandlerListeners.put(appPackage, mNettyHandlerListeners);
			}/* else {
				if (nettyHandlerListener != null) {
					nettyHandlerListener = mNettyHandlerListeners.get(type);
					nettyHandlerListener = null;
				}
			}*/
			mNettyHandlerListeners.put(type, listener);
		}
	}

	@SuppressWarnings("rawtypes")
	public INettyHandlerListener getNettyHandlerListener(String appPackage, Integer type) {
		if (appPackage != null && type != null && nettyHandlerListeners != null && nettyHandlerListeners.containsKey(appPackage)) {
			Map<Integer, INettyHandlerListener> mNettyHandlerListeners = nettyHandlerListeners.get(appPackage);
			return mNettyHandlerListeners == null ? null : mNettyHandlerListeners.get(type);
		}
		return null;
	}

	/**
	 * 心跳请求
	 * 
	 * @param
	 */
	public void sendHeartBeatMessage(Context context) {
	    if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendHeartBeatMessage");
		Device device = getDeviceInfoByAppPackage(context.getPackageName());
		if (ctx != null && device != null) {
			// 心跳请求
			DecPushProtoc.PushPojo builder = this.createHeatBeatMessage(device.getDeviceId());
			ctx.writeAndFlush(builder);
		}
	}

	/**
	 * 设备注册请求
	 * 
	 */
	public void sendRegistrationMessage(Device deviceInfo, INettyHandlerListener<DecPushProtoc.DeviceRegistrationResult> listener) {
	    if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendRegistrationMessage");
		// Log.i(ApplicationContextClient.class.getName(),"sendRegistrationMessage");
		// 激活后发送设备注册请求
		if (deviceInfo != null) {
			if (listener == null && this.getNettyHandlerListener(deviceInfo.getAppPackage(), NettyProcessorHandler.REGISTRATION_RESULT) == null) {
				this.registListener(deviceInfo.getAppPackage(), NettyProcessorHandler.REGISTRATION_RESULT, new RegistrationResultListener(mContext, deviceInfo));
			} else {
				this.registListener(deviceInfo.getAppPackage(), NettyProcessorHandler.REGISTRATION_RESULT, listener);
			}
			DecPushProtoc.PushPojo pushMessage = this.createCommandRegistration(deviceInfo.getImei(), deviceInfo.getDeviceId(), deviceInfo.getAppKey(),
					deviceInfo.getAppPackage());
			ctx.writeAndFlush(pushMessage);
		}
	}

	/**
	 * 发送设备上线消息
	 *
	 * @param deviceInfo
	 * @param listener
	 */
	public void sendDeviceOnlineMessage(Device deviceInfo, INettyHandlerListener<DecPushProtoc.DeviceOnLineResult> listener) {
		if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendDeviceOnlineMessage");
		if (deviceInfo != null) {
			this.registListener(deviceInfo.getAppPackage(), NettyProcessorHandler.DEVICE_ONLINE_RESULT, listener);
			DecPushProtoc.PushPojo pushMessage = this.createCommandDeviceOnline(deviceInfo.getDeviceId());
			ctx.writeAndFlush(pushMessage);
		}
	}

	/**
	 * 绑定设备别名
	 * 
	 * @param alias 别名
	 * @param regId 注册ID
	 */
	public void sendBindingAliasToDeviceMessage(String alias,String regId) {
		if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendBindingAliasToDeviceMessage");
		//this.registListener(deviceInfo.getAppPackage(), NettyProcessorHandler.DEVICE_OFFLINE_RESULT, listener);
		DecPushProtoc.PushPojo pushMessage = this.createCommandBindingAliasToDeviceMessage(alias,regId);
		ctx.writeAndFlush(pushMessage);
	}

	/**
	 * 发送绑定别名到设备的消息
	 *
	 * @param deviceInfo
	 * @param listener
	 */
	public void sendDeviceOfflineMessage(Device deviceInfo, INettyHandlerListener<DecPushProtoc.ResultCode> listener) {
		if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendDeviceOfflineMessage");
		if (deviceInfo != null) {
			this.registListener(deviceInfo.getAppPackage(), NettyProcessorHandler.DEVICE_OFFLINE_RESULT, listener);
			DecPushProtoc.PushPojo pushMessage = this.createCommandDeviceOffline(deviceInfo.getDeviceId());
			ctx.writeAndFlush(pushMessage);
		}
	}


	/**
	 * 发送消息确认回执消息
	 *
	 * @param msgId
	 */
	public void sendReceiptMessage(Device deviceInfo, String msgId) {
		if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "sendReceiptMessage");
		if (deviceInfo != null) {
			DecPushProtoc.PushPojo pushMessage = this.createCommandMessageReceipt(deviceInfo.getAppKey(), deviceInfo.getDeviceId(), deviceInfo.getRegId(), msgId);
			ctx.writeAndFlush(pushMessage);
		}
	}

	/**
	 * 创建Registration对象
	 *
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandRegistration(String imei, String deviceId, String appKey, String appPackage) {
		DecPushProtoc.DeviceRegistration.Builder builder = DecPushProtoc.DeviceRegistration.newBuilder();
		builder.setImei(imei);
		builder.setDeviceId(deviceId);
		builder.setAppKey(appKey);
		builder.setAppPackage(appPackage);
		builder.setDeviceType(SystemConsts.CHANNEL);
		DecPushProtoc.DeviceRegistration commandProtoc = builder.build();

		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_REGISTRATION);
		messageBuilder.setDeviceRegistration(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 * 创建DeviceOnline对象
	 *
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandDeviceOnline(String deviceId) {
		DecPushProtoc.DeviceOnline.Builder builder = DecPushProtoc.DeviceOnline.newBuilder();
		//TODO online message
		builder.setDeviceId(deviceId);
		DecPushProtoc.DeviceOnline commandProtoc = builder.build();

		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_ONLINE);
		messageBuilder.setDeviceOnline(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 * 创建DeviceOffline对象
	 *
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandDeviceOffline(String deviceId) {
		DecPushProtoc.DeviceOffline.Builder builder = DecPushProtoc.DeviceOffline.newBuilder();
		builder.setDeviceId(deviceId);
		DecPushProtoc.DeviceOffline commandProtoc = builder.build();

		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_OFFLINE);
		messageBuilder.setDeviceOffline(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 *
	 * @param alias
	 * @param regId
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandBindingAliasToDeviceMessage(String alias,String regId) {
		DecPushProtoc.DeviceBinding.Builder builder = DecPushProtoc.DeviceBinding.newBuilder();
		builder.setRegistrationId(regId);
		builder.setAlias(alias);
		DecPushProtoc.DeviceBinding commandProtoc = builder.build();

		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_BINDING);
		messageBuilder.setDeviceBinding(commandProtoc);
		return messageBuilder.build();
	}


	/**
	 * 创建MessageReceipt对象
	 *
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandMessageReceipt(String appKey, String deviceId, String registrationId, String msgId) {
		DecPushProtoc.PushMessageReceipt.Builder builder = DecPushProtoc.PushMessageReceipt.newBuilder();
		builder.setAppKey(appKey);
		builder.setRegistrationId(registrationId);
		builder.setDeviceId(deviceId);
		builder.setMsgId(msgId);
		DecPushProtoc.PushMessageReceipt commandProtoc = builder.build();
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.PUSH_MESSAGE_RECEIPT);
		messageBuilder.setPushMessageReceipt(commandProtoc);
		return messageBuilder.build();
	}

	/**
	 * 创建心跳消息对象
	 *
	 * @param deviceId 设备标识
	 * @return
	 */
	private DecPushProtoc.PushPojo createHeatBeatMessage(String deviceId) {
		DecPushProtoc.HeartBeat.Builder msg = DecPushProtoc.HeartBeat.newBuilder();
		msg.setDeviceId(deviceId);
		DecPushProtoc.PushPojo.Builder builder = this.createCommandPushMessage(DecPushProtoc.Type.HEART_BEAT);
		builder.setHeartBeat(msg.build());
		return builder.build();
	}

	/**
	 * 创建发送消息对象
	 * 
	 * @param type
	 * @return
	 */
	private DecPushProtoc.PushPojo.Builder createCommandPushMessage(DecPushProtoc.Type type) {
		DecPushProtoc.PushPojo.Builder builder = DecPushProtoc.PushPojo.newBuilder();
		builder.setType(type);
		return builder;
	}

	public void destory() {
		isClosed = true;
		if (this.deviceInfos != null) {
			this.deviceInfos.clear();
			this.deviceInfos = null;
		}

		if (nettyHandlerListeners != null) {
			nettyHandlerListeners.clear();
			nettyHandlerListeners = null;
		}

		pushDbService = null;
		if (ctx != null) {
			ctx.close();
			ctx = null;
		}
	}
}
