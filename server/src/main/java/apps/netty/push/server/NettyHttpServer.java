package apps.netty.push.server;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;

import com.google.protobuf.ExtensionRegistry;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import apps.netty.push.handler.HttpMessageHandler;
import apps.netty.push.handler.context.ApplicationContext;
import apps.netty.push.utils.SystemPrintUtil;
import apps.netty.server.protoc.DecPushProtoc;

/**
 * server处理类main方法启动server
 * 
 * @author mengxuanliang
 * 
 */
@Scope("singleton")
public class NettyHttpServer implements HttpServer {

	private static Logger logger = LoggerFactory.getLogger(NettyHttpServer.class);
	
	// 服务端口
	@Value("${http.server.port}")
	private int port;

	@Value("${server.ipAddress}")
	private String ipAddress;
	
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	@Resource
	private HttpMessageHandler httpMessageHandler;
	
	@Resource
	private ApplicationContext applicationContext;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * 启动服务器方法入口
	 */
	@Override
	public void start() throws Exception {
		
		Thread tcpThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				bossGroup = new NioEventLoopGroup(); // (1)
				workerGroup = new NioEventLoopGroup();
				try {
					// 引导辅助程序
					ServerBootstrap sb = new ServerBootstrap(); // (2)
					// 通过nio方式来接收连接和处理连接
					sb.group(bossGroup, workerGroup);
					// 设置nio类型的channel
					sb.channel(NioServerSocketChannel.class); // (3)
					// 设置监听端口
					//sb.localAddress(new InetSocketAddress(port));
					sb.localAddress(ipAddress, port);
					// 有连接到达时会创建一个channel
					final ExtensionRegistry registry = ExtensionRegistry.newInstance();
					DecPushProtoc.registerAllExtensions(registry);

					sb.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							// pipeline管理channel中的Handler，在channel队列中添加一个handler来处理业务
					         //处理http服务的关键handler
							pipeline.addLast("encoder",new HttpResponseEncoder());
							pipeline.addLast("decoder",new HttpRequestDecoder());
							pipeline.addLast("aggregator", new HttpObjectAggregator(10*1024*1024)); 
							pipeline.addLast("handler", httpMessageHandler);// 服务端业务逻辑
							
						}
					});
					sb.option(ChannelOption.SO_BACKLOG, 128); // (5)
					sb.childOption(ChannelOption.SO_KEEPALIVE, false); // (6)
					// Bind and start to accept incoming connections.
					// 配置完成，开始绑定server，通过调用sync同步方法阻塞直到绑定成功
					ChannelFuture cf = sb.bind().sync(); // (7)
					applicationContext.addChannel(cf.channel());
					System.out.println("server applicationContext addChannel:======================\n" + cf.channel().localAddress() + "-"
							+ cf.channel().remoteAddress());
					SystemPrintUtil.printServerInfo(this.getClass().getName() + " started and listen on " + cf.channel().localAddress());
					// Wait until the server socket is closed.
					cf.channel().closeFuture().sync();
				}catch(Exception e){
					e.printStackTrace();
				}finally {
					System.out.println("finally!!!");
					try{
						stopServer();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				
			}
		});
		
		tcpThread.start();
		
	}

	/**
	 * 重启netty服务器
	 */
	@Override
	public void restart() throws Exception {
		SystemPrintUtil.printServerInfo(this.getClass().getName() + " restarting  netty server on port:" + port);
		ChannelGroupFuture future = applicationContext.closeAllChannels();
		if (future != null) {
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().close();
					stopServer();
					start();
				}
			});
		}
	}

	/**
	 * 停止服务器
	 */
	@Override
	public void stop() throws Exception {
		ChannelGroupFuture future = applicationContext.closeAllChannels();
		
		logger.info("stop http server future class:"+future.getClass().getName());
		
		if (future != null) {
			
			future.awaitUninterruptibly();
			//stopServer();
			/*future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.channel().close();
					stopServer();
				}
			});*/
		}

	}

	private void stopServer() throws Exception {
		if (workerGroup != null) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}

		if (bossGroup != null) {
			bossGroup.shutdownGracefully();
			bossGroup = null;
		}
		applicationContext.destory();
		SystemPrintUtil.printServerInfo(this.getClass().getName() + " stop netty server on " + this.port + " success!");
	}

	public static void main(String[] args) {
		String dateString = new Date().toString();

		System.out.println(dateString);
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US);// MMM
		// dd
		// hh:mm:ss
		// Z
		// yyyy
		// DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
		// Locale.getDefault());
		try {
			System.out.println(sdf.parse(dateString));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
