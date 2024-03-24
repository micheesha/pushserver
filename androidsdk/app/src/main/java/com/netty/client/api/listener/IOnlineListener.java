package com.netty.client.api.listener;

import apps.netty.server.protoc.DecPushProtoc;

/**
 * Created by Vbe on 2019/5/10.
 */
public interface IOnlineListener {
    void receive(DecPushProtoc.ResultCode resultCode);
}
