package com.zrmiller.zupdate;

import com.google.gson.*;
import com.zrmiller.App;
import com.zrmiller.saving.SaveFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class UpdateManager {

    private final String AUTHOR;
    private final String REPO;
    private final String DIRECTORY;

    private final String LATEST_VERSION_URL;
    private String downloadURL;

    private int fileSize;
    private int bytesProcessed;
    private final int BYTE_BUFFER_SIZE = 1024 * 4;

    private final Gson gson = new Gson();

    //    private String jsonFileName = "update.json";
    private SaveFile<UpdateSaveFile> saveFile;
    private DownloadVersion latestVersion;
    private String launchPath;
    private String jarName;
    private ArrayList<String> launchArgs = new ArrayList<>();

    public enum UpdateStage {NONE, DOWNLOAD, COPY, CLEAN}

    /**
     * Directory will contain update.json and store temp files, not the app itself
     *
     * @param author
     * @param repo
     * @param directory
     */
    public UpdateManager(String author, String repo, String directory, SaveFile<UpdateSaveFile> saveFile) {
        this.AUTHOR = author;
        this.REPO = repo;
        this.DIRECTORY = directory.endsWith("/") ? directory : directory + "/";
        this.saveFile = saveFile;
        LATEST_VERSION_URL = "https://api.github.com/repos/" + AUTHOR + "/" + REPO + "/releases/latest";
    }

    public void runUpdateProcess() {
        launchPath = getLaunchPath();
        launchArgs.add("launchPath:" + launchPath);
        runProcess(launchPath, "update");
    }

    public void runProcess(String path, String command) {
        ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(path);
        launchArgs.add(command);
//        args.add(command);
        ProcessBuilder builder = new ProcessBuilder(args);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean continueUpdateProcess(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("launchPath:")) {
                launchArgs.add(arg);
                launchPath = arg.replaceFirst("launchPath:", "");
                break;
            }
            if (arg.startsWith("jar"))
                launchArgs.add(arg);
        }
        for (String s : args) {
            switch (s) {
                case "update":
                    downloadFile();
                    runProcess(DIRECTORY + jarName, "copy");
                    break;
                case "copy":
                    copy();
                    runProcess(launchPath, "clean");
                    break;
                case "clean":

                    break;
            }
        }
        if (launchPath == null) {
            launchPath = getLaunchPath();
        }
        return true;
    }

    public boolean isUpdateAvailable() {
        String currentVersion = readCurrentVersion();
        if (currentVersion == null)
            return true;
        latestVersion = fetchLatestVersion();
        if (latestVersion == null)
            return false;
        return !currentVersion.equals(latestVersion.TAG);
    }

    public String readCurrentVersion() {
        return saveFile.data.tag;
    }

    public DownloadVersion fetchLatestVersion() {
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(LATEST_VERSION_URL).openConnection());
            BufferedReader inputStream = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            StringBuilder builder = new StringBuilder();
            while (inputStream.ready()) {
                builder.append(inputStream.readLine());
            }
            inputStream.close();
            JsonObject json = JsonParser.parseString(builder.toString()).getAsJsonObject();
            String tag = json.get("tag_name").getAsString();
            JsonObject asset = json.getAsJsonArray("assets").get(0).getAsJsonObject();
            jarName = asset.get("name").getAsString();
            launchArgs.add("jar:" + jarName);
            String url = asset.get("browser_download_url").getAsString();
            DownloadVersion version = new DownloadVersion(tag, jarName, url);
            return version;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeTag(String tag) {
        saveFile.data.tag = tag;
        saveFile.saveToDisk();
    }

    public boolean downloadFile() {
        try {
            if (latestVersion == null)
                fetchLatestVersion();
            if (latestVersion == null)
                return false;
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(latestVersion.DOWNLOAD_URL).openConnection());
            fileSize = httpConnection.getContentLength();
            BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(DIRECTORY + latestVersion.JAR_NAME));
            byte[] data = new byte[BYTE_BUFFER_SIZE];
            bytesProcessed = 0;
            int numBytesRead;
            while ((numBytesRead = inputStream.read(data, 0, BYTE_BUFFER_SIZE)) >= 0) {
                bytesProcessed += numBytesRead;
                outputStream.write(data, 0, numBytesRead);
            }
            inputStream.close();
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean validateDirectory() {
        File file = new File(DIRECTORY);
        if (file.exists())
            return file.isDirectory();
        return file.mkdirs();
    }

    private boolean copy() {
        try {
            Files.copy(Paths.get(DIRECTORY + jarName), Paths.get(launchPath), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getLaunchPath() {
        try {
            return App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

}
