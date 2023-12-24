package com.zrmiller.zupdate.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ReleaseVersion implements Comparable<ReleaseVersion> {

    public final AppVersion version;
    public final String tag;
    public final String fileName;
    public final String downloadURL;
    public final String body;
    public final boolean preRelease;

    /**
     * @param tag      GitHub tag, ie v0.1.0
     * @param fileName Name of the jar file to download
     * @param url      URL of the file to download
     */
    public ReleaseVersion(String tag, String fileName, String url, String body, boolean preRelease) {
        this.tag = tag;
        this.version = new AppVersion(tag);
        this.fileName = fileName;
        this.downloadURL = url;
        this.body = body;
        this.preRelease = preRelease;
    }

    public ReleaseVersion(JsonElement json) {
        this(json.getAsJsonObject());
    }

    public ReleaseVersion(JsonObject json) {
        tag = json.get("tag_name").getAsString();
        version = new AppVersion(tag);
        JsonObject asset = json.getAsJsonArray("assets").get(0).getAsJsonObject();
        fileName = asset.get("name").getAsString();
        downloadURL = asset.get("browser_download_url").getAsString();
        body = json.get("body").getAsString();
        preRelease = json.get("prerelease").getAsBoolean();
    }

    @Override
    public int compareTo(ReleaseVersion other) {
        return version.compareTo(other.version);
    }

    @Override
    public String toString() {
        return version.toString();
    }

}