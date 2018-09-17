package com.ira.kngwato.utils;

import java.util.Date;

public final class Logger {
    public static final int INFO = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int MESSAGE = 3;

    public static void log(int logLevel, String message) {
        String level;
        if (logLevel == INFO) {
            level = "[ INFO ]\t\t";
        }else if (logLevel == ERROR) {
            level = "[ ERROR ]\t\t";
        }else if (logLevel == WARNING) {
            level = "[ WARNING ]\t";
        }else {
            level = "[ MESSAGE ]\t";
        }

        String date = (logLevel != MESSAGE) ? (new Date()).toString() : "";
        String msg = level + date + "\t" + message;
        System.out.println(msg + "\n");
    }
}
