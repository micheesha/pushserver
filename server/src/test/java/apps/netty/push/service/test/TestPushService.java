package apps.netty.push.service.test;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;
import com.mongodb.DBObject;
import apps.netty.push.ServerApplication;
import apps.netty.push.service.PushService;

@RunWith(SpringRunner.class)   
@SpringBootTest(classes={ServerApplication.class})// 指定启动类
public class TestPushService {

	@Resource
	PushService pushService;
	
	@Resource
	MongoTemplate mongoTemplate;
	
	@Test
    public void testSaveAppInfo() throws Exception {
		 
    }
}
