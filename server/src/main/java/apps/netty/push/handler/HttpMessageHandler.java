package apps.netty.push.handler;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import apps.netty.push.constants.PushConstants;
import apps.netty.push.handler.context.ApplicationContext;
import apps.netty.push.pojo.MessageInfo;
import apps.netty.push.pojo.WebsocketMessage;

/**
 * 
* Title: NettyServerHandler
* Description: 服务端业务逻辑
* Version:1.0.0  
* @author mengxuan liang
* @date 2019.03.11
 */
@Sharable
@Service("httpMessageHandler")
public class HttpMessageHandler extends ChannelInboundHandlerAdapter {
	
	private static Logger logger = LoggerFactory.getLogger(HttpMessageHandler.class);
	
	@Resource
	private ApplicationContext applicationContext;
	
	private String result="";
    /*
     * 收到消息时，返回信息
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(! (msg instanceof FullHttpRequest)){
            result="未知请求!";
            send(ctx,result,HttpResponseStatus.BAD_REQUEST);
            return;
        }
        FullHttpRequest httpRequest = (FullHttpRequest)msg;
        
        try{
        	
        	 HttpMethod method=httpRequest.method();//获取请求方法
        	
        	 //如果是GET请求
            if(HttpMethod.GET.equals(method)){ 
                //接受到的消息，做业务逻辑处理...
                result="GET request not support";
                send(ctx,result,HttpResponseStatus.BAD_REQUEST);
                return;
            }
        	
            String path=httpRequest.uri();          //获取路径
            String requestParam = getBody(httpRequest);     //获取参数
            
            logger.info("requestPath:"+path);
            logger.info("requestParam:"+requestParam);
           
             if(!path.contains("?accessToken=")) {
            	 result="Bad request,url should like '?accessToken='";
                 send(ctx,result,HttpResponseStatus.BAD_REQUEST);
                 return;
             }
             
             if(!path.contains("/sendByAlias") 
            		 && !path.contains("/ws/sendToUserList")
            		 && !path.contains("/pushmessage/rollback")) {
            	 result="Bad request,url not find";
                 send(ctx,result,HttpResponseStatus.BAD_REQUEST);
                 return;
             }
             
            String accessToken = path.split("\\?")[1].replace("accessToken=", "");
            
            logger.info("accessToken:"+accessToken);
            
            if(StringUtils.isBlank(accessToken) || !applicationContext.validateToken(accessToken,PushConstants.TOKEN_TYPE.HTTP)) {
            	result="Bad request,accessToken not find";
                send(ctx,result,HttpResponseStatus.BAD_REQUEST);
                return;
            }
            
            //如果不是这个路径，就直接返回错误
            if(path.contains("/sendByAlias")){
            	MessageInfo messageInfo = null;
            	try{
            		messageInfo = new Gson().fromJson(requestParam, MessageInfo.class);
	          	}catch(Exception e){
	          		
	          		logger.error("exception",e);
	          	}
            	if(null != messageInfo) {
            		//push message to related device by  alias
            		Map<String,Object> result = new HashMap<>();
            		try{
            			result = applicationContext.sendMessageByAlias(messageInfo);
            		}catch(Exception e) {
            			logger.error("exception while sendMessageByAlias",e);
            			result.put("errorMessage", "exception while sendMessageByAlias");
            		}
                    send(ctx,new Gson().toJson(result),HttpResponseStatus.OK);
            	}else {
            		send(ctx,"message is not corerct",HttpResponseStatus.BAD_REQUEST);
            	}
            	
                return;
            }
            
            //如果不是这个路径，就直接返回错误
            if(path.contains("/pushmessage/rollback")){
            	MessageInfo messageInfo = null;
            	try{
            		messageInfo = new Gson().fromJson(requestParam, MessageInfo.class);
	          	}catch(Exception e){
	          		
	          		logger.error("exception",e);
	          	}
            	if(null != messageInfo) {
            		//push message to related device by  alias
                	Map<String,Object> result = new HashMap<>();
            		try{
            			result = applicationContext.rollbackMessage(messageInfo);
            		}catch(Exception e) {
            			logger.error("exception while sendMessageByAlias",e);
            			result.put("errorMessage", "exception while rollbackMessage");
            		}
                    send(ctx,new Gson().toJson(result),HttpResponseStatus.OK);
            	}else {
            		send(ctx,"message is not corerct",HttpResponseStatus.BAD_REQUEST);
            	}
            	
                return;
            }
            
            //如果不是这个路径，就直接返回错误
            if(path.contains("/ws/sendToUserList")){
            	
            	WebsocketMessage messageInfo = null;
            	try{
            		  messageInfo = new Gson().fromJson(requestParam, WebsocketMessage.class);
            	}catch(Exception e){
            		
            		logger.error("exception",e);
            	}
            	if(null != messageInfo) {
            		//push message to related device by  alias
                	Map<String,Object> result = applicationContext.sendWebsocketMessageToUserList(messageInfo);
                	
                    send(ctx,new Gson().toJson(result),HttpResponseStatus.OK);
            	}else {
            		send(ctx,"message is not corerct",HttpResponseStatus.BAD_REQUEST);
            	}
                return;
            }
            
            
            logger.info("接收到:"+method+" 请求");
            
            //如果是POST请求
            if(HttpMethod.POST.equals(method)){ 
                //接受到的消息，做业务逻辑处理...
                result="POST请求";
                send(ctx,result,HttpResponseStatus.OK);
                return;
            }

            //如果是PUT请求
            if(HttpMethod.PUT.equals(method)){ 
                //接受到的消息，做业务逻辑处理...
                result="PUT请求";
                send(ctx,result,HttpResponseStatus.OK);
                return;
            }
            //如果是DELETE请求
            if(HttpMethod.DELETE.equals(method)){ 
                //接受到的消息，做业务逻辑处理...
                result="DELETE请求";
                send(ctx,result,HttpResponseStatus.OK);
                return;
            }
        }catch(Exception e){
            logger.error("exception: ",e);
        }finally{
            //释放请求
            httpRequest.release();
        }   
    }   
    /**
     * 获取body参数
     * @param request
     * @return
     */
    private String getBody(FullHttpRequest request){
        ByteBuf buf = request.content();
        return buf.toString(CharsetUtil.UTF_8);
    }

    /**
     * 发送的返回值
     * @param ctx     返回
     * @param context 消息
     * @param status 状态
     */
    private void send(ChannelHandlerContext ctx, String context,HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(context, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /*
     * 建立连接时，返回消息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	logger.info("连接的客户端地址:" + ctx.channel().remoteAddress());
        ctx.writeAndFlush("客户端"+ InetAddress.getLocalHost().getHostName() + "成功与服务端建立连接！ ");
        super.channelActive(ctx);
    }
}
 
