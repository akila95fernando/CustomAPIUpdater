package com.sample.bean;

public class Operation {
    private String id;
    private String target;
    private String verb;
    private String authType;
    private String throttlingPolicy;

    public Operation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getThrottlingPolicy() {
        return throttlingPolicy;
    }

    public void setThrottlingPolicy(String throttlingPolicy) {
        this.throttlingPolicy = throttlingPolicy;
    }

    @Override
    public String toString() {
        return "Operation{" +
                "target='" + target + '\'' +
                ", verb='" + verb + '\'' +
                ", throttlingPolicy='" + throttlingPolicy + '\'' +
                '}';
    }
}