package apps.netty.push.constants;

import java.util.concurrent.ScheduledFuture;

import io.netty.util.AttributeKey;

public class PushConstants {

	public final class MESSAGE_PUSHED_STATUS {
		public static final String NOT_PUSH = "NOT_PUSH";
		public static final String RECEIPT_RECEIVED = "RECEIPT_RECEIVED";
		public static final String PUSH_SUCCESS = "PUSH_SUCCESS";
	}
	
	public final class BASE_STATUS {
		public static final String YES = "Y";  
		public static final String NO = "N";  
		
		public static final String ACTIVE = "A";  
		public static final String INACTIVE = "I";  
	}
	
	public final class TOKEN_TYPE {
		public static final String HTTP = "HTTP";
		public static final String WEBSOCKET = "WEBSOCKET";
	}
	
	public static final AttributeKey<ScheduledFuture<?>> KEY_DELAY_CHECK = AttributeKey.valueOf("KEY_DELAY_CHECK");
	public static final AttributeKey<String> KEY_REGID = AttributeKey.valueOf("KEY_REGID");
	public static final AttributeKey<String> KEY_DEVICEID = AttributeKey.valueOf("KEY_DEVICEID");
	
	public static final AttributeKey<String> KEY_UID = AttributeKey.valueOf("KEY_UID");
	
}
