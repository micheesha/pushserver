package apps.netty.push.handler.process;

import io.netty.channel.ChannelHandlerContext;

import apps.netty.server.protoc.DecPushProtoc;

/**
 * 消息回执处理
 * 
 * @author mengxuanliang
 * 
 */
public class MessageReceiptProcessor extends AbstractHandleProcessor<DecPushProtoc.PushMessageReceipt> {

	@Override
	public DecPushProtoc.PushPojo process(ChannelHandlerContext ctx) {
		DecPushProtoc.PushMessageReceipt messageReceipt = this.getProcessObject();
		if (messageReceipt != null) {
			applicationContext.updateMessageReceipt(messageReceipt.getMsgId());
		}
		return null;
	}
}
