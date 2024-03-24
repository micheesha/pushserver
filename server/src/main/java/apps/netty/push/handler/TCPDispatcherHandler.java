package apps.netty.push.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import apps.netty.push.constants.PushConstants;
import apps.netty.push.handler.context.ApplicationContext;
import apps.netty.push.handler.process.IHandleProcessor;
import apps.netty.push.handler.process.factory.IHandleProcessorFactory;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * 分发器Handler
 * 
 * @author mengxuan
 * 
 */
@Sharable
@Service("dispatcherHandler")
@Scope("singleton")
public class TCPDispatcherHandler extends ChannelInboundHandlerAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(TCPDispatcherHandler.class);
	
	final static AttributeKey<String> tokenAttr = AttributeKey.newInstance("token");
	
	@Resource
	private IHandleProcessorFactory handleProcessorFactory;
	@Resource
	private ApplicationContext applicationContext;
	
	
	
	/**
     * 如果5秒没有验证通过，关闭连接
     * @description DelayClose
     * @author cgg
     * @version 0.1
     * @date 2014年8月16日
     */
    class DelayClose implements Runnable {

        private ChannelHandlerContext ctx;

        public DelayClose(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
        	
        	logger.info("开始检查连接有没有认证...");
        	
            Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(PushConstants.KEY_DELAY_CHECK);
            if(futureAttr.get() != null){
                futureAttr.remove();
            }
            if (!ctx.channel().isRegistered()) {
            	logger.info("{}检查10秒内有没有通过验证，连接已经被unRegistered", ctx.channel().toString());
                return;
            }
            
            String regId = ctx.channel().attr(PushConstants.KEY_REGID).get();
            if (StringUtils.isBlank(regId)) {
                // 没有验证通过，关闭连接
            	logger.info("at 10秒内没有通过验证，关闭连接", ctx.channel().toString());
            	
            	DecPushProtoc.PushPojo pojo = applicationContext.createConnectionFailedResult("After 10 seconds not pass validation,close connection");
            	
            	ctx.writeAndFlush(pojo);
            	
            	//close channel
                //ctx.pipeline().close();
                ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
                
                applicationContext.removeChannel(ctx.channel());
            }else {
            	logger.info("at 10秒内连接验证通过 regId or deviceId:"+regId);
            	futureAttr = ctx.channel().attr(PushConstants.KEY_DELAY_CHECK);
            	logger.info("connection validation job:"+futureAttr.get());
            }
        }

    }
	
	
	@Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		logger.info("channelRegistered.....");
		logger.info("channelRegistered:"+ctx.channel().isRegistered());
		
		DelayClose delayClose = new DelayClose(ctx);
        ScheduledFuture<?> future = ctx.executor().schedule(delayClose, 10, TimeUnit.SECONDS);
        Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(PushConstants.KEY_DELAY_CHECK);
        if(futureAttr.get() != null){
            futureAttr.get().cancel(false);
        }
        futureAttr.set(future);
		
	}
	
	@Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		logger.info("channelUnregistered.....");
		
		Attribute<ScheduledFuture<?>> futureAttr = ctx.channel().attr(PushConstants.KEY_DELAY_CHECK);
        if(futureAttr.get() != null){
            logger.info("remove future");
            futureAttr.get().cancel(false);
            futureAttr.remove();
        }
        logger.info("remove a attr KEY_REGID======");
        ctx.channel().attr(PushConstants.KEY_REGID).remove();
	}
	
	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		logger.info("userEventTriggered.....");
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // 空闲时间过久，关闭连接
                 
                ctx.pipeline().close();
            }
        }
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		// 将新的连接加入到ChannelGroup，当连接断开ChannelGroup会自动移除对应的Channel
		applicationContext.addChannel(ctx.channel());
		
		logger.info("applicationContext addChannel:======================\n" + ctx.channel().localAddress() + "-" + ctx.channel().remoteAddress());
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		
		logger.info("msg is:"+msg);
		
		IHandleProcessor handleProcessor = handleProcessorFactory.findHandleProcessor(ctx, msg);
		if (handleProcessor != null) {
			DecPushProtoc.PushPojo pushMessage = handleProcessor.process(ctx);
			if (pushMessage != null) {
				logger.info("writeAndFlush pushMessage:=======================\n" + pushMessage);
				ctx.writeAndFlush(pushMessage);
			}
		}
	}

	public void channelReadComplete(ChannelHandlerContext ctx) {
		// flush掉所有写回的数据
		// ctx.close().addListener(ChannelFutureListener.CLOSE); //
		// 当flush完成后关闭channel
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.info("server exceptionCaught====" + ctx.channel().remoteAddress() + "-" + cause.getMessage());
		cause.printStackTrace();// 捕捉异常信息
		
		logger.error("exceptionCaught",cause);
		
		//System.out.println("server exceptionCaught====" + ctx.channel().remoteAddress());
		// ctx.close();// 出现异常时关闭channel
		//applicationContext.offline(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("server channelInactive====" + ctx.channel().remoteAddress());
		applicationContext.removeChannel(ctx.channel());
		
	}

}
