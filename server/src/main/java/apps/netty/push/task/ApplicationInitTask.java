package apps.netty.push.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import apps.netty.push.handler.context.ApplicationContext;

/**
 * Application初始化任务
 * 
 * @author mengxuanliang
 * 
 */
@Service("applicationInitTask")
@Scope("singleton")
public class ApplicationInitTask extends TimerTask {
	
	private static Logger logger = LoggerFactory.getLogger(ApplicationInitTask.class);
	
	@Resource
	private ApplicationContext applicationContext;

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	public void run() {
		try{
			if (applicationContext != null) {
				
				logger.info(sdf.format(new Date()) + "-APPLICATIONINITTASK EXECUTE!");
				applicationContext.init();
			}
		}catch(Exception e){
			logger.error("exception",e);		
		}
	}
}
