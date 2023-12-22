package com.zrmiller.zupdate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * An update system for a single jar program using the GitHub API.
 * <p>
 * Example Usage:
 * <pre>
 *      UpdateManager updateManager = new UpdateManager(...);
 *      updateManager.continueUpdateProcess(args);
 *      if (updateManager.isUpdateAvailable()) {
 *          updateManager.addProgressListener(new IUpdateProgressListener(){...});
 *          updateManager.runUpdateProcess();
 *      }
 * </pre>
 */
public class UpdateManager {

    // These variables are read from pom.xml
//    public String APP_VERSION;
//    public String APP_NAME;
//    public String APP_URL;
//    private String CURRENT_VERSION;
    private AppVersion CURRENT_VERSION;

    // Validation checks
    private boolean appSettingsHaveBeenRead = false;

    private final String DIRECTORY;
    private final String LATEST_VERSION_URL;
    private final String ALL_RELEASES_URL;

    private static final int BYTE_BUFFER_SIZE = 1024 * 4;

    //    private final SaveFile<UpdateSaveFile> saveFile;
    private DownloadVersion latestVersion;
    private String launchPath;
    private String jarName;

    private final ArrayList<IUpdateProgressListener> progressListeners = new ArrayList<>();

    private static final String JAR_PREFIX = "jar:";
    private static final String LAUNCH_PATH_PREFIX = "launcher:";

    private boolean clean = false;

    /**
     * Directory will contain update.json and store temp files.
     *
     * @param author    GitHub author
     * @param repo      GitHub repo name
     * @param directory Directory where downloaded file will be stored temporarily
     */
    public UpdateManager(String author, String repo, String directory, AppVersion version) {
        this.DIRECTORY = directory;
        this.CURRENT_VERSION = version;
        LATEST_VERSION_URL = "https://api.github.com/repos/" + author + "/" + repo + "/releases/latest";
        ALL_RELEASES_URL = "https://api.github.com/repos/" + author + "/" + repo + "/releases";
    }

    /**
     * Begins the entire update process.
     */
    public void runUpdateProcess() {
        ZLogger.log("Running update process...");
        if (!isUpdateAvailable()) return;
        ZLogger.log("JARNAME:" + latestVersion.FILE_NAME);
        String[] args = new String[]{UpdateCommand.DOWNLOAD.toString(), JAR_PREFIX + latestVersion.FILE_NAME, LAUNCH_PATH_PREFIX + getLaunchPath()};
        continueUpdateProcess(args);
    }

    /**
     * Continues the update process if necessary. This should be called before anything else.
     * Uses command line args to pass information between runs.
     *
     * @param args The command line arguments of the program.
     */
    public void continueUpdateProcess(String[] args) {
        boolean download = false;
        boolean copy = false;
//        boolean clean = false;
        ArrayList<String> launchArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith(LAUNCH_PATH_PREFIX)) {
                launchArgs.add(arg);
                launchPath = arg.replaceFirst(LAUNCH_PATH_PREFIX, "");
                jarName = launchPath.replaceFirst(".*[\\\\/]", "");
                break;
            }
            if (arg.equals(UpdateCommand.DOWNLOAD.toString())) download = true;
            if (arg.equals(UpdateCommand.PATCH.toString())) copy = true;
            if (arg.equals(UpdateCommand.CLEAN.toString())) clean = true;
        }
        if (launchPath == null) {
            launchPath = getLaunchPath();
            launchArgs.add(LAUNCH_PATH_PREFIX + launchPath);
        }
        if (download) {
            boolean success = downloadFile();
            if (success) runProcess(DIRECTORY + jarName, UpdateCommand.PATCH, launchArgs);
        } else if (copy) {
            copy();
            runProcess(launchPath, UpdateCommand.CLEAN, launchArgs);
        } else if (clean) {
            clean();
        }
    }

    /**
     * Checks if a new version is available on GitHub. Won't ping multiple times.
     *
     * @return Update available
     */
    public boolean isUpdateAvailable() {
        return isUpdateAvailable(false);
    }

    /**
     * Checks if a new version is available on GitHub.
     *
     * @param forceCheck Ping GitHub even if it has already been pinged before
     * @return Update available
     */
    public boolean isUpdateAvailable(boolean forceCheck) {
//        if (!appSettingsHaveBeenRead) return false;
        // FIXME: Don't check clean here as it will break a repeat check
        if (clean) return false;
        String currentVersionString = CURRENT_VERSION.toString();
        if (currentVersionString == null)
            return false;
        if (latestVersion == null || forceCheck) {
            latestVersion = fetchLatestVersion();
            if (latestVersion == null)
                return false;
        }
        // FIXME : Move this so that it only prints once
        ZLogger.log("Current version: " + currentVersionString);
        ZLogger.log("Latest version: " + latestVersion.TAG);
        return !currentVersionString.equals(latestVersion.TAG);
    }

    /**
     * Reruns the program at the specified path, running the specified command on launch.
     *
     * @param path          Location of the .jar file to run
     * @param updateCommand Command to run
     */
    private void runProcess(String path, UpdateCommand updateCommand, ArrayList<String> additionalArgs) {
        ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(path);
        args.add(updateCommand.toString());
        args.add(ZLogger.getLaunchArg());
        args.addAll(additionalArgs);
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            ZLogger.log("Running '" + updateCommand + "' process... " + Arrays.toString(args.toArray()));
            ZLogger.close();
            builder.start();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private boolean readAppData() {
//        Properties properties = new Properties();
//        try {
//            InputStream stream = new BufferedInputStream(Objects.requireNonNull(UpdateManager.class.getClassLoader().getResourceAsStream("project.properties")));
//            properties.load(stream);
//            stream.close();
//        } catch (IOException e) {
//            ZLogger.err("Properties not found! Create a 'project.properties' file in the resources folder, then add the lines 'version=${project.version}' and 'artifactId=${project.artifactId}'.");
//            return false;
//        }
//        APP_NAME = properties.getProperty("name");
//        APP_VERSION = "v" + properties.getProperty("version");
//        APP_URL = properties.getProperty("url");
//        return true;
//    }

    private DownloadVersion fetchLatestVersion() {
        System.out.println("Fetching latest version... " + LATEST_VERSION_URL);
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
            String fileName = asset.get("name").getAsString();
            String url = asset.get("browser_download_url").getAsString();
            latestVersion = new DownloadVersion(tag, fileName, url);
            return latestVersion;
        } catch (IOException e) {
            ZLogger.log("UpdateManager failed to fetch latest version! Make sure there is a jar file uploaded to the releases section and has a version tag in the correct format!");
            return null;
        }
    }

    public void addProgressListener(IUpdateProgressListener progressListener) {
        progressListeners.add(progressListener);
    }

    public void removeProgressListener(IUpdateProgressListener progressListener) {
        progressListeners.remove(progressListener);
    }

//    public void writeTag(String tag) {
//        saveFile.data.tag = tag;
//        saveFile.saveToDisk();
//    }

    private boolean validateDirectory() {
        File file = new File(DIRECTORY);
        if (file.exists())
            return file.isDirectory();
        return file.mkdirs();
    }

    public DownloadVersion getLatestVersion() {
        return latestVersion;
    }

//    public String getCurrentVersionTag() {
//        return APP_VERSION;
//    }

    private String getLaunchPath() {
        try {
            String path = UpdateManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (path.startsWith("/")) path = path.replaceFirst("/", "");
            return path;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    ////////////////////////
    //  Updating Actions  //
    ////////////////////////

    /**
     * Downloads the new .jar file from GitHub.
     *
     * @return Success
     */
    private boolean downloadFile() {
        ZLogger.log("Downloading new version...");
        try {
            if (latestVersion == null) fetchLatestVersion();
            if (latestVersion == null) return false;
            ZLogger.log("File Name: " + latestVersion.FILE_NAME + "...");
            ZLogger.log("Version: " + latestVersion.TAG);
            ZLogger.log("URL: " + latestVersion.DOWNLOAD_URL);
            ZLogger.log("Output directory: " + DIRECTORY);
            ZLogger.log("Output file: " + latestVersion.FILE_NAME);
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(latestVersion.DOWNLOAD_URL).openConnection());
            int fileSize = httpConnection.getContentLength();
            ZLogger.log("File size:" + fileSize);
            BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(DIRECTORY + latestVersion.FILE_NAME)));
            byte[] data = new byte[BYTE_BUFFER_SIZE];
            int totalBytesRead = 0;
            int numBytesRead;
            int currentProgressPercent = 0;
            while ((numBytesRead = inputStream.read(data, 0, BYTE_BUFFER_SIZE)) >= 0) {
                outputStream.write(data, 0, numBytesRead);
                totalBytesRead += numBytesRead;
//                final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) fileSize)) * 100000d);
                int newProgressPercent = Math.round((float) totalBytesRead / fileSize * 100);
                if (newProgressPercent != currentProgressPercent) {
                    currentProgressPercent = newProgressPercent;
//                    ZLogger.log("Download progress: " + currentProgressPercent);
                    for (IUpdateProgressListener listener : progressListeners) {
                        listener.onDownloadProgress(currentProgressPercent);
                    }
                }
            }
            inputStream.close();
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ZLogger.log("Error while downloading file!");
            return false;
        }
    }

    /**
     * Copies the new jar from the temp directory to the user's original directory.
     *
     * @return Success
     */
    private boolean copy() {
        ZLogger.log("Copying file...");
        ZLogger.log("Target: " + DIRECTORY + jarName);
        ZLogger.log("Destination: " + launchPath);
        int MAX_COPY_ATTEMPTS = 5;
        for (int i = 0; i < MAX_COPY_ATTEMPTS; i++) {
            try {
                Files.copy(Paths.get(DIRECTORY + jarName), Paths.get(launchPath), StandardCopyOption.REPLACE_EXISTING);
                ZLogger.log("Files copied successfully.");
                return true;
            } catch (IOException e) {
//                e.printStackTrace();
                ZLogger.log("Error while copying files, retrying... (" + (i + 1) + ")");
                ZLogger.log(e.getStackTrace());
            }
        }
        ZLogger.log("Error while copying files!");
        return false;
    }

    private void clean() {
        // TODO : This
        ZLogger.log("Cleaning...");
    }

}
