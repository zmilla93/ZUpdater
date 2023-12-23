package com.zrmiller.zupdate;

public class ReleaseVersion {

    public final String TAG;
    public final String FILE_NAME;
    public final String DOWNLOAD_URL;

    /**
     * @param tag      GitHub tag, ie v0.1.0
     * @param fileName Name of the jar file to download
     * @param url      URL of the file to download
     */
    public ReleaseVersion(String tag, String fileName, String url) {
        TAG = tag;
        FILE_NAME = fileName;
        DOWNLOAD_URL = url;
    }

}