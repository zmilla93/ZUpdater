package com.zrmiller;

import com.zrmiller.gui.MainFrame;
import com.zrmiller.gui.ProgressFrame;
import com.zrmiller.saving.SaveFile;
import com.zrmiller.zupdate.UpdateManager;
import com.zrmiller.zupdate.UpdateSaveFile;
import com.zrmiller.zupdate.ZLogger;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {

    private static final String directory = "D:/ZUpdate/";
    public static String APP_NAME;
    public static String APP_VERSION;
    public static String APP_URL;
    public static ProgressFrame progressFrame;

    public static SaveFile<UpdateSaveFile> updateSaveFile = new SaveFile<>(directory + "update.json", UpdateSaveFile.class);

    public static void main(String[] args) {
        ZLogger.open(directory, args);
        ZLogger.log("Program Launched: " + Arrays.toString(args));
        readAppData();

        // TEMP PROGRESS BAR:
        try {
            SwingUtilities.invokeAndWait(() -> progressFrame = new ProgressFrame());
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // FIXME : Copying files sometimes fails, not sure why
        UpdateManager updateManager = null;
        updateManager = runUpdateProcess(args);
        String version = APP_VERSION;
        System.out.println(version);

        final String[] finalArgs = args;
        try {
            SwingUtilities.invokeAndWait(() -> {
                MainFrame mainFrame = new MainFrame(finalArgs, version);
                mainFrame.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        ZLogger.log("Program Launched!");

        Runtime.getRuntime().addShutdownHook(new Thread(ZLogger::close));

    }

    public static UpdateManager runUpdateProcess(String[] args) {
        UpdateManager updateManager = new UpdateManager("zmilla93", "ZUpdater", directory, updateSaveFile);
        updateManager.continueUpdateProcess(args);
        if (updateManager.isUpdateAvailable()) {
            updateManager.addProgressListener(progressFrame);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    progressFrame.setVisible(true);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            updateManager.runUpdateProcess();
        }
        return updateManager;
    }

//    public static String getAppVersion() {
//        if (APP_VERSION == null) {
//            final Properties properties = new Properties();
//            try {
//                InputStream stream = App.class.getClassLoader().getResourceAsStream("project.properties");
//                properties.load(stream);
//                assert stream != null;
//                stream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            APP_NAME = properties.getProperty("name");
//            APP_VERSION = "v" + properties.getProperty("version");
//            APP_URL = "v" + properties.getProperty("url");
//        }
//        return APP_VERSION;
//    }

    private static void readAppData() {
        Properties properties = new Properties();
        try {
            InputStream stream = new BufferedInputStream(Objects.requireNonNull(
                    UpdateManager.class.getClassLoader().getResourceAsStream("project.properties")));
            properties.load(stream);
            stream.close();
        } catch (IOException e) {
            System.err.println("Properties not found! Create a 'project.properties' file in the resources folder, then add the lines 'version=${project.version}' and 'artifactId=${project.artifactId}'.");
            return;
        }
        APP_NAME = properties.getProperty("name");
        APP_VERSION = "v" + properties.getProperty("version");
        APP_URL = properties.getProperty("url");
    }

}
