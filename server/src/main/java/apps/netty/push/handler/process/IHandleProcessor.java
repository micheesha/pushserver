package apps.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;
import apps.netty.server.protoc.DecPushProtoc;

public interface IHandleProcessor {
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx);

	public void updateObject(Object t);

}
