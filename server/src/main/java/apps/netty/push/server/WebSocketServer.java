package apps.netty.push.server;

/**
 * server接口
 * 
 * @author mengxuanliang
 * 
 */
public interface WebSocketServer {
	public void start() throws Exception;

	public void stop() throws Exception;

	public void restart() throws Exception;
}
