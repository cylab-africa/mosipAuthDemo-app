package com.example.mosipauth.dataObjects;

public class NewBioAuthDto {
    private String digitalId;
    private String deviceCode;
    private String deviceServiceVersion;
    private String bioSubType;
    private String purpose;
    private String env;
    private String count;
    private String bioType;
    private String bioValue;
    private String transactionId;
    private String timestamp;
    private String requestedScore;
    private String qualityScore;
    private String domainUri;

    public String getDomainUri() {
        return this.domainUri;
    }

    public void setDomainUri(String domainUri) {
        this.domainUri = domainUri;
    }

    public String getDigitalId() {
        return this.digitalId;
    }

    public void setDigitalId(String digitalId) {
        this.digitalId = digitalId;
    }

    public String getDeviceCode() {
        return this.deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceServiceVersion() {
        return this.deviceServiceVersion;
    }

    public void setDeviceServiceVersion(String deviceServiceVersion) {
        this.deviceServiceVersion = deviceServiceVersion;
    }

    public String getBioSubType() {
        return this.bioSubType;
    }

    public void setBioSubType(String bioSubType) {
        this.bioSubType = bioSubType;
    }

    public String getPurpose() {
        return this.purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getEnv() {
        return this.env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBioValue() {
        return this.bioValue;
    }

    public void setBioValue(String bioValue) {
        this.bioValue = bioValue;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestedScore() {
        return this.requestedScore;
    }

    public void setRequestedScore(String requestedScore) {
        this.requestedScore = requestedScore;
    }

    public String getQualityScore() {
        return this.qualityScore;
    }

    public void setQualityScore(String qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getBioType() {
        return this.bioType;
    }

    public void setBioType(String bioType) {
        this.bioType = bioType;
    }
}