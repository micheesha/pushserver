package apps.netty.push.service.test;

import com.google.gson.Gson;

import apps.netty.push.pojo.MessageInfo;

public class GenerateJson {

	public static void main(String[] args) {
		 
		MessageInfo messageInfo = new MessageInfo();
		
		messageInfo.setTitle("test");
		messageInfo.setContent("asdasdasd");
		messageInfo.setNeedReceipt(true);
		messageInfo.setAlias("12345");
		messageInfo.setMessageType(100);
		
		System.out.println(new Gson().toJson(messageInfo));
		
		

	}

}
