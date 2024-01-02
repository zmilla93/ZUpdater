package com.zrmiller.zupdate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.zrmiller.zupdate.data.AppVersion;
import com.zrmiller.zupdate.data.ReleaseVersion;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * An update system for a single JAR program using the GitHub API.
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

    // TODO : Add a function to fetch patch notes, and possibly save them to disk.

    private static final int BYTE_BUFFER_SIZE = 1024 * 4;
    private static final String LAUNCH_PATH_PREFIX = "launcher:";
    private static final String TEMP_FILE_NAME = "SlimTrade-Updater.jar";

    private final AppVersion CURRENT_VERSION;
    private final String DIRECTORY;
    private final String LATEST_VERSION_URL;
    private final String ALL_RELEASES_URL;
    private final boolean VALID_DIRECTORY;

    private ReleaseVersion latestRelease;
    private boolean allowPreRelease;
    private String launchPath;
    private UpdateAction currentAction = UpdateAction.NONE;
    private final ArrayList<IUpdateProgressListener> progressListeners = new ArrayList<>();

    private static final int MAX_ACTION_ATTEMPTS = 5;
    private static final int ACTION_RETRY_DELAY_MS = 50;

    /**
     * Handles updating a single JAR file program using the GitHub API.
     *
     * @param author    GitHub author
     * @param repo      GitHub repo name
     * @param directory Directory where downloaded file will be stored temporarily
     * @param version   Version information about the currently running program
     */
    public UpdateManager(String author, String repo, String directory, AppVersion version, boolean allowPreRelease) {
        this.DIRECTORY = UpdateUtil.cleanFileSeparators(directory);
        this.CURRENT_VERSION = version;
        this.allowPreRelease = allowPreRelease;
        LATEST_VERSION_URL = "https://api.github.com/repos/" + author + "/" + repo + "/releases/latest";
        ALL_RELEASES_URL = "https://api.github.com/repos/" + author + "/" + repo + "/releases";
        VALID_DIRECTORY = UpdateUtil.validateDirectory(DIRECTORY);
        if (!VALID_DIRECTORY) ZLogger.log("Failed to validate directory: " + DIRECTORY);
    }

    /**
     * Begins the entire update process.
     */
    public void runUpdateProcess() {
        String[] args = new String[]{UpdateAction.DOWNLOAD.toString(), LAUNCH_PATH_PREFIX + getLaunchPath()};
        continueUpdateProcess(args);
    }

    /**
     * Continues the update process if necessary. This should be called before anything else.
     * Uses command line args to pass information between runs.
     *
     * @param args The command line arguments of the program.
     */
    public void continueUpdateProcess(String[] args) {
        // Parse program args
        ArrayList<String> launchArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith(LAUNCH_PATH_PREFIX)) {
                launchArgs.add(arg);
                launchPath = arg.replaceFirst(LAUNCH_PATH_PREFIX, "");
                continue;
            }
            if (arg.equals(UpdateAction.DOWNLOAD.toString())) currentAction = UpdateAction.DOWNLOAD;
            if (arg.equals(UpdateAction.PATCH.toString())) currentAction = UpdateAction.PATCH;
            if (arg.equals(UpdateAction.CLEAN.toString())) currentAction = UpdateAction.CLEAN;
        }
        if (launchPath == null) {
            launchPath = getLaunchPath();
            launchArgs.add(LAUNCH_PATH_PREFIX + launchPath);
        }
        // Run the target action based on args
        switch (currentAction) {
            case DOWNLOAD -> {
                boolean success = downloadFile();
                if (success) runProcess(DIRECTORY + TEMP_FILE_NAME, UpdateAction.PATCH, launchArgs);
            }
            case PATCH -> {
                patch();
                runProcess(launchPath, UpdateAction.CLEAN, launchArgs);
            }
            case CLEAN -> clean();
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
        if (!VALID_DIRECTORY) return false;
        String currentVersionString = CURRENT_VERSION.toString();
        if (currentVersionString == null) return false;
        if (latestRelease == null || forceCheck) {
            ZLogger.log("Checking for update...");
            ZLogger.log("Current version: " + currentVersionString);
            if (allowPreRelease) latestRelease = fetchLatestReleaseFromAll();
            else latestRelease = fetchLatestRelease();
            if (latestRelease == null) return false;
            ZLogger.log("Latest version: " + latestRelease.tag);
        }
        boolean updateAvailable = !currentVersionString.equals(latestRelease.tag);
        if (updateAvailable) ZLogger.log("Update available!");
        else ZLogger.log("Program is up to date.");
        return updateAvailable;
    }

    /**
     * Reruns the program at the specified path, running the specified UpdateAction on launch.
     *
     * @param path           Location of the JAR file to run
     * @param updateAction   UpdateAction to perform
     * @param additionalArgs A list of command line arguments that should be passed to the next program run
     */
    private void runProcess(String path, UpdateAction updateAction, ArrayList<String> additionalArgs) {
        ArrayList<String> args = new ArrayList<>();
        args.add("java");
        args.add("-jar");
        args.add(path);
        args.add(updateAction.toString());
        args.add(ZLogger.getLaunchArg());
        args.addAll(additionalArgs);
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            ZLogger.log("Running '" + updateAction + "' process... " + Arrays.toString(args.toArray()));
            ZLogger.close();
            builder.start();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ReleaseVersion fetchLatestRelease() {
        JsonElement json = fetchDataFromGitHub(LATEST_VERSION_URL);
        if (json == null) return null;
        return new ReleaseVersion(json);
    }

    public ReleaseVersion fetchLatestReleaseFromAll() {
        JsonElement json = fetchDataFromGitHub(ALL_RELEASES_URL);
        if (json == null) return null;
        JsonArray array = json.getAsJsonArray();
        ArrayList<ReleaseVersion> versions = new ArrayList<>();
        for (JsonElement entry : array) {
            ReleaseVersion releaseVersion = new ReleaseVersion(entry);
            if (!releaseVersion.appVersion.valid) continue;
            versions.add(releaseVersion);
        }
        Collections.sort(versions);
        for (ReleaseVersion version : versions) {
            System.out.println(version);
        }
        return versions.get(versions.size() - 1);
    }

    /**
     * Fetches data from a GitHub API endpoint.
     *
     * @param url GitHub API endpoint
     * @return A JSON response, or null if request failed.
     */
    private JsonElement fetchDataFromGitHub(String url) {
        try {
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(url).openConnection());
            BufferedReader inputStream;
            try {
                inputStream = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            } catch (IOException e) {
                ZLogger.log("Failed to connect to GitHub. This is either a connection issue or the API rate has been exceeded.");
                return null;
            }
            StringBuilder builder = new StringBuilder();
            while (inputStream.ready()) builder.append(inputStream.readLine());
            inputStream.close();
            return JsonParser.parseString(builder.toString());
        } catch (MalformedURLException e) {
            ZLogger.log("Failed to fetch data from GitHub, bad URL: " + url);
        } catch (IOException e) {
            ZLogger.log("Failed to fetch data from GitHub.");
        }
        return null;
    }

    private String getLaunchPath() {
        try {
            String path = UpdateManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (path.startsWith("/")) path = path.replaceFirst("/", "");
            return UpdateUtil.cleanFileSeparators(path);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UpdateAction getCurrentUpdateAction() {
        return currentAction;
    }

    /////////////////////////
    //  Progress Listeners //
    /////////////////////////

    public void addProgressListener(IUpdateProgressListener progressListener) {
        progressListeners.add(progressListener);
    }

    public void removeProgressListener(IUpdateProgressListener progressListener) {
        progressListeners.remove(progressListener);
    }

    ////////////////////////
    //  Updating Actions  //
    ////////////////////////

    /**
     * Downloads the new JAR file from GitHub.
     *
     * @return Success
     */
    private boolean downloadFile() {
        ZLogger.log("Downloading new version from " + latestRelease.downloadURL + "...");
        try {
            if (latestRelease == null) latestRelease = fetchLatestRelease();
            if (latestRelease == null) return false;
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(latestRelease.downloadURL).openConnection());
            int fileSize = httpConnection.getContentLength();
            BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(DIRECTORY + TEMP_FILE_NAME)));
            byte[] data = new byte[BYTE_BUFFER_SIZE];
            int totalBytesRead = 0;
            int numBytesRead;
            int currentProgressPercent = 0;
            while ((numBytesRead = inputStream.read(data, 0, BYTE_BUFFER_SIZE)) >= 0) {
                outputStream.write(data, 0, numBytesRead);
                totalBytesRead += numBytesRead;
                int newProgressPercent = Math.round((float) totalBytesRead / fileSize * 100);
                if (newProgressPercent != currentProgressPercent) {
                    currentProgressPercent = newProgressPercent;
                    for (IUpdateProgressListener listener : progressListeners) {
                        int finalCurrentProgressPercent = currentProgressPercent;
                        SwingUtilities.invokeLater(() -> listener.onDownloadProgress(finalCurrentProgressPercent));
                    }
                }
            }
            inputStream.close();
            outputStream.close();
            for (IUpdateProgressListener listener : progressListeners) {
                SwingUtilities.invokeLater(listener::onDownloadComplete);
            }
            return true;
        } catch (IOException e) {
            ZLogger.log("Error while downloading file!");
            ZLogger.log(e.getStackTrace());
            for (IUpdateProgressListener listener : progressListeners)
                SwingUtilities.invokeLater(listener::onDownloadFailed);
            return false;
        }
    }

    /**
     * Copies the new JAR from the working directory to the user's original directory.
     */
    private void patch() {
        ZLogger.log("Copying file...");
        ZLogger.log("Target: " + DIRECTORY + TEMP_FILE_NAME);
        ZLogger.log("Destination: " + launchPath);
        Exception exception = null;
        for (int i = 1; i <= MAX_ACTION_ATTEMPTS; i++) {
            try {
                Files.copy(Paths.get(DIRECTORY + TEMP_FILE_NAME), Paths.get(launchPath), StandardCopyOption.REPLACE_EXISTING);
                ZLogger.log("File copied successfully.");
                return;
            } catch (IOException e) {
                ZLogger.log("Failed to copy file, retrying...");
                exception = e;
                try {
                    Thread.sleep(ACTION_RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        ZLogger.log("Failed to copy file!");
        ZLogger.log(exception.getStackTrace());
    }

    /**
     * Deletes the temporary JAR file used for patching.
     */
    private void clean() {
        ZLogger.log("Cleaning...");
        Path tempFilePath = Paths.get(DIRECTORY + TEMP_FILE_NAME);
        Exception exception = null;
        for (int i = 1; i <= MAX_ACTION_ATTEMPTS; i++) {
            try {
                Files.delete(tempFilePath);
                ZLogger.log("Deleted temporary file: " + DIRECTORY + TEMP_FILE_NAME);
                return;
            } catch (IOException e) {
                ZLogger.log("Failed to delete file, retrying...");
                exception = e;
                try {
                    Thread.sleep(ACTION_RETRY_DELAY_MS);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        ZLogger.log("Failed to delete file: " + DIRECTORY + TEMP_FILE_NAME);
        ZLogger.log(exception.getStackTrace());
    }

}
