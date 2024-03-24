package apps.netty.push.handler.process.factory;

import io.netty.channel.ChannelHandlerContext;

import apps.netty.push.handler.process.IHandleProcessor;

/**
 * 请求处理工厂接口 生产IHandleProcessor
 * 
 * @author mengxuanliang
 * 
 */
public interface IHandleProcessorFactory {
	public IHandleProcessor findHandleProcessor(ChannelHandlerContext ctx, Object msg);
}
