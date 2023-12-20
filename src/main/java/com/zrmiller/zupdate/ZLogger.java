package com.zrmiller.zupdate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * A simple logging implementation that works through program starts.
 */
public class ZLogger {

    private static BufferedWriter writer;
    private static String directory;
    private static final String filePrefix = "Log";
    private static final String folder = "logs/";
    private static String logFile;
    private static final SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    private static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final String logPrefix = "logfile:";

    public static void open(String directory, String[] args) {
        ZLogger.directory = directory;
        File file = new File(directory + folder);
        if (!file.exists())
            if (!file.mkdirs())
                return;
        boolean newFile = false;
        for (String arg : args) {
            if (arg.startsWith(logPrefix)) {
                logFile = arg.replaceFirst(logPrefix, "");
                break;
            }
        }
        if (logFile == null) {
            String time = fileFormatter.format(System.currentTimeMillis());
            time = time.replaceFirst(":", "h").replaceFirst(":", "m") + "s";
            logFile = directory + folder + filePrefix + "_" + time + ".txt";
//            String[] tempArgs = args;
//            args = new String[tempArgs.length + 1];
//            System.arraycopy(tempArgs, 0, args, 0, tempArgs.length);
//            args[args.length - 1] = logPrefix + logFile;
            newFile = true;
        }
        try {
            if (newFile)
                writer = new BufferedWriter(new FileWriter(logFile));
            else
                writer = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void err(String message){
        log(message, true);
    }

    public static void log(String message) {
        log(message, false);
    }

    public static void log(String message, boolean error) {
        try {
            String time = timestampFormatter.format(System.currentTimeMillis());
            time = time + " | ";
            writer.write(time + message + "\n");
            if (error) System.err.println(message);
            else System.out.println(message);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String[] messages) {
        for (String message : messages)
            log(message);
    }

    public static void log(StackTraceElement[] elements) {
        for (StackTraceElement element : elements)
            log(element.toString());
    }

    public static String getLaunchArg() {
        return logPrefix + logFile;
    }

    public static void close() {
        if (writer == null) return;
        try {
            log("Program Closed.");
            writer.write("\n");
            writer.close();
            writer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
