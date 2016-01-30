package com.gz.mymqtt;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Calendar;

/**
 * inner log
 * for closing the log out
 * and choice to log on txt
 * Created by host on 2016/1/23.
 */
public class Log {
    public static final boolean PRINT = false;

    public static final boolean ERROR_ONLY = true;

    public static void i(String tag, String info) {
        if (!PRINT)
            return;
        android.util.Log.i(tag, info);
        writeInFile(tag, info);
    }

    public static void e(String tag, String info) {
        if (!PRINT && !ERROR_ONLY)
            return;
        android.util.Log.e(tag, info);
        writeInFile(tag, info);
    }

    public static void d(String tag, String info) {
        if (!PRINT)
            return;
        android.util.Log.d(tag, info);
        writeInFile(tag, info);
    }

    public static void getStackTraceString(Throwable tr) {
        if (!PRINT && !ERROR_ONLY)
            return;
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return;
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        writeInFile("exception", sw.toString());
    }

    private static void writeInFile(String tag, String info) {
        Calendar calendar = Calendar.getInstance();
        String year = calendar.get(Calendar.YEAR) + "-";
        String month = calendar.get(Calendar.MONTH) + 1 + "-";
        String day = calendar.get(Calendar.DAY_OF_MONTH) + "-";
        String hour = calendar.get(Calendar.HOUR_OF_DAY) + ":";
        String minute = calendar.get(Calendar.MINUTE) + ":";
        int second = calendar.get(Calendar.SECOND);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(year).append(month).append(day)
                .append(hour).append(minute).append(second)
                .append("----").append(tag).append(":").append(info).append("\n");
        File sdDir;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED); //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
            String fileName = sdDir.getAbsolutePath() + "/MyMqtt.txt";
            File file = new File(fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                fileOutputStream.write(stringBuilder.toString().getBytes());
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
