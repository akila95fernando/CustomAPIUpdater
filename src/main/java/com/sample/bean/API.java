package com.sample.bean;

public class API {
    private String id;
    private String name;
    private String description;
    private String context;
    private String version;
    private String provider;
    private String lifeCycleStatus;
    private String type;

    public API() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLifeCycleStatus() {
        return lifeCycleStatus;
    }

    public void setLifeCycleStatus(String lifeCycleStatus) {
        this.lifeCycleStatus = lifeCycleStatus;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "API{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", context='" + context + '\'' +
                ", version='" + version + '\'' +
                ", lifeCycleStatus='" + lifeCycleStatus + '\'' +
                '}';
    }
}