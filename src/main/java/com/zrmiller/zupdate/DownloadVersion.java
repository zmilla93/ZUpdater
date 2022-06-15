package com.zrmiller.zupdate;

public class DownloadVersion {

    public final String TAG;
    public final String FILE_NAME;
    public final String DOWNLOAD_URL;

    /**
     * @param tag       v0.1.0
     * @param fileName  ZUpdater.jar
     * @param url       https://github.com/zmilla93/ZUpdater/releases
     */
    public DownloadVersion(String tag, String fileName, String url) {
        TAG = tag;
        FILE_NAME = fileName;
        DOWNLOAD_URL = url;
    }
}