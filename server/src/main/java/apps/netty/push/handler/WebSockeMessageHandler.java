package apps.netty.push.handler;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import apps.netty.push.constants.PushConstants;
import apps.netty.push.handler.context.ApplicationContext;

/**
 * 接受/处理/响应客户端websocke请求的核心业务处理类
 */
@Sharable
@Service("websocketMessageHandler")
public class WebSockeMessageHandler extends SimpleChannelInboundHandler<Object> {

	private static Logger logger = LoggerFactory.getLogger(WebSockeMessageHandler.class);
	
    private WebSocketServerHandshaker webSocketServerHandshaker;
    
    @Value("${websocket.url}")
    private String WEB_SOCKET_URL;;
    
    @Resource
	private ApplicationContext applicationContext;
    
    //客户端与服务端创建链接的时候调用
    @Override
    public void channelActive (ChannelHandlerContext context)throws Exception{
        
    	logger.info("WEB_SOCKET_URL:"+WEB_SOCKET_URL);
    	logger.info("websocket connection open....");
    	
    }
    //客户端与服务端断开连接的时候调用
    @Override
    public void channelInactive(ChannelHandlerContext context)throws Exception{
    	logger.info("websocket server channelInactive====" + context.channel().remoteAddress());
    	applicationContext.removeWebsocketChannel(context.channel());
    }
    //服务端接收客户端发送过来的数据结束之后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext context)throws Exception{
        context.flush();
    }
    //工程出现异常的时候调用
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable)throws Exception{
        throwable.printStackTrace();
        context.close();
    }
    //服务端处理客户端websocke请求的核心方法
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        
    	logger.info("channelRead0 param:"+o);
    	
    	//处理客户端向服务端发起的http握手请求
        if (o instanceof FullHttpRequest){
        	FullHttpRequest  request = (FullHttpRequest) o;
        	String url = request.uri();
        	logger.info("channelRead0 url is:"+url);
        	
        	String uid = url.split("/")[3];
        	String token = url.split("/")[5];
        	logger.info("uid is:"+uid);
        	logger.info("token is:"+token);
        	
        	//TODO,VALIDATE token
        	if(applicationContext.validateToken(token,PushConstants.TOKEN_TYPE.WEBSOCKET)) {
                handHttpRequest(channelHandlerContext,request,uid);
        	}else {
        		logger.info("websocket connection token:"+token +" is invalid ,close connection");
        		sendValidateMessage(channelHandlerContext,"invalid token",HttpResponseStatus.BAD_REQUEST);
        		channelHandlerContext.channel().close().addListener(ChannelFutureListener.CLOSE);
        	}
        	
        	
        }else if (o instanceof WebSocketFrame){//处理websocket链接业务
        	
            handWebSocketFrame(channelHandlerContext,(WebSocketFrame) o);
        }
    }

    /**
     * 处理客户端与服务端之间的websocket业务
     * @param context
     * @param webSocketFrame
     */
    private void handWebSocketFrame(ChannelHandlerContext context,WebSocketFrame webSocketFrame){
    	
        if (webSocketFrame instanceof CloseWebSocketFrame){//判断是否是关闭websocket的指令
            webSocketServerHandshaker.close(context.channel(),(CloseWebSocketFrame) webSocketFrame.retain());
        }
        if (webSocketFrame instanceof PingWebSocketFrame){//判断是否是ping消息
            context.channel().write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }
        if (!(webSocketFrame instanceof TextWebSocketFrame)){//判断是否是二进制消息
            logger.info("不支持二进制消息");
            
            throw new RuntimeException(this.getClass().getName());
        }
        //返回应答消息
        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame) webSocketFrame ).text();
        //System.out.println("服务端收到客户端的消息：" + request);
        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(context.channel().id() + ":" + request);
        //服务端向每个连接上来的客户端发送消息
        context.channel().writeAndFlush(textWebSocketFrame);
    }




    /**
     * 处理客户端向服务端发起http握手请求业务
     * @param context
     * @param fullHttpRequest
     */
    private void handHttpRequest(ChannelHandlerContext context,FullHttpRequest fullHttpRequest,String uid){
    	
        if (!fullHttpRequest.getDecoderResult().isSuccess() ||!("websocket".equals(fullHttpRequest.headers().get("Upgrade")))){//判断是否http握手请求
            sendHttpResponse(context,fullHttpRequest, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL,null,false);
        webSocketServerHandshaker = webSocketServerHandshakerFactory.newHandshaker(fullHttpRequest);
        if (webSocketServerHandshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
        }else{
            webSocketServerHandshaker.handshake(context.channel(),fullHttpRequest);
        }
        
        applicationContext.addWebsocketChannel(context.channel(),uid);
        
        //send offline message
        try{
        	applicationContext.sendOfflineMessageToWebsocketUser(uid);
        }catch(Exception e){
        	logger.error("exception",e);
        }
    }
    
    /**
     * 服务端想客户端发送响应消息
     * @param context
     * @param fullHttpRequest
     * @param defaultFullHttpResponse
     */
    private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest fullHttpRequest, DefaultFullHttpResponse defaultFullHttpResponse){
        if (defaultFullHttpResponse.getStatus().code() != 200){
            ByteBuf buf = Unpooled.copiedBuffer(defaultFullHttpResponse.getStatus().toString(), CharsetUtil.UTF_8);
            defaultFullHttpResponse.content().writeBytes(buf);
            buf.release();
        }
        //服务端向客户端发送数据
        ChannelFuture future = context.channel().writeAndFlush(defaultFullHttpResponse);
        if (defaultFullHttpResponse.getStatus().code() !=200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private void sendValidateMessage(ChannelHandlerContext ctx, String context,HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(context, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
