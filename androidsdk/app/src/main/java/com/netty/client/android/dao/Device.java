package com.netty.client.android.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table t_device.
 */
public class Device {

    private Long id;
    private String appKey;
    private String deviceId;
    private String imei;
    private String appPackage;
    private String regId;
    private Integer isOnline;

    private String alias;
    private String tag;

    public Device() {
    }

    public Device(Long id) {
        this.id = id;
    }

    public Device(Long id, String appKey, String deviceId, String imei, String appPackage, String regId, Integer isOnline) {
        this.id = id;
        this.appKey = appKey;
        this.deviceId = deviceId;
        this.imei = imei;
        this.appPackage = appPackage;
        this.regId = regId;
        this.isOnline = isOnline;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getAppPackage() {
        return appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
    }

    public String getRegId() {
        return regId;
    }

    public void setRegId(String regId) {
        this.regId = regId;
    }

    public Integer getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Integer isOnline) {
        this.isOnline = isOnline;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
