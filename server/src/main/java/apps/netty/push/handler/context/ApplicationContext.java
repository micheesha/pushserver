package apps.netty.push.handler.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import apps.netty.push.constants.PushConstants;
import apps.netty.push.pojo.AppInfo;
import apps.netty.push.pojo.ChannelInfo;
import apps.netty.push.pojo.DeviceInfo;
import apps.netty.push.pojo.MessageInfo;
import apps.netty.push.pojo.PushMessageInfo;
import apps.netty.push.pojo.PushWebsocketMessage;
import apps.netty.push.pojo.WebsocketInfo;
import apps.netty.push.pojo.WebsocketMessage;
import apps.netty.push.service.PushService;
import apps.netty.push.utils.HttpUtils;
import apps.netty.push.utils.Md5Util;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 应用上下文环境
 * 
 * @author mengxuan
 * 
 */
@Service("applicationContext")
@Scope("singleton")
public class ApplicationContext {
	
	private static Logger logger = LoggerFactory.getLogger(ApplicationContext.class);
	
	// ChannelGroup用于保存所有连接的客户端，注意要用static来保证只有一个ChannelGroup实例，否则每new一个TcpServerHandler都会创建一个ChannelGroup
	private static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	//deviceId mapping Channel map
	protected static ConcurrentHashMap<String ,ChannelInfo> nettyChannelMap = new ConcurrentHashMap<String,ChannelInfo>(); // deviceId - mqChannel 登录
	
	//deviceId mapping Channel map
	protected static ConcurrentHashMap<String ,Channel> nettyWebsocketChannelMap = new ConcurrentHashMap<String,Channel>(); // deviceId - mqChannel 登录
		
	
	private static ApplicationContext applicationContext;
	
	//本服务器的ip和端口
	private String tcpServerAddress;
	private String websocketServerAddress;
	
	@Resource
	private PushService pushService;

	@Value("${dec.order.status.query.url}")
	private String decOrderStatusQueryUrl;
	
	public static ApplicationContext getInstance() {
		return applicationContext;
	}

	/**
	 * 初始化执行方法
	 */
	@PostConstruct
	public void init() {
		logger.info(this.getClass().getName() + " INIT ...");
		
		applicationContext = this;
		logger.info(this.getClass().getName() + " INIT SUCCESS!!!\n");
	}

	/**
	 * 刷新心跳
	 */
	public void refreshHeart(String deviceId,Channel channel) {
		if(StringUtils.isBlank(deviceId)){
			return;
		}

		ChannelInfo channelInfo = nettyChannelMap.get(deviceId);
		
		if(null == channelInfo){
			return;
		}
		channelInfo.setHeartTime(System.currentTimeMillis());
	}

	/**
	 * 设备 心跳监控 超时的需要下线处理
	 */
	public void deviceMonitors(Long timeout) {
		
		if (nettyChannelMap != null && !nettyChannelMap.isEmpty()) {
			List<DeviceInfo> deviceListNew = new ArrayList<DeviceInfo>();
			Iterator<Map.Entry<String, ChannelInfo>> iterator = nettyChannelMap.entrySet().iterator();

			StringBuffer sb = new StringBuffer();
			while (iterator.hasNext()) {
				Map.Entry<String, ChannelInfo> entry = iterator.next();
				ChannelInfo channelInfo = entry.getValue();
				if (channelInfo != null) {
					Long heartTime = channelInfo.getHeartTime() == null ? 0 : channelInfo.getHeartTime();
					
					logger.info("device:"+entry.getKey()+" heartTime:"+heartTime);
					
					Long cha = System.currentTimeMillis() - heartTime;
					
					logger.info("timeout is:"+timeout);
					logger.info("cha is:"+cha);
					
					if (cha >= timeout) {
						Channel channel = channelInfo.getChannel();
						
						// 关闭渠道信息
						if (channel != null) {
							channel.close().addListener(ChannelFutureListener.CLOSE);
						}
						
						DeviceInfo deviceInfo = new DeviceInfo();
						deviceInfo.setIsOnline(PushConstants.BASE_STATUS.NO);
						deviceInfo.setOfflineTime(new Date());
						deviceListNew.add(deviceInfo);
						deviceInfo.setDeviceId(entry.getKey());

						sb.append("DEVICE STATUS:" + (deviceInfo.getDeviceId() + "-" + deviceInfo.getImei()) + "-"
								+ (deviceInfo.getIsOnline() == PushConstants.BASE_STATUS.YES ? "ONLINE" : "OFFLINE"));
						
						sb.append("-TIMEOUT OFFLINE!\n");
					}
				}
			}
			pushService.updateDeviceListToOffline(deviceListNew);
			if (sb.length() > 0) {
				logger.info("device status:"+sb.toString());
				sb.delete(0, sb.length() - 1);
			}
			sb = null;
			deviceListNew.clear();
			deviceListNew = null;
		}
		
	}
	
	/**
	 * 注册设备信息,并上线
	 * 
	 * @param deviceInfo
	 */
	public Map<String,Object> registDeviceNew(Channel channel, DecPushProtoc.DeviceRegistration registration) {
		
		Map<String,Object> returnMap = new HashMap<>();
		
		if(null == registration){
			returnMap.put("errorMessage", "registration info is empty");
			returnMap.put("errorCode", "error");
			return returnMap;
		}
		
		String regId = "";
		 
		//security validate 
		String appKey= registration.getAppKey();
		String appPackage = registration.getAppPackage();
		
		if(StringUtils.isBlank(appKey) || StringUtils.isBlank(appPackage)) {
			returnMap.put("errorMessage", "registration is empty");
			returnMap.put("errorCode", "error");
			return returnMap;
		}
		
		AppInfo appInfo = pushService.getAppInfo(appKey,appPackage);
		
		if(null == appInfo) {
			returnMap.put("errorMessage", "can not find app info");
			returnMap.put("errorCode", "error");
			return returnMap;
		}
		
		//String alias =  channel.attr(aliasKey).get();
		String deviceId =  registration.getDeviceId();
		DeviceInfo deviceInfo = pushService.queryDeviceByDeviceId(registration.getDeviceId());
		if(null == deviceInfo) {
			deviceInfo = new DeviceInfo();
			// 重新创建一个ID
			regId = Md5Util.toMD5(UUID.randomUUID().toString());
			deviceInfo.setRegId(regId);
			deviceInfo.setAppKey(registration.getAppKey());
			deviceInfo.setAppPackage(registration.getAppPackage());
			deviceInfo.setChannel(registration.getDeviceType());
			deviceInfo.setImei(registration.getImei());
			deviceInfo.setDeviceId(registration.getDeviceId());
			deviceInfo.setIsOnline(PushConstants.BASE_STATUS.YES);//注册后就上线
			deviceInfo.setOnlineTime(new Date());
			deviceInfo.setAlias(registration.getAlias());
			deviceInfo.setTag(registration.getTag());
			deviceInfo.setStatus(PushConstants.BASE_STATUS.ACTIVE);
			
			// 更新到数据库
			pushService.saveOrUpdateDeviceInfo(deviceInfo);
			 
		}else {
			regId = deviceInfo.getRegId();
		}
		
		ChannelInfo channelInfo = new ChannelInfo();
		channelInfo.setChannel(channel);
		channelInfo.setHeartTime(System.currentTimeMillis());
		
		//为了安全验证
		channel.attr(PushConstants.KEY_REGID).set(regId);
		channel.attr(PushConstants.KEY_DEVICEID).set(deviceId);
		
		
		nettyChannelMap.put(deviceId, channelInfo);
		returnMap.put("regId", regId);
		 
		returnMap.put("success", "success");
		return returnMap;
	}
	
	
	/**
	 * 添加渠道到渠道组里面 用于群发消息
	 * 
	 * @param channel
	 * @return
	 */
	public boolean addChannel(Channel channel) {
		if (channel != null) {
			boolean result =  channels.add(channel);
			logger.info("addChannel result:"+result);
			logger.info("addChannel left size:"+channels.size());
			
			return result;
		}
		return false;
	}
	
	
	/**
	 * 添加渠道到渠道组里面 用于群发消息
	 * 
	 * @param channel
	 * @return
	 */
	public boolean removeChannel(Channel channel) {
		if (channel != null) {
			boolean result =  channels.remove(channel);
			
			logger.info("removeChannel result:"+result);
			logger.info("removeChannel left size:"+channels.size());
			
			String deviceId = channel.attr(PushConstants.KEY_DEVICEID).get();
			
			if(StringUtils.isNotBlank(deviceId)){
				nettyChannelMap.remove(deviceId);
				logger.info("remove nettyChannelMap left size:"+nettyChannelMap.size());
			}
			return result;
		}
		return false;
	}
	
	public void addWebsocketChannel(Channel channel,String uid) {
		
		channel.attr(PushConstants.KEY_UID).set(uid);
		
		nettyWebsocketChannelMap.put(uid, channel);
		
		WebsocketInfo websocketInfo = new WebsocketInfo();
		websocketInfo.set_id(uid);
		websocketInfo.setUid(uid);
		websocketInfo.setIsOnline(PushConstants.BASE_STATUS.YES);
		websocketInfo.setOnlineTime(new Date());
		websocketInfo.setWebsocketServerAddress(this.getWebsocketServerAddress());
		
		pushService.saveOrUpdateWebsocketInfo(websocketInfo);
	}
	
	public void removeWebsocketChannel(Channel channel) {
		
		String uid = channel.attr(PushConstants.KEY_UID).get();
		nettyWebsocketChannelMap.remove(uid);
		
		WebsocketInfo websocketInfo = new WebsocketInfo();
		websocketInfo.set_id(uid);
		websocketInfo.setUid(uid);
		websocketInfo.setIsOnline(PushConstants.BASE_STATUS.NO);
		websocketInfo.setOfflineTime(new Date());
		
		pushService.saveOrUpdateWebsocketInfo(websocketInfo);
	}

	public boolean validateToken(String token,String type) {
		return pushService.validateToken(token,type);
	}
	
	public Map<String,Object> sendWebsocketMessageToUserList(WebsocketMessage messageInfo) {
		
		Map<String,Object> returnMap = new HashMap<>();
		
		List<String> uidList = new ArrayList<>();
		if(!CollectionUtils.isEmpty(messageInfo.getUidList())) {
			uidList = messageInfo.getUidList();
		} else if(!StringUtils.isBlank(messageInfo.getUid())){
			uidList.add(messageInfo.getUid());
		}
		
		if(uidList.isEmpty()) {
			returnMap.put("errorMessage", "uid or uidlist list is empty");
			return returnMap;
		}
		
		List<PushWebsocketMessage> pushWebsocketMessageList = new ArrayList<>();
		for(String uid : uidList) {
			PushWebsocketMessage pushWebsocketMessage = new PushWebsocketMessage();
			pushWebsocketMessage.setFrom(messageInfo.getFrom());
			pushWebsocketMessage.setNeedOfflineSent(messageInfo.isNeedOfflineSent());
			pushWebsocketMessage.setStatus(PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH);
			pushWebsocketMessage.setText(messageInfo.getText());
			pushWebsocketMessage.setTitle(messageInfo.getTitle());
			pushWebsocketMessage.setUid(uid);
			pushWebsocketMessageList.add(pushWebsocketMessage);
		}
		
		pushService.savePushWebsocketMessageInfo(pushWebsocketMessageList);
			
		List<DBObject> returnMessage = new ArrayList<>();
		
		for(PushWebsocketMessage pushWebsocketMessage : pushWebsocketMessageList) {
			Channel channel = nettyWebsocketChannelMap.get(pushWebsocketMessage.getUid());
			if(channel != null && channel.isWritable()) {
				TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(pushWebsocketMessage.getText());
				channel.writeAndFlush(textWebSocketFrame).addListener(
			            new ChannelFutureListener() {
			                @Override
			                public void operationComplete(ChannelFuture future)
			                   throws Exception {
			                    if (!future.isSuccess()) {
			                        future.cause().printStackTrace();;
			                        logger.error("exception",future.cause());
			                        
			                   }else {
			                	   pushService.updatePushWebsocketMessageStatus(pushWebsocketMessage.get_id(),PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS);
			                	   logger.info("push success");
			                   }
			                }
	            }); 
				
				DBObject jsonObject = new BasicDBObject();
			    jsonObject.put("message", "sucess");
				jsonObject.put("success", "SUCCESS");
				jsonObject.put("uid", pushWebsocketMessage.getUid());
				returnMessage.add(jsonObject);
			}else {
				DBObject jsonObject = new BasicDBObject();
			    jsonObject.put("errorMessage", "user not connect to server");
				jsonObject.put("errorCode", "error");
				jsonObject.put("uid", pushWebsocketMessage.getUid());
				returnMessage.add(jsonObject);
			}
		}
		returnMap.put("pushResult", returnMessage);
		return returnMap;
	}
	
	
	/**
	 * 关闭所有channel
	 * 
	 * @return
	 */
	public ChannelGroupFuture closeAllChannels() {
		if (channels != null && channels.size() > 0) {
			return channels.close();
		}
		return null;
	}

	/**
	 * 设备上线
	 * 
	 * @param deviceId
	 * @return
	 */
	public Map<String,String> online(Channel channel,String deviceId) {
		
		Map<String,String> returnMap = new HashMap<>();
		
		if(StringUtils.isNotBlank(deviceId)) {
			
			DeviceInfo deviceInfo = new DeviceInfo();
			
			deviceInfo.setIsOnline(PushConstants.BASE_STATUS.YES);
			deviceInfo.setOnlineTime(new Date());
			deviceInfo.setTcpServerAddress(this.tcpServerAddress);
			deviceInfo.setDeviceId(deviceId);
			
			logger.info(" device online deviceInfo:"+deviceInfo);
			
			boolean result = pushService.updateDeviceToOnline(deviceInfo);
			
			if(result) {
				ChannelInfo channelInfo = new ChannelInfo();
				channelInfo.setChannel(channel);
				channelInfo.setHeartTime(System.currentTimeMillis());
				
				//为了安全验证,暂时传递deviceId,后面再验证deviceId 拿到regId//TODO
				channel.attr(PushConstants.KEY_REGID).set(deviceId);
				channel.attr(PushConstants.KEY_DEVICEID).set(deviceId);
				
				nettyChannelMap.put(deviceId, channelInfo);
			}else {
				returnMap.put("error", "error") ;
				returnMap.put("errorMessage", "deviceId is not find.") ;
			}
		}else {
			returnMap.put("error", "error") ;
			returnMap.put("errorMessage", "deviceId is empty,online failed.") ;
		}
		
		return returnMap;
	}
	

	/**
	 * 设备下线
	 * 
	 * @param deviceId
	 * @return
	 */
	public void offline(String deviceId) {
		nettyChannelMap.remove(deviceId);
		
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.setDeviceId(deviceId);
		deviceInfo.setIsOnline(PushConstants.BASE_STATUS.NO);
		
		this.pushService.updateDeviceToOffline(deviceInfo);
		
	}
	
	public Map<String,Object> sendMessageByAlias(MessageInfo messageInfo) throws Exception {
		
		Map<String,Object> returnMap = new HashMap<String,Object>();
		
		List<String> aliasList = new ArrayList<>();
		if(!CollectionUtils.isEmpty(messageInfo.getAliasList())) {
			aliasList = messageInfo.getAliasList();
		} else if(null != messageInfo.getAlias() && !"".equals(messageInfo.getAlias())){
			aliasList.add(messageInfo.getAlias());
		}
		
		if(aliasList.isEmpty()) {
			returnMap.put("errorMessage", "alias or alias list is empty");
			return returnMap;
		}
		
		List<PushMessageInfo> pusgMessageInfoList = new ArrayList<>();
		for(String alias : aliasList) {
			List<DeviceInfo> deviceList = pushService.listDeviceByAlias(alias);
			
			logger.info("listDeviceByAlias alias:"+alias+" deviceList size:"+deviceList.size());
			
			if(null != deviceList && !deviceList.isEmpty()){
				Date nowDate = new Date();
				for(DeviceInfo deviceInfo : deviceList) {
					PushMessageInfo pushMessageInfo = new PushMessageInfo();
					pushMessageInfo.setTitle(messageInfo.getTitle());
					pushMessageInfo.setContent(messageInfo.getContent());
					pushMessageInfo.setMessageType(messageInfo.getMessageType());
					pushMessageInfo.setStatus(PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH);
					pushMessageInfo.setAlias(alias);
					pushMessageInfo.setDeviceId(deviceInfo.getDeviceId());
					pushMessageInfo.setNeedReceipt(messageInfo.isNeedReceipt());
					pushMessageInfo.set_id(messageInfo.get_id()+"_"+alias);
					pushMessageInfo.setCreatedDate(nowDate);
					pusgMessageInfoList.add(pushMessageInfo);
				}
			}
		}
		
		this.pushService.savePushMessageInfo(pusgMessageInfoList);
		
		List<DBObject> returnMessage = new ArrayList<>();
		
		for(PushMessageInfo pushMessageInfo : pusgMessageInfoList) {
			
			String deviceId = pushMessageInfo.getDeviceId();
			
			Channel channel = null;
			
			ChannelInfo channelInfo = nettyChannelMap.get(deviceId);
			
			if(null == channelInfo){
				 DBObject jsonObject = new BasicDBObject();
				    jsonObject.put("errorMessage", "device is not connected to server");
					jsonObject.put("errorCode", 1);
					jsonObject.put("alias", pushMessageInfo.getAlias());
					returnMessage.add(jsonObject);
					continue;
			}
			
			channel = channelInfo.getChannel();
			
			if(!channel.isWritable()) {
				    DBObject jsonObject = new BasicDBObject();
				    jsonObject.put("errorMessage", "device is disconnected");
					jsonObject.put("errorCode", 1);
					jsonObject.put("alias", pushMessageInfo.getAlias());
					returnMessage.add(jsonObject);
					continue;
			}
			 	
		    DecPushProtoc.PushMessage.Builder builder = DecPushProtoc.PushMessage.newBuilder();
			builder.setContent(ByteString.copyFrom(messageInfo.getContent().getBytes("UTF-8")));
			builder.setTitle(messageInfo.getTitle());
			builder.setIsNeedReceipt(messageInfo.isNeedReceipt());
			builder.setMessageType(pushMessageInfo.getMessageType());
			builder.setMsgId(pushMessageInfo.get_id());
			
			DecPushProtoc.PushMessage pushMessage2 = builder.build();
		 
			DecPushProtoc.PushPojo.Builder pushPojoBuilder = DecPushProtoc.PushPojo.newBuilder();
			pushPojoBuilder.setPushMessage(pushMessage2);
			pushPojoBuilder.setType(DecPushProtoc.Type.PUSH_MESSAGE);
			DecPushProtoc.PushPojo pushPojo  = pushPojoBuilder.build();
		 
			
			logger.info("sendMessageByAlias message:"+pushPojo);
			
			channel.writeAndFlush(pushPojo).addListener(
		            new ChannelFutureListener() {
		                @Override
		                public void operationComplete(ChannelFuture future)
		                   throws Exception {
		                    if (!future.isSuccess()) {
		                        future.cause().printStackTrace();;
		                        logger.error("exception",future.cause());
		                        
		                   }else {
		                	   pushService.updatePushMessageStatus(pushMessageInfo.get_id(),PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS);
		                	   logger.info("push success");
		                   }
		                }
            }); 
			
			DBObject jsonObject = new BasicDBObject();
		    jsonObject.put("message", "sucess");
			jsonObject.put("success", "SUCCESS");
			jsonObject.put("alias", pushMessageInfo.getAlias());
			returnMessage.add(jsonObject);
		}
		returnMap.put("pushResult", returnMessage);
		
		return returnMap;
	}
	
	
	public Map<String,Object> rollbackMessage(MessageInfo messageInfo) {
		Map<String,Object> returnMap = new HashMap<String,Object>();
		
		if(StringUtils.isBlank(messageInfo.get_id())) {
			returnMap.put("errorMessage", "message id is empty");
			return returnMap;
		}
		
		long rollbackMessageCount = this.pushService.rollbackMessageInfo(messageInfo.get_id());
		
		DBObject jsonObject = new BasicDBObject();
	    jsonObject.put("message", "sucess");
		jsonObject.put("success", "SUCCESS");
		jsonObject.put("message", "rollbacked "+rollbackMessageCount+" record message");
		
		returnMap.put("rollbackResult", jsonObject);
		
		return returnMap;
	}
	
	/**
	 * 绑定别名
	 * 
	 * @param deviceId
	 * @return
	 */
	public Map<String,String> bindingDevice(String registrationId,String alias) {
		Map<String,String> returnMap = new HashMap<>();
		if(StringUtils.isBlank(registrationId)){
			returnMap.put("errorCode", "error");
			returnMap.put("errorMessage", "registrationId is empty");
			return returnMap;
		}
		if(StringUtils.isBlank(alias)){
			returnMap.put("errorCode", "error");
			returnMap.put("errorMessage", "alias is empty");
			return returnMap;
		}
		boolean bindingResult = pushService.bindingAliasToDevice(registrationId,alias);
		if(!bindingResult) {
			returnMap.put("errorCode", "error");
			returnMap.put("errorMessage", "registrationId is not valid");
			return returnMap;
		}
		
		returnMap.put("sucess", "SUCCESS");
		return returnMap;
	}
	
	public void sendOfflineMessageToDevice(String deviceId) throws Exception {
		// 如果设备上线成功 则查询设备是否存在离线消息 有则返回离线消息列表
		List<PushMessageInfo> pusgMessageInfoList = pushService.listPushMessageByDevice(deviceId);
		
		if(pusgMessageInfoList != null && !pusgMessageInfoList.isEmpty()) {
			//只发一条
			PushMessageInfo pushMessageInfo = pusgMessageInfoList.get(0);
			//回调唐人街，检查这一条消息的订单状态 是否被处理了
			String[] strArr = pushMessageInfo.get_id().split("_");
			
			decOrderStatusQueryUrl = decOrderStatusQueryUrl.replace("{orderTransactionId}", strArr[1]).replace("{messageType}", strArr[0]);
			
			boolean canPush =true;
			
			JsonObject result = HttpUtils.httpGetRequest(decOrderStatusQueryUrl);
			
			logger.info("dec order status query result:"+result);
			
			if(result != null && 
					result.get("data") != null && 
					result.get("data").getAsJsonObject().get("canPush") != null &&
					!result.get("data").getAsJsonObject().get("canPush").getAsBoolean()
					 ) {
				canPush =false;
			}
			if(!canPush) {
				return;
			}
			
			Channel channel = null;
			ChannelInfo channelInfo = nettyChannelMap.get(deviceId);
			
			if(null == channelInfo){
					return;
			}
			channel = channelInfo.getChannel();
			if(!channel.isWritable()) {
					return;
			}
		    DecPushProtoc.PushMessage.Builder builder = DecPushProtoc.PushMessage.newBuilder();
			builder.setContent(ByteString.copyFrom(pushMessageInfo.getContent().getBytes("UTF-8")));
			builder.setTitle(pushMessageInfo.getTitle());
			builder.setIsNeedReceipt(pushMessageInfo.isNeedReceipt());
			builder.setMessageType(pushMessageInfo.getMessageType());
			builder.setMsgId(pushMessageInfo.get_id());
			
			DecPushProtoc.PushMessage pushMessage2 = builder.build();
		 
			DecPushProtoc.PushPojo.Builder pushPojoBuilder = DecPushProtoc.PushPojo.newBuilder();
			pushPojoBuilder.setPushMessage(pushMessage2);
			pushPojoBuilder.setType(DecPushProtoc.Type.PUSH_MESSAGE);
			DecPushProtoc.PushPojo pushPojo  = pushPojoBuilder.build();
		 
			channel.writeAndFlush(pushPojo).addListener(
		            new ChannelFutureListener() {
		                @Override
		                public void operationComplete(ChannelFuture future)
		                   throws Exception {
		                    if (!future.isSuccess()) {
		                        future.cause().printStackTrace();;
		                        logger.error("exception",future.cause());
		                        
		                   }else {
		                	   pushService.updatePushMessageStatus(pushMessageInfo.get_id(),PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS);
		                	   logger.info("push success");
		                   }
		                }
            }); 
			 
		}
		
	}
	
	
	public void sendOfflineMessageToWebsocketUser(String uid) throws Exception {
		// 如果设备上线成功 则查询设备是否存在离线消息 有则返回离线消息列表
		List<PushWebsocketMessage> pusgMessageInfoList = pushService.listPushWebsocketMessageByUid(uid);
		
		if(pusgMessageInfoList != null && !pusgMessageInfoList.isEmpty()) {
			for(PushWebsocketMessage pushWebsocketMessage : pusgMessageInfoList) {
				Channel channel = nettyWebsocketChannelMap.get(uid);
				
				if(null == channel){
						continue;
				}
				if(!channel.isWritable()) {
						continue;
				}
				TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(pushWebsocketMessage.getText());
			 
				channel.writeAndFlush(textWebSocketFrame).addListener(
			            new ChannelFutureListener() {
			                @Override
			                public void operationComplete(ChannelFuture future)
			                   throws Exception {
			                    if (!future.isSuccess()) {
			                        future.cause().printStackTrace();;
			                        logger.error("exception",future.cause());
			                        
			                   }else {
			                	   pushService.updatePushWebsocketMessageStatus(pushWebsocketMessage.get_id(),PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS);
			                	   logger.info("push success");
			                   }
			                }
	            }); 
			}
		}
		
	}
	
	public void updateMessageReceipt(String msgId) {
		pushService.updateMessageReceipt(msgId);
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

	 
	
	/**
	 * 创建RegistrationResult对象
	 * 
	 * @param type
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandRegistrationResultNew(String appKey, String appPackage, Map<String,Object> returnMap) {
		
		DecPushProtoc.DeviceRegistrationResult.Builder builder = DecPushProtoc.DeviceRegistrationResult.newBuilder();
		builder.setAppKey(appKey);
		builder.setAppPackage(appPackage);
		
		builder.setResultCode(DecPushProtoc.ResultCode.SUCCESS);
		
		if(returnMap.containsKey("errorCode")){
			builder.setErrorCode(returnMap.get("errorCode").toString());
		}
		if(returnMap.containsKey("errorMessage")){
			builder.setErrorMessage(returnMap.get("errorMessage").toString());
			builder.setResultCode(DecPushProtoc.ResultCode.FAILED);
		}
		if(returnMap.containsKey("regId")){
			builder.setRegistrationId(returnMap.get("regId").toString());
		}
		
		DecPushProtoc.DeviceRegistrationResult commandProtoc = builder.build();
		
		logger.info("commandProtoc:"+commandProtoc.toString());
		
		
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder pojoBuilder = DecPushProtoc.PushPojo.newBuilder();
		pojoBuilder.setDeviceRegistrationResult(commandProtoc);
		pojoBuilder.setType(DecPushProtoc.Type.DEVICE_REGISTRATION_RESULT);
		
		return pojoBuilder.build();
	}

	/**
	 * 创建DeviceOnoffResult对象
	 * 
	 * @param type
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandDeviceOnLineResult(String deviceId, DecPushProtoc.ResultCode resultCode,String errorMessage) {
		DecPushProtoc.DeviceOnLineResult.Builder builder = DecPushProtoc.DeviceOnLineResult.newBuilder();
		if (errorMessage != null) {
			builder.setErrorMessage(errorMessage);
		}
		builder.setResultCode(resultCode);
		builder.setDeviceId(deviceId);
		
		DecPushProtoc.DeviceOnLineResult commandProtoc = builder.build();
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_ONLINE_RESULT);
		messageBuilder.setDeviceOnLineResult(commandProtoc);
		return messageBuilder.build();
	}
	
	/**
	 * 创建DeviceOnoffResult对象
	 * 
	 * @param type
	 * @return
	 */
	public DecPushProtoc.PushPojo createCommandDeviceOffLineResult(String deviceId, DecPushProtoc.ResultCode resultCode,String errorMessage) {
		DecPushProtoc.DeviceOffLineResult.Builder builder = DecPushProtoc.DeviceOffLineResult.newBuilder();
		if (errorMessage != null) {
			builder.setErrorMessage(errorMessage);
		}
		builder.setResultCode(resultCode);
		builder.setDeviceId(deviceId);
		
		DecPushProtoc.DeviceOffLineResult commandProtoc = builder.build();
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_OFFLINE_RESULT);
		messageBuilder.setDeviceOffLineResult(commandProtoc);
		return messageBuilder.build();
	}
	
	/**
	 * 创建DeviceOnoffResult对象
	 * 
	 * @param type
	 * @return
	 */
	public DecPushProtoc.PushPojo createConnectionFailedResult(String errorMessage) {
		
		DecPushProtoc.ConnectionResult.Builder builder = DecPushProtoc.ConnectionResult.newBuilder();
		
		builder.setResultCode(DecPushProtoc.ResultCode.FAILED);
		builder.setErrorMessage(errorMessage);
		
		DecPushProtoc.ConnectionResult commandProtoc = builder.build();
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.CONNECTION_RESULT);
		messageBuilder.setConnectionResult(commandProtoc);
		return messageBuilder.build();
	}
	
	
	/**
	 * 创建result对象
	 * 
	 * @param type
	 * @return
	 */
	public DecPushProtoc.PushPojo createBindDeviceResultMessage(Map<String,String> returnMap,String deviceId,String registrationId,String alias) {
		
		DecPushProtoc.DeviceBindingResult.Builder builder = DecPushProtoc.DeviceBindingResult.newBuilder();
		builder.setResultCode(DecPushProtoc.ResultCode.SUCCESS);
		if (null != returnMap && returnMap.containsKey("errorMessage")) {
			builder.setErrorMessage(returnMap.get("errorMessage"));
			builder.setErrorCode(returnMap.get("errorCode"));
			builder.setResultCode(DecPushProtoc.ResultCode.FAILED);
		}
		 
		builder.setDeviceId(deviceId);
		builder.setRegistrationId(registrationId);
		builder.setAlias(alias);
		
		DecPushProtoc.DeviceBindingResult commandProtoc = builder.build();
		// 创建消息对象
		DecPushProtoc.PushPojo.Builder messageBuilder = this.createCommandPushMessage(DecPushProtoc.Type.DEVICE_BINDING_RESULT);
		messageBuilder.setDeviceBindingResult(commandProtoc);
		return messageBuilder.build();
	}
	
	public String getTcpServerAddress() {
		return tcpServerAddress;
	}

	public void setTcpServerAddress(String tcpServerAddress) {
		this.tcpServerAddress = tcpServerAddress;
	}
	
	public String getWebsocketServerAddress() {
		return websocketServerAddress;
	}

	public void setWebsocketServerAddress(String websocketServerAddress) {
		this.websocketServerAddress = websocketServerAddress;
	}

	/**
	 * 销毁
	 */
	public void destory() {
		channels.clear();
		nettyChannelMap.clear();
	}

}
