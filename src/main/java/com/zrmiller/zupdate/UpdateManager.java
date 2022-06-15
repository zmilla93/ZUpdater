package com.zrmiller.zupdate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private final String AUTHOR;
    private final String REPO;
    private final String DIRECTORY;

    private final String LATEST_VERSION_URL;
    private String downloadURL;
    private int fileSize;
    private int bytesProcessed;
    private static final int BYTE_BUFFER_SIZE = 1024 * 4;

    private final SaveFile<UpdateSaveFile> saveFile;
    private DownloadVersion latestVersion;
    private String launchPath;
    private String jarName;

    public String APP_VERSION;
    public String APP_NAME;
    public String APP_URL;

    private static final String JAR_PREFIX = "jar:";
    private static final String LAUNCH_PATH_PREFIX = "launcher:";

    /**
     * Directory will contain update.json and store temp files.
     *
     * @param author
     * @param repo
     * @param directory
     */
    public UpdateManager(String author, String repo, String directory, SaveFile<UpdateSaveFile> saveFile) {
        this.AUTHOR = author;
        this.REPO = repo;
        this.DIRECTORY = directory;
        this.saveFile = saveFile;
        LATEST_VERSION_URL = "https://api.github.com/repos/" + AUTHOR + "/" + REPO + "/releases/latest";
        readAppData();
    }

    /**
     * Begins the entire update process.
     */
    public void runUpdateProcess() {
        System.out.println("Running update process...");
        if (!isUpdateAvailable()) return;
        System.out.println("JARNAME:" + latestVersion.FILE_NAME);
        String[] args = new String[]{UpdateCommand.DOWNLOAD.toString(), JAR_PREFIX + latestVersion.FILE_NAME, LAUNCH_PATH_PREFIX + getLaunchPath()};
        continueUpdateProcess(args);
    }

    /**
     * Continues the update process if necessary. This should be called before anything else.
     * Used the command line args to pass information between runs.
     *
     * @param args The command line arguments of the program.
     */
    public void continueUpdateProcess(String[] args) {
        System.out.println("CONTINUE PROCESS : " + Arrays.toString(args));
        boolean download = false;
        boolean copy = false;
        boolean clean = false;
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
            downloadFile();
            runProcess(DIRECTORY + jarName, UpdateCommand.PATCH, launchArgs);
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
        String currentVersion = APP_VERSION;
        if (currentVersion == null)
            return true;
        if (latestVersion == null || forceCheck) {
            latestVersion = fetchLatestVersion();
            if (latestVersion == null)
                return false;
        }
        return !currentVersion.equals(latestVersion.TAG);
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
            ZLogger.log("Running '" + updateCommand + "' process... "  + Arrays.toString(args.toArray()));
            ZLogger.close();
            builder.start();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readAppData() {
        Properties properties = new Properties();
        try {
            InputStream stream = new BufferedInputStream(Objects.requireNonNull(
                    App.class.getClassLoader().getResourceAsStream("project.properties")));
            properties.load(stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        APP_NAME = properties.getProperty("name");
        APP_VERSION = "v" + properties.getProperty("version");
        APP_URL = properties.getProperty("url");
    }

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
            e.printStackTrace();
            return null;
        }
    }

    public void writeTag(String tag) {
        saveFile.data.tag = tag;
        saveFile.saveToDisk();
    }

    private boolean validateDirectory() {
        File file = new File(DIRECTORY);
        if (file.exists())
            return file.isDirectory();
        return file.mkdirs();
    }

    public DownloadVersion getLatestVersion() {
        return latestVersion;
    }

    public String getLaunchPath() {
        try {
            String path = UpdateManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            if (path.startsWith("/"))
                path = path.replaceFirst("/", "");
            return path;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Updating

    /**
     * Downloads the new .jar file from GitHub.
     *
     * @return Success
     */
    private boolean downloadFile() {
        ZLogger.log("Downloading new version...");
        try {
            if (latestVersion == null)
                fetchLatestVersion();
            if (latestVersion == null)
                return false;
            ZLogger.log("File Name: " + latestVersion.FILE_NAME + "...");
            ZLogger.log("Version: " + latestVersion.TAG);
            ZLogger.log("URL: " + latestVersion.DOWNLOAD_URL);
            HttpURLConnection httpConnection = (HttpURLConnection) (new URL(latestVersion.DOWNLOAD_URL).openConnection());
            fileSize = httpConnection.getContentLength();
            BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(DIRECTORY + latestVersion.FILE_NAME));
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

    /**
     * Copies the new jar from the temp directory to the user's original directory.
     *
     * @return Success
     */
    private boolean copy() {
        ZLogger.log("Copying file...");
        ZLogger.log("Target: " + DIRECTORY + jarName);
        ZLogger.log("Destination: " + launchPath);
        try {
            Files.copy(Paths.get(DIRECTORY + jarName), Paths.get(launchPath), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clean() {
        // TODO : This
        ZLogger.log("Cleaning...");
    }

}
