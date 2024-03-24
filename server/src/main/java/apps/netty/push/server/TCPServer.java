package apps.netty.push.server;

/**
 * server接口
 * 
 * @author mengxuanliang
 * 
 */
public interface TCPServer {
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;
}
