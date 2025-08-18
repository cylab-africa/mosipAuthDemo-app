package com.example.mosipauth.dataObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class DeviceInformation {
    public String[] specVersion;
    public String digitalId;
    public String deviceId;
    public String deviceCode;
    public String env;
    public String purpose;
    public String serviceVersion;
    public String deviceStatus;
    public String firmware;
    public String certification;
    public int[] deviceSubId;
    public String callbackId;
}
