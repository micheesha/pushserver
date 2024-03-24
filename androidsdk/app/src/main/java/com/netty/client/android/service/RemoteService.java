package com.netty.client.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.netty.client.android.NettyServerManager;
import com.netty.client.android.broadcast.AlarmReceiver;
import com.netty.client.android.broadcast.NettyAlarmManager;
import com.netty.client.android.dao.DaoMaster;
import com.netty.client.android.dao.DaoMaster.OpenHelper;
import com.netty.client.android.dao.DaoSession;
import com.netty.client.android.dao.Device;
import com.netty.client.android.handler.NettyProcessorHandler;
import com.netty.client.android.listener.ConnectionListener;
import com.netty.client.android.listener.INettyHandlerListener;
import com.netty.client.android.listener.RegistrationResultListener;
import com.netty.client.android.listener.ServerStatusListener;
import com.netty.client.consts.SystemConsts;
import com.netty.client.context.ApplicationContextClient;

import apps.netty.server.protoc.DecPushProtoc;

/**
 * 远程服务
 * 
 * @author mengxuanliang
 * 
 */
public class RemoteService extends Service {

	private static ApplicationContextClient applicationContextClient = null;// ApplicationContextClient.getInstance();

	private NettyServerManager mConnectionManager = null;
	private NettyProcessorHandler mNettyProcessorHandler = null;
	// 服務器连接成功后监听
	private ServerStatusListener mServerStatusListener = null;

	// 广播通知
	private AlarmReceiver alarmReceiver = null;

	/**
     * 
     */
	IBinder mBinder = new NettyServiceClientImpl();

	Thread thread = null;

	/**
	 * dao操作类
	 */
	private static DaoMaster daoMaster;

	private static DaoSession daoSession;

	private static Context mContext;

	@Override
	public void onCreate() {
		super.onCreate();
		// 初始化
		init();
		// 连接服务器
		connectServer();
		// 注册广播
		if (alarmReceiver == null) {
			alarmReceiver = new AlarmReceiver();
		}
		// 註冊廣播
		IntentFilter filter = new IntentFilter();
		// 心跳action
		filter.addAction(AlarmReceiver.ACTION);
		// 连接action
		filter.addAction(AlarmReceiver.CONNECT_ACTION);
		// 切换网络action
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(alarmReceiver, filter);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			connectivityManager.requestNetwork(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
				@Override
				public void onAvailable(Network network) {
					super.onAvailable(network);
					NetworkInfo gprs = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
					NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (!gprs.isConnected() && !wifi.isConnected()) {
						// 網絡斷開 了
						// 停止心跳消息发送
						NettyAlarmManager.stopHeart();
						// 停止重连
						NettyAlarmManager.stopReconnection();
					} else {
						// 启动重连
						NettyAlarmManager.startReconnection(getApplicationContext());
					}
				}
			});
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		mContext = this;
		if (mNettyProcessorHandler == null) {
			mNettyProcessorHandler = new NettyProcessorHandler(this);
		}
		if (mServerStatusListener == null) {
			mServerStatusListener = new ServerStatusListener();
		}

		if (applicationContextClient == null) {
			applicationContextClient = new ApplicationContextClient(mContext);
		}
	}

	@Override
	public void onDestroy() {
		if (SystemConsts.isDebug)
			Log.i(getClass().getName(), "onDestroy");
		if (applicationContextClient != null) {
			// 下线所有设备 并且设置数据库下线状态
			applicationContextClient.offlineAllDevices();
			applicationContextClient.destory();
			applicationContextClient = null;
		}
		if (mServerStatusListener != null) {
			mServerStatusListener.destory();
			mServerStatusListener = null;
		}
		mNettyProcessorHandler = null;
		daoSession.clear();
		daoMaster = null;
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
		if (alarmReceiver != null) {
			// 解除广播
			unregisterReceiver(alarmReceiver);
		}
		super.onDestroy();
	}

	/**
	 * 连接服务器
	 */
	private void connectServer() {
		if (mConnectionManager == null) {
			mConnectionManager = NettyServerManager.getInstance(RemoteService.this, mNettyProcessorHandler);
		}
		if (SystemConsts.isDebug) {
			Log.i(RemoteService.class.getName(), "connectServer connectState:" + mConnectionManager.getConnectState());
		}
		if (mConnectionManager != null && !mConnectionManager.isConnected()) {
			if (thread == null) {
				thread = new Thread() {
					public void run() {

						try {
							mConnectionManager.connect(SystemConsts.HOST, SystemConsts.PORT, mServerStatusListener);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
			}
			thread.start();
		}
	}

	/**
	 * @author mrsimple
	 */
	public class NettyServiceClientImpl extends Binder {
		public RemoteService getService() {
			return RemoteService.this;
		}
	}

	public void connect(String appKey, String appPackage) throws RemoteException {
		if (SystemConsts.isDebug)
			Log.i(RemoteService.class.getName(), "connect");
		init();
		if (mConnectionManager == null) {
			mConnectionManager = NettyServerManager.getInstance(RemoteService.this, mNettyProcessorHandler);
		}
		if (mConnectionManager != null) {
			if (SystemConsts.isDebug)
				Log.i(RemoteService.class.getName(), "regist connectState:" + mConnectionManager.getConnectState());
			// 服务连接成功 后监听
			ConnectionListener connectionListener = new ConnectionListener(RemoteService.this, appKey, appPackage);
			if (mConnectionManager.isConnecting()) {
				// 注册ConnectionListener 当服务器连接成功 后通知
				mServerStatusListener.registNettyHandlerListener(connectionListener);
			} else if (mConnectionManager.isConnected()) {
				// 如果已经连接成功了 则 直接执行ConnectionListener
				connectionListener.callback(null);
				connectionListener = null;
			} else {
				// 服务器尚未连接 则进行server连接工作
				try {
					// 如果服务器正在连接状态则 等待
					mConnectionManager.connect(SystemConsts.HOST, SystemConsts.PORT, connectionListener);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void deleteDevice(String appPackage) throws RemoteException {
		if (applicationContextClient == null) {
			applicationContextClient = new ApplicationContextClient(mContext);
		}
		Device deviceInfo = applicationContextClient.getDeviceInfoByAppPackage(appPackage);
		applicationContextClient.deleteDeviceInfo(deviceInfo);
	}

	public void deviceOffline(String appPackage) throws RemoteException {
		if (SystemConsts.isDebug)
			Log.i(RemoteService.class.getName(), "deviceOffline");
		Device deviceInfo = applicationContextClient.getDeviceInfoByAppPackage(appPackage);
		applicationContextClient.sendDeviceOfflineMessage(deviceInfo, new INettyHandlerListener<DecPushProtoc.ResultCode>() {
			@Override
			public void callback(DecPushProtoc.ResultCode deviceOnLineResult) {
				if (SystemConsts.isDebug)
					Log.i(RemoteService.class.getName(), "DeviceOffline:" + deviceOnLineResult);
				if (deviceOnLineResult == DecPushProtoc.ResultCode.SUCCESS) {
					applicationContextClient.destory();
					applicationContextClient = null;
				}
			}
		});
	}

	public void bindingAliasToDevice(String appPackage, String alias) throws RemoteException {
		if (SystemConsts.isDebug)
			Log.i(RemoteService.class.getName(), "bindingAliasToDevice");
		Device deviceInfo = applicationContextClient.getDeviceInfoByAppPackage(appPackage);
		applicationContextClient.sendBindingAliasToDeviceMessage(alias, deviceInfo.getRegId());
	}

	/*
	 * 返回Binder实例，即实现了ILogin接口的Stub的子类，这里为LoginStubImpl [url=home.php?mod=space&uid=133757]@see[/url] android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// 初始化
		init();
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	/**
	 * 获取上下文环境
	 * 
	 * @return
	 */
	public static final ApplicationContextClient getApplicationContextClient() {
		if (applicationContextClient == null) {
			applicationContextClient = new ApplicationContextClient(mContext);
		}
		return applicationContextClient;
	}

	/**
	 * 取得DaoMaster
	 * 
	 * @param context
	 * @return DaoMaster
	 */
	public static DaoMaster getDaoMaster(Context context) {
		if (daoMaster == null) {
			OpenHelper helper = new DaoMaster.DevOpenHelper(context, SystemConsts.DATABASE_NAME, null);
			daoMaster = new DaoMaster(helper.getWritableDatabase());
		}
		return daoMaster;
	}

	/**
	 * 取得DaoSession
	 * 
	 * @param context
	 * @return DaoSession
	 */
	public static DaoSession getDaoSession(Context context) {
		if (daoSession == null) {
			if (daoMaster == null) {
				daoMaster = getDaoMaster(context);
			}
			daoSession = daoMaster.newSession();
		}
		return daoSession;
	}

}
