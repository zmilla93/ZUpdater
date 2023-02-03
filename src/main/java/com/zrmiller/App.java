package com.zrmiller;

import com.zrmiller.saving.SaveFile;
import com.zrmiller.zupdate.MainFrame;
import com.zrmiller.zupdate.UpdateManager;
import com.zrmiller.zupdate.UpdateSaveFile;
import com.zrmiller.zupdate.ZLogger;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {

    private static final String directory = "D:/ZUpdate/";
    public static String APP_NAME;
    public static String APP_VERSION;
    public static String APP_URL;

    public static SaveFile<UpdateSaveFile> updateSaveFile = new SaveFile<>(directory + "update.json", UpdateSaveFile.class);
    private static int version = 1;

    public static void main(String[] args) {
        ZLogger.open(directory, args);
        ZLogger.log("Program Launched: " + Arrays.toString(args));
        UpdateManager updateManager = new UpdateManager("zmilla93", "ZUpdater", directory, updateSaveFile);
        updateManager.continueUpdateProcess(args);
        if (updateManager.isUpdateAvailable()) {
            // TODO : Add GUI Here
            updateManager.runUpdateProcess();


        }

        final String[] finalArgs = args;
        try {
            SwingUtilities.invokeAndWait(() -> {
                MainFrame mainFrame = new MainFrame(finalArgs, updateManager.APP_VERSION);
                mainFrame.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

        ZLogger.log("Program Launched!");

        Runtime.getRuntime().addShutdownHook(new Thread(ZLogger::close));

    }

    public static String getAppVersion() {
        if (APP_VERSION == null) {
            final Properties properties = new Properties();
            try {
                InputStream stream = App.class.getClassLoader().getResourceAsStream("project.properties");
                properties.load(stream);
                assert stream != null;
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            APP_NAME = properties.getProperty("name");
            APP_VERSION = "v" + properties.getProperty("version");
            APP_URL = "v" + properties.getProperty("url");
        }
        return APP_VERSION;
    }

}
