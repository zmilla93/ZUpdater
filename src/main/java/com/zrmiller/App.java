package com.zrmiller;

import com.zrmiller.saving.SaveFile;
import com.zrmiller.zupdate.DownloadVersion;
import com.zrmiller.zupdate.MainFrame;
import com.zrmiller.zupdate.UpdateManager;
import com.zrmiller.zupdate.UpdateSaveFile;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 */
public class App {

    private static String directory = "D:\\ZUpdate\\";

    public static SaveFile<UpdateSaveFile> updateSaveFile = new SaveFile<>(directory + "update.json", UpdateSaveFile.class);

    public static void main(String[] args) {

        UpdateManager updateManager = new UpdateManager("zmilla93", "slimtrade", "D:\\ZUpdate\\", updateSaveFile);
        System.out.println("Launched:" + updateManager.getLaunchPath());
        updateManager.continueUpdateProcess(args);
//        DownloadVersion version = updateManager.fetchLatestVersion();
//        updateManager.downloadFile(version);


        try {
            SwingUtilities.invokeAndWait(() -> {
                MainFrame mainFrame = new MainFrame(args);
                mainFrame.setVisible(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }

//        updateManager.d

//        System.out.println(System.getProperty("user.dir"));
//
//        UpdateManager updateManager = new UpdateManager("zmilla93", "slimtrade", "D:\\ZUpdate\\", updateSaveFile);
//        String classPath = System.getProperty("java.class.path");
//        System.out.println("CP" + classPath);
//        System.out.println("V:" + updateManager.readCurrentVersion());
//        updateManager.writeTag("ASDFASDF");
//        System.out.println("V:" + updateManager.readCurrentVersion());
        return;
    }
}
