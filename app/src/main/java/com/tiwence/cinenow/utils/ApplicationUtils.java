package com.tiwence.cinenow.utils;

import android.content.Context;

import com.tiwence.cinenow.model.Movie;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by temarill on 21/01/2015.
 */
public class ApplicationUtils {

    public static final String MOVIES_FILE_NAME = "movies";
    public static final String THEATERS_FILE_NAME = "theaters";

    /**
     * @param context
     * @param data
     */
    public static void saveDataInCache(Context context, Object data, String fileName) {

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (oos != null) oos.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param context
     * @return
     */
    public static Object getDataInCache(Context context, String fileName) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = context.openFileInput(fileName);
            ois = new ObjectInputStream(fis);
            return ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param showTime
     * @return
     */
    public static int getTimeRemaining(String showTime) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        Date now = c.getTime();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String dateShowRootStr = format.format(now);
        dateShowRootStr += " " + showTime;

        format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date showTimeDate = null;
        try {
            showTimeDate = format.parse(dateShowRootStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long timeRemain = showTimeDate.getTime() - now.getTime();

        return (int) ((timeRemain / 1000) / 60);
    }


    public static String getYear() {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        Date now = c.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        return format.format(now);
    }

    public static String getTimeString(int timeRemaining) {
        String time = "Dans ";

        int hour = timeRemaining / 60;
        int minute = timeRemaining;
        if (hour > 0) {
            time += hour + "h";
            minute = minute - 60*hour;
        }
        time += minute + "min";

        return time;
    }
}
