package apps.netty.push.service;

import java.util.List;

import apps.netty.push.pojo.AppInfo;
import apps.netty.push.pojo.DeviceInfo;
import apps.netty.push.pojo.PushMessageInfo;
import apps.netty.push.pojo.PushWebsocketMessage;
import apps.netty.push.pojo.WebsocketInfo;

public interface PushService {

	/**
	 * 保存消息到数据库
	 * 
	 * @return
	 */
	public void savePushMessageInfo(List<PushMessageInfo> pushMessageInfoList);

	public void updatePushMessageStatus(String id,String status);
	
	public void updatePushWebsocketMessageStatus(String id,String status);
	
	
	
	/**
	 * 获取device离线push消息列表
	 * 
	 * @return
	 */
	public List<PushMessageInfo> listPushMessageByDevice(String deviceId);
	
	
	/**
	 * 获取uid离线push消息列表,for websocket 
	 * 
	 * @return
	 */
	public List<PushWebsocketMessage> listPushWebsocketMessageByUid(String deviceId);
	
 
	/**
	 * 查詢設備
	 * 
	 * @return
	 */
	public DeviceInfo queryDeviceByDeviceId(String deviceId);

	public List<DeviceInfo> listDeviceByAlias(String alias);
	
	/**
	 * 更新设备信息
	 * 
	 * @param deviceInfo
	 * @return
	 */
	public void saveOrUpdateDeviceInfo(DeviceInfo deviceInfo);

	/**
	 * 批量更新设备信息列表
	 * 
	 * @param deviceInfos
	 * @return
	 */
	public void updateDeviceListToOffline(List<DeviceInfo> deviceInfos);
	
	/**
	 * 批量更新设备信息列表
	 * 
	 * @param deviceInfos
	 * @return
	 */
	public void updateDeviceToOffline(DeviceInfo deviceInfo);
	
	public boolean updateDeviceToOnline(DeviceInfo deviceInfo);
	
	public void updateMessageReceipt(String msgId);
	
	public boolean bindingAliasToDevice(String registrationId,String alias);
	
	public AppInfo getAppInfo(String appKey,String appPackage);
	
	public boolean validateToken(String token,String type);
	
	public void saveOrUpdateWebsocketInfo(WebsocketInfo websocketInfo);
	public void updateWebsocketToOffline(WebsocketInfo websocketInfo);
	
	public void savePushWebsocketMessageInfo(List<PushWebsocketMessage> pushWebsocketMessageList);
	
	public long rollbackMessageInfo(String _id);
}
