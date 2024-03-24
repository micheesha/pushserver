package com.netty.client.android.listener;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.netty.client.android.dao.Device;
import com.netty.client.android.service.RemoteService;
import com.netty.client.consts.SystemConsts;
import com.netty.client.context.ApplicationContextClient;

/**
 * 服务器连接成功 回調處理
 * 
 * @类名称：ConnectionListener
 * @类描述：
 * @创建人：mengxuanliang
 * @创建时间：2014-10-20 上午9:31:31
 * 
 */
public class ConnectionListener implements INettyHandlerListener<Device> {
	private ApplicationContextClient applicationContextClient = RemoteService.getApplicationContextClient();

	private Device deviceInfo;

	private Context mContext;

	public ConnectionListener(Context context, String appKey, String appPackage) {
		if (SystemConsts.isDebug)
			Log.i(ApplicationContextClient.class.getName(), "ConnectionListener");
		mContext = context;
		// 获取设备缓存信息
		deviceInfo = applicationContextClient.makeDevice(appKey, appPackage);
	}

	@Override
	public void callback(Device t) {
		// 服务器连接成功后发送设备注册信息给服务器
		// 发送设备注册消息
		if (deviceInfo != null) {
			// 如果设备已注册，立即上线
			if(TextUtils.isEmpty(deviceInfo.getRegId())) {
				applicationContextClient.sendRegistrationMessage(deviceInfo, new RegistrationResultListener(mContext, deviceInfo));
			} else {//if(deviceInfo.getIsOnline() == ApplicationContextClient.DEVICE_OFFLINE){
				applicationContextClient.sendDeviceOnlineMessage(deviceInfo, null);
			}
		}

	}
}
