package com.zrmiller.zupdate.data;

public class AppInfo {

    public final String name;
    public final AppVersion appVersion;
    public final String url;

    public AppInfo(String name, AppVersion appVersion, String url) {
        this.name = name;
        this.appVersion = appVersion;
        this.url = url;
    }

}
