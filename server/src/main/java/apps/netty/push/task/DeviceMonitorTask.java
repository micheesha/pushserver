package apps.netty.push.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import apps.netty.push.handler.context.ApplicationContext;

/**
 * 设备监控任务
 * 
 * @author mengxuanliang
 * 
 */
@Service("deviceMonitorTask")
@Scope("singleton")
public class DeviceMonitorTask extends TimerTask {
	
	private static Logger logger = LoggerFactory.getLogger(DeviceMonitorTask.class);
	
	// 超时时间
	private Long timeout = 240000L;//3分钟心跳时间，延时1分钟秒
	
	@Resource
	private ApplicationContext applicationContext;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	@Scheduled(fixedDelay = 240000) //每4分钟秒执行一次
	public void run() {
		try{
			if (applicationContext != null) {
				
				logger.info(sdf.format(new Date()) + "-DEVICEMONITORTASK EXECUTE!");
				
				applicationContext.deviceMonitors(timeout);
			}
		}catch(Exception e){
			logger.error("exception",e);		
		}
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}
}
