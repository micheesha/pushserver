package com.netty.client.android.listener;

/**
 * android Handler 回调监听
 * 
 * @author mengxuanliang
 * 
 * @param <T>
 */
public interface INettyHandlerListener<T> {
	public void callback(T t);
}
