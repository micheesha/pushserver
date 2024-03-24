package com.netty.client.android.handler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.netty.client.android.broadcast.ClientBroadcastReceiver;
import com.netty.client.android.listener.INettyHandlerListener;
import com.netty.client.android.service.RemoteService;
import com.netty.client.api.ConnectionManager;
import com.netty.client.consts.SystemConsts;
import com.netty.client.context.ApplicationContextClient;
import apps.netty.server.protoc.DecPushProtoc;

public class NettyProcessorHandler extends Handler {

	public static final int REGISTRATION_RESULT = 100;
	public static final int DEVICE_ONLINE_RESULT = 101;
	public static final int DEVICE_OFFLINE_RESULT = 102;
	public static final int DEVICE_BINDING_RESULT = 104;
	public static final int MESSAGE = 103;
	private Context context;
	private ApplicationContextClient applicationContextClient;

	public NettyProcessorHandler(Context context) {
		this.context = context;
		this.applicationContextClient = RemoteService.getApplicationContextClient();
	}

	/**
	 * 消息处理方法
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void handleMessage(Message msg) {
		if (msg != null) {
			if (SystemConsts.isDebug)
				Log.i(getClass().getName(), "-handleMessage:msg.what=" + msg.what);
			MessageObject mo = (MessageObject) msg.obj;
			if (mo == null) {
				return;
			}
			INettyHandlerListener listener = applicationContextClient.getNettyHandlerListener(mo.getAppPackage(), msg.what);
			if (listener != null) {
				if (SystemConsts.isDebug)
					Log.i(getClass().getName(), "-handleMessage:listener.callback=" + listener.toString());
				listener.callback(mo.getObj());
				return;
			}

			switch (msg.what) {
                /*case REGISTRATION_RESULT:
                    handleRegistResult((DecPushProtoc.ResultCode) mo.getObj());
                    break;*/
                case DEVICE_ONLINE_RESULT:
                    // 上线结果消息
                    handleOnlineResult((DecPushProtoc.ResultCode) mo.getObj());
                    break;
                case DEVICE_OFFLINE_RESULT:
                    // 下线结果消息
                    handleOfflineResult((DecPushProtoc.ResultCode) mo.getObj());
                    break;
                case MESSAGE:
                    // 消息
                    handlePushMessage((DecPushProtoc.PushMessage) mo.getObj());
                    break;
				case DEVICE_BINDING_RESULT:
					handleBindingMessage((DecPushProtoc.ResultCode) mo.getObj());
					break;
                default:
                    break;
			}
		}
	}

	/*protected void handleRegistResult(DecPushProtoc.ResultCode resultCode) {
		if (context != null && SystemConsts.isDebug) {
			switch (resultCode) {
				case SUCCESS:
					Toast.makeText(context, "设备注册成功", Toast.LENGTH_LONG).show();
					break;
				case FAILED:
					Toast.makeText(context, "设备注册失败", Toast.LENGTH_LONG).show();
					break;
				default:
					break;
			}
		}
	}*/

	/**
	 * 上线消息结果处理
	 * 
	 * @param resultCode 消息内容
	 */
	protected void handleOnlineResult(DecPushProtoc.ResultCode resultCode) {
		ConnectionManager.notificationOnlineListener(resultCode);
		if (context != null && SystemConsts.isDebug) {
			switch (resultCode) {
			case SUCCESS:
				Toast.makeText(context, "设备上线成功", Toast.LENGTH_LONG).show();
				break;
			case FAILED:
				Toast.makeText(context, "设备上线失败", Toast.LENGTH_LONG).show();
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 下线消息结果处理
	 * 
	 * @param resultCode 消息内容
	 */
	protected void handleOfflineResult(DecPushProtoc.ResultCode resultCode) {
		if (context != null && SystemConsts.isDebug) {
			switch (resultCode) {
			case SUCCESS:
				Toast.makeText(context, "设备下线成功", Toast.LENGTH_LONG).show();
				break;
			case FAILED:
				Toast.makeText(context, "设备下线失败", Toast.LENGTH_LONG).show();
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 绑定别名的消息处理
	 *
	 * @param resultCode 消息内容
	 */
	protected void handleBindingMessage(DecPushProtoc.ResultCode resultCode) {
		ConnectionManager.notificationBindingListener(resultCode);
		if (context != null && SystemConsts.isDebug) {
			switch (resultCode) {
				case SUCCESS:
					Toast.makeText(context, "绑定别名成功", Toast.LENGTH_LONG).show();
					break;
				case FAILED:
					Toast.makeText(context, "绑定别名失败", Toast.LENGTH_LONG).show();
					break;
				default:
					break;
			}
		}
	}

	/**
	 * PUSH消息处理
	 * 
	 * @param message 消息内容
	 */
	protected void handlePushMessage(DecPushProtoc.PushMessage message) {
		if (message != null) {
			ConnectionManager.notificationMessageListeners(message);
			// 发送广播
			/*Intent intent = new Intent();
			intent.setAction(ClientBroadcastReceiver.NOTIFICATION_ACTION);
			intent.putExtra(SystemConsts.NOTIFICATION_MESSAGE, message);
			context.sendBroadcast(intent);*/
			// 获取app icon id
			// int icon = context.getApplicationInfo().icon;
			// 系统通知消息
			// notification(icon, message);
		}
	}

}
