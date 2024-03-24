package apps.netty.push.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import apps.netty.push.server.HttpServer;
import apps.netty.push.server.NettyHttpServer;
import apps.netty.push.server.NettyTCPServer;
import apps.netty.push.server.NettyWebsocketServer;
import apps.netty.push.server.TCPServer;
import apps.netty.push.server.WebSocketServer;

/**
 * 自动化配置初始化服务
 *
 * @author mengxuanliang
 * @create 2018-10-14 19:52
 **/
@Configuration
@ConditionalOnClass
public class ServerAutoStarter {


    private static  final  int _BLACKLOG =   1024;

    private static final  int  CPU =Runtime.getRuntime().availableProcessors();

    private static final  int  SEDU_DAY =10;

    private static final  int TIMEOUT =120;

    private static final  int BUF_SIZE=10*1024*1024;


    public ServerAutoStarter(){

    }

    

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public TCPServer initTCPServer(){
         
        return new NettyTCPServer();
    }
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public HttpServer initHttpServer(){
         
        return new NettyHttpServer();
    }
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public WebSocketServer initWebsocketServer(){
         
        return new NettyWebsocketServer();
    }
}
