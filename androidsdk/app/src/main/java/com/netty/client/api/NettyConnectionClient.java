package com.netty.client.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.netty.client.android.broadcast.NettyAlarmManager;
import com.netty.client.android.service.RemoteService;
import com.netty.client.consts.SystemConsts;

/**
 * 连接客户端jar
 * 
 * @类名称：NettyConnectionClient
 * @类描述：
 * @创建人：mengxuanliang
 * @创建时间：2014-10-20 上午11:08:32
 * 
 */
public class NettyConnectionClient {

	private RemoteService nettyServiceClient = null;
	private Context mContext;
	private String mAppKey;

	private Intent aidlIntent = new Intent(RemoteService.class.getName());

	public NettyConnectionClient(Context context, String appKey) {
		this.mContext = context;
		this.mAppKey = appKey;
		// 启动service
		// if(!isMyServiceExisted()){
		// 如果service不存在 则启动

		// }
		aidlIntent.setClass(context, RemoteService.class);
		mContext.startService(aidlIntent);
		bindNettyService();
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			if (SystemConsts.isDebug)
				Log.i("NettyConnectionClient", "### aidl disconnected.");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			nettyServiceClient = ((RemoteService.NettyServiceClientImpl)service).getService();
			if (SystemConsts.isDebug) {
				Log.i("NettyConnectionClient", "### aidl onServiceConnected. service : " + service.getClass().getName());
				Log.i("NettyConnectionClient", "### after asInterface : " + nettyServiceClient.getClass().getName());
			}
			try {
				nettyServiceClient.connect(mAppKey, mContext.getPackageName());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	public void setHeartbeatPeriod(long milliseconds) {
		NettyAlarmManager.PERIOD = milliseconds;
	}

	public void setDebug(boolean debug) {
		SystemConsts.isDebug = debug;
	}

	public void setHostPost(String host, int port) {
		SystemConsts.HOST = host;
		SystemConsts.PORT = port;
	}

	/**
	 * 綁定service服务
	 * 
	 * @param
	 * @param
	 */
	private void bindNettyService() {
		// 服务端的action
		mContext.bindService(aidlIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindNettyService() {
		mContext.unbindService(mConnection);
	}

	/**
	 * 解除设备注册 （删除service中缓存的设备信息内容）
	 * 
	 * @param
	 */
	public void offline() {
		if (nettyServiceClient != null && mContext != null) {
			try {
				// 想service发送 设备下线消息
				nettyServiceClient.deviceOffline(mContext.getPackageName());
				unbindNettyService();
				mContext.stopService(aidlIntent);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void online() {
		if (nettyServiceClient != null && mContext != null) {
			try {
				nettyServiceClient.connect(mAppKey, mContext.getPackageName());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 *
	 * @param alias 别名
	 */
	public void bindingAliasToDevice(String alias) {
		if (nettyServiceClient != null && mContext != null) {
			try {
				nettyServiceClient.bindingAliasToDevice(mContext.getPackageName(), alias);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	public void reRegister() {
		if (nettyServiceClient != null && mContext != null) {
			try {
				nettyServiceClient.deleteDevice(mContext.getPackageName());
				online();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
