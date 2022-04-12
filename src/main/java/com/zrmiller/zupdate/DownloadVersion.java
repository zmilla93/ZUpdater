package com.zrmiller.zupdate;

public class DownloadVersion {
    public final String TAG;
    public final String JAR_NAME;
    public final String DOWNLOAD_URL;

    public DownloadVersion(String tag, String fileName, String url) {
        TAG = tag;
        JAR_NAME = fileName;
        DOWNLOAD_URL = url;
    }
}