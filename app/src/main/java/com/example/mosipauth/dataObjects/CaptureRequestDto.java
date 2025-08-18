package com.example.mosipauth.dataObjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class CaptureRequestDto {
    public String env;
    public String purpose;
    public String specVersion;
    public int timeout;
    public String captureTime;
    public String transactionId;
    public String domainUri;
    @JsonProperty("bio")
    public List<CaptureRequestDeviceDetailDto> mosipBioRequest;
    public List<Map<String, String>> customOpts;
}
