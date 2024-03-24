package apps.netty.push.service;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import apps.netty.push.constants.PushConstants;
import apps.netty.push.pojo.AppInfo;
import apps.netty.push.pojo.DeviceInfo;
import apps.netty.push.pojo.PushMessageInfo;
import apps.netty.push.pojo.PushWebsocketMessage;
import apps.netty.push.pojo.WebsocketInfo;
import apps.netty.push.pojo.WebsocketToken;

@Service
public class PushServiceImpl implements PushService{

	private static Logger logger = LoggerFactory.getLogger(PushServiceImpl.class);
	
	@Resource
	MongoTemplate mongoTemplate;
	
	@Override
	public void savePushMessageInfo(List<PushMessageInfo> pushMessageInfoList) {
		 
		for(PushMessageInfo pushMessageInfo : pushMessageInfoList) {
			mongoTemplate.save(pushMessageInfo, "push_message_info");
		}
		
	}

	@Override
	public void updatePushMessageStatus(String id,String status) {
		
		Query query = new Query();
  	    query.addCriteria(Criteria.where("_id").is(id));
  	    Update update = Update.update("status", status);
  	    
  	    if(status.equals(PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS)){
  	    	update.set("pushTime", new Date());
  	    }
  	    
  	     mongoTemplate.updateFirst(query, update, "push_message_info");
	}
	
	@Override
	public void updatePushWebsocketMessageStatus(String id,String status) {
		
		Query query = new Query();
  	    query.addCriteria(Criteria.where("_id").is(id));
  	    Update update = Update.update("status", status);
  	    
  	    if(status.equals(PushConstants.MESSAGE_PUSHED_STATUS.PUSH_SUCCESS)){
  	    	update.set("pushTime", new Date());
  	    }
  	    
  	     mongoTemplate.updateFirst(query, update, "push_websocket_message_info");
	}
	
	
	@Override
	public List<PushMessageInfo> listPushMessageByDevice(String deviceId) {
		/*Query query = new Query();
  	    query.addCriteria(Criteria.where("status").is(PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH).and("deviceId").is(deviceId));
  	    */
		String querySql2 = "{'deviceId':'"+deviceId+"'}";
		 Query query2 = new  BasicQuery(querySql2);
	  	    query2.with(new Sort(new Order(Direction.DESC, "onlineTime")))
	  	    .limit(1);
		
  	     Document deviceInfo = mongoTemplate.findOne(query2, Document.class, "push_device_info");
		
  	     String alias = deviceInfo.getString("alias");
  	     
		String querySql  = "{'$and':[{'deviceId': '"+deviceId+"'},{'alias':'"+alias+"'},{'$or':[{'status':'"+PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH+"'},{'$and':[{'status':{'$ne':'"+PushConstants.MESSAGE_PUSHED_STATUS.RECEIPT_RECEIVED+"'}},{'status':{'$ne':'"+PushConstants.BASE_STATUS.INACTIVE+"'}},{'isNeedReceipt':true}]}]}]}";
		
  	    Query query = new  BasicQuery(querySql);
  	    query.with(new Sort(new Order(Direction.DESC, "createdDate")));
  	    query.limit(1);
  	    
  	    List<PushMessageInfo> message = mongoTemplate.find(query, PushMessageInfo.class, "push_message_info");
  	    
		//TODO清理其他信息
		String querySql3  = "{'$and':[{'deviceId': '"+deviceId+"'},{'alias':'"+alias+"'}]}";
		Query query3 = new  BasicQuery(querySql3);
		
		logger.info("clear other message querySql3:"+querySql3);
		
		Update update = new Update();
		update.set("status", PushConstants.BASE_STATUS.INACTIVE);
		mongoTemplate.updateMulti(query3, update, "push_message_info");
		
		return  message;
		
	}
	
	public List<PushWebsocketMessage> listPushWebsocketMessageByUid(String uid) {
		String querySql  = "{'$and':[{'uid': '"+uid+"'},{'status':'"+PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH+"'},{'isNeedOfflineSent':true}]}";
		
  	    Query query = new  BasicQuery(querySql);
  	    
		return  mongoTemplate.find(query, PushWebsocketMessage.class, "push_websocket_message_info");	
	}
	
	@Override
	public DeviceInfo queryDeviceByDeviceId(String deviceId) {
		// TODO Auto-generated method stub
		return mongoTemplate.findOne(new Query(Criteria.where("deviceId").is(deviceId)), DeviceInfo.class, "push_device_info");
	}
	
	/**
	 * 
	 * 
	 */
	@Override
	public List<DeviceInfo> listDeviceByAlias(String alias) {
		// TODO Auto-generated method stub
		return mongoTemplate.find(new Query(Criteria.where("alias").is(alias)), DeviceInfo.class, "push_device_info");
	}
	
	
	@Override
	public void saveOrUpdateDeviceInfo(DeviceInfo deviceInfo) {
		deviceInfo.set_id(deviceInfo.getDeviceId());
		
		this.mongoTemplate.save(deviceInfo, "push_device_info");
	}

	@Override
	public void updateDeviceListToOffline(List<DeviceInfo> deviceInfos) {
		for(DeviceInfo deviceInfo : deviceInfos) {
			
			Query query = new Query();
	  	    query.addCriteria(Criteria.where("deviceId").is(deviceInfo.getDeviceId()));
	  	    Update update = Update.update("isOnline", PushConstants.BASE_STATUS.NO).set("offlineTime", deviceInfo.getOfflineTime());
	  	    UpdateResult updateResult = mongoTemplate.updateFirst(query, update, "push_device_info");
		}
	}
	
	public void updateDeviceToOffline(DeviceInfo deviceInfo) {
		Query query = new Query();
  	    query.addCriteria(Criteria.where("deviceId").is(deviceInfo.getDeviceId()));
  	    Update update = Update.update("isOnline", PushConstants.BASE_STATUS.NO).set("offlineTime", deviceInfo.getOfflineTime());
  	    mongoTemplate.updateFirst(query, update, "push_device_info");
	}
	
	public boolean updateDeviceToOnline(DeviceInfo deviceInfo) {
		Query query = new Query();
  	    query.addCriteria(Criteria.where("deviceId").is(deviceInfo.getDeviceId()));
  	    Update update = Update.update("isOnline", PushConstants.BASE_STATUS.YES)
  	    		.set("onlineTime", deviceInfo.getOnlineTime())
  	    		.set("tcpServerAddress", deviceInfo.getTcpServerAddress())
  	    		.set("offlineTime", null);
  	    
  	    UpdateResult updateResult = mongoTemplate.updateFirst(query, update, "push_device_info");
  	    
  	    if(updateResult.getMatchedCount() >0){
  	    	return true;
  	    }else {
  	    	return false;
  	    }
	}
	
	
	public void updateMessageReceipt(String msgId){
		Query query = new Query();
  	    query.addCriteria(Criteria.where("_id").is(msgId));
  	    Update update = Update.update("status", PushConstants.MESSAGE_PUSHED_STATUS.RECEIPT_RECEIVED).set("receiptReceivedTime", new Date());
  	    mongoTemplate.updateFirst(query, update, "push_message_info");
	}
	
	public boolean bindingAliasToDevice(String registrationId,String alias) {
		
		 	Query query = new Query();
	  	    query.addCriteria(Criteria.where("regId").is(registrationId));
	  	    Update update = Update.update("alias", alias);
	  	  UpdateResult updateResult = mongoTemplate.updateFirst(query, update, "push_device_info");
		
	  	  if(null != updateResult && updateResult.getMatchedCount() >0 ){
	  		  //clear other device's alias that alias is this alias
	  		 /*query = new Query();
	  	     query.addCriteria(Criteria.where("alias").is(alias));
	  	     update = Update.update("alias", "");
	  	     mongoTemplate.updateFirst(query, update, "push_device_info");*/
	  		  return true;
	  	  }else {
	  		  return false;
	  	  }
	}
	
	
	public AppInfo getAppInfo(String appKey,String appPackage) {
		return mongoTemplate.findOne(new Query(Criteria.where("appKey").is(appKey).and("appPackage").is(appPackage).and("status").is(PushConstants.BASE_STATUS.ACTIVE)), AppInfo.class, "push_app_info");
	}
	
	public boolean validateToken(String token,String type) {
		WebsocketToken dbObject= mongoTemplate.findOne(new Query(Criteria.where("token").is(token).and("status").is(PushConstants.BASE_STATUS.ACTIVE).and("type").is(type)), WebsocketToken.class, "push_token");
		if(null != dbObject) {
			return true;
		}else {
			return false;
		}
	}
	
	public void saveOrUpdateWebsocketInfo(WebsocketInfo websocketInfo) {
		
		
		Query query = new Query();
  	    query.addCriteria(Criteria.where("_id").is(websocketInfo.get_id()));
  	    Update update = Update.update("isOnline", PushConstants.BASE_STATUS.YES).set("onlineTime", websocketInfo.getOnlineTime());
  	    UpdateResult updateResult = mongoTemplate.updateFirst(query, update, "push_websocket_info");
		if(updateResult != null && updateResult.getMatchedCount() >0){
			return;
		}else {
			this.mongoTemplate.save(websocketInfo, "push_websocket_info");
		}
	}
	
	public void updateWebsocketToOffline(WebsocketInfo websocketInfo) {
		Query query = new Query();
  	    query.addCriteria(Criteria.where("_id").is(websocketInfo.get_id()));
  	    Update update = Update.update("isOnline", PushConstants.BASE_STATUS.NO).set("offlineTime", websocketInfo.getOfflineTime());
  	    mongoTemplate.updateFirst(query, update, "push_websocket_info");
	}
	
	public void savePushWebsocketMessageInfo(List<PushWebsocketMessage> pushWebsocketMessageList) {
		mongoTemplate.insert(pushWebsocketMessageList, "push_websocket_message_info");
	}
	
	public long rollbackMessageInfo(String _id) {
		
		String querySql  = "{'$and':[{ _id: { $regex: /"+_id+"_/, $options: 'si' }},{'status':'"+PushConstants.MESSAGE_PUSHED_STATUS.NOT_PUSH+"'}]}";
  	    Query query = new  BasicQuery(querySql);
  	    DeleteResult deleteResult = mongoTemplate.remove(query, "push_message_info");
  	    
  	    return deleteResult.getDeletedCount();
		
	}

}
