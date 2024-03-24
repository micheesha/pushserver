package com.netty.client.api.listener;

import apps.netty.server.protoc.DecPushProtoc;;

/**
 * 客户端连接监听器
 * 
 * @类名称：IConnectionListener
 * @类描述：
 * @创建人：mengxuanliang
 * @创建时间：2014-10-28 下午12:41:04
 * 
 */
public interface IConnectionListener {
	void receive(DecPushProtoc.PushMessage message);
}
