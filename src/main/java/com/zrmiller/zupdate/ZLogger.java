package com.zrmiller.zupdate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * A simple logging implementation that works through program starts.
 */
// FIXME : Add support for legacy file format?
public class ZLogger {

    private static BufferedWriter writer;
    private static String logFile;
    private static String DIRECTORY;
    private static final String FILE_PREFIX = "log_";
    private static final String SUBFOLDER = "logs";
    private static final String logPrefix = "logfile:";

    private static final int MAX_LOG_FILES = 5;
    private static boolean VALID_DIRECTORY;

    // Converts System.currentTime() to a timestamp usable in a file name
    private static final SimpleDateFormat fileFormatter = new SimpleDateFormat("yyyy-MM-dd_HH'h'mm'm'ss's'");
    // Converts System.currentTime() to a timestamp usable during logging
    private static final SimpleDateFormat timestampFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    // Regex for file matching
    private static final String regString = FILE_PREFIX + "(?<year>\\d+)-(?<month>\\d+)-(?<day>\\d+)_(?<hour>\\d+)h(?<minute>\\d+)m(?<second>\\d+)s\\.txt";

    public static void open(String appDirectory, String[] args) {
        DIRECTORY = appDirectory + SUBFOLDER;
        System.out.println(DIRECTORY);
        VALID_DIRECTORY = validateDirectory(DIRECTORY);
        if (!VALID_DIRECTORY) {
            System.err.println("Failed to validate logging directory: " + DIRECTORY);
            return;
        }
        boolean newFile = false;
        for (String arg : args) {
            if (arg.startsWith(logPrefix)) {
                logFile = arg.replaceFirst(logPrefix, "");
                break;
            }
        }
        if (logFile == null) {
            String time = fileFormatter.format(System.currentTimeMillis());
            logFile = DIRECTORY + File.separator + FILE_PREFIX + time + ".txt";
            newFile = true;
        }
        try {
            if (newFile) {
                writer = new BufferedWriter(new FileWriter(logFile));
                cleanOldFiles();
            } else writer = new BufferedWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void err(String message) {
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

    private static void cleanOldFiles() {
        File dir = new File(DIRECTORY);
        File[] files = dir.listFiles();
        ArrayList<Date> dates = new ArrayList<>();
        if (files == null) return;
        // Convert file names to sortable dates
        for (File file : files) {
            if (!file.getName().matches(regString)) continue;
            Date date;
            try {
                date = timestampFormatter.parse(fileNameToTimestamp(file.getName()));
                dates.add(date);
            } catch (ParseException ignore) {
            }
        }
        final int MAX_FILE_DELETIONS = 20;
        int attempts = 0;
        // Delete old log files
        while (dates.size() > MAX_LOG_FILES && attempts < MAX_FILE_DELETIONS) {
            Date oldestDate = Collections.min(dates);
            String fileTimestamp = fileFormatter.format(oldestDate);
            String path = DIRECTORY + File.separator + FILE_PREFIX + fileTimestamp + ".txt";
            File file = new File(path);
            if (file.delete()) log("Deleted old log file: " + path);
            else log("Failed to delete file: " + path);
            dates.remove(oldestDate);
            attempts++;
        }
    }

    // FIXME : Move to Utility?
    private static boolean validateDirectory(String directory) {
        File file = new File(directory);
        if (file.exists()) return file.isDirectory();
        return file.mkdirs();
    }

    private static String fileNameToTimestamp(String fileName) {
        return fileName.replaceFirst(FILE_PREFIX, "")
                .replaceFirst("_", " ")
                .replaceAll("-", "/")
                .replaceAll("[hm]", ":")
                .replaceAll("s", "")
                .replaceAll("\\.txt", "");
    }

}
