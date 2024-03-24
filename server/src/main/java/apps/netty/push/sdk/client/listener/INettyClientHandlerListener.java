package apps.netty.push.sdk.client.listener;

import apps.netty.push.sdk.pojo.MessageResult;

public interface INettyClientHandlerListener {
	public void receive(MessageResult messageResult);
}
