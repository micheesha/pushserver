package com.netty.client.api.listener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.netty.client.consts.SystemConsts;

import apps.netty.server.protoc.DecPushProtoc;
import apps.netty.server.protoc.DecPushProtoc.PushMessage;

/**
 * 默认消息通知监听实现
 * 
 * @类名称：DefaultNotificationListener
 * @类描述：
 * @创建人：mengxuanliang
 * @创建时间：2014-10-28 下午12:41:54
 * 
 */
public class DefaultNotificationListener implements IConnectionListener {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);

	private NotificationManager notificationManager;

	private Context mContext;
	private int mIconId;
	private boolean mIsShowSysMsg = false ;

	public DefaultNotificationListener(Context context,boolean isShowSysMsg) {
		mContext = context;
		mIconId = context.getApplicationInfo().icon;
		mIsShowSysMsg = isShowSysMsg ;
	}

	@Override
	public void receive(PushMessage message) {
		if (SystemConsts.isDebug)
			Log.i(getClass().getName(), "receive:mContext:"+mContext.getClass().getName()+"-mIconId:"+mIconId);
		 
			//系统消息并且需要展示的进行消息提醒 |用户消息必须提醒
			notification(mContext, mIconId, message);
		 
		

	}

	/**
	 * 消息通知
	 * 
	 * @param icon 图标
	 * @param context 上下文
	 * @param message 消息
	 */
	@SuppressWarnings("deprecation")
	private void notification(Context context, int icon, DecPushProtoc.PushMessage message) {
		if (notificationManager == null) {
			notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				NotificationChannel channel = new NotificationChannel("netty", "-Netty", NotificationManager.IMPORTANCE_LOW);
				channel.enableVibration(false);
				notificationManager.createNotificationChannel(channel);
			}
		}
		//String idx = message.getMsgId();
		int idx = 0;
		String title = message.getTitle();
		String content = message.getContent().toStringUtf8();
		PackageManager packageManager = context.getPackageManager();
		Intent appIntent = new Intent();
		appIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
		appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		// appIntent.setPackage(message.getAppPackage());
		// appIntent.set
		appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, idx, appIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification.Builder notification = null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			notification = new Notification.Builder(context, "netty");
		} else {
			notification = new Notification.Builder(context);
		}
		notification.setWhen(System.currentTimeMillis());
		notification.setDefaults(Notification.DEFAULT_SOUND);
		// 使用系统默认样式
		notification.setSmallIcon(icon);
		notification.setTicker(title);
		notification.setAutoCancel(true);
		notification.setContentTitle(title);
		notification.setContentText(content);
		notification.setContentIntent(pendingIntent);
		notificationManager.notify(idx, notification.build());
	}
}
