package com.tiwence.cinenow2.utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;

import com.tiwence.cinenow2.R;

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
import java.util.TimeZone;

/**
 * Created by temarill on 21/01/2015.
 */
public class ApplicationUtils {

    public static final String MOVIES_FILE_NAME = "movies";
    public static final String THEATERS_FILE_NAME = "theaters";
    public static final String FAVORITES_MOVIES_FILE_NAME = "favorites_movies";
    public static final String SHOWTIMES_FEED_FILE_NAME = "showtimes_feed";
    public static final String FAVORITES_THEATERS_FILE_NAME = "favorites_theaters" ;
    //public static final String PLAYSTORE_URL = "https://play.google.com/apps/testing/com.tiwence.cinenow2";
    public static final String PLAYSTORE_URL = "https://play.google.com/store/apps/details?id=com.tiwence.cinenow2";
    public static final String MOVIE_DB_API_DOC = "https://www.themoviedb.org/documentation/api";

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

    public static final int ANIM_FADEIN = 300;
    public static void fadeView(View view, boolean fadeIn) {
        ObjectAnimator animAlpha = null;
        if (fadeIn) {
            animAlpha = ObjectAnimator.ofFloat(view, "alpha", 1.0f);
        } else {
            animAlpha = ObjectAnimator.ofFloat(view, "alpha", 0.0f);
        }
        animAlpha.setDuration(ANIM_FADEIN);
        animAlpha.start();
    }


    public static String getYear() {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        Date now = c.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy");
        return format.format(now);
    }

    public static String getTimeString(Context context, int timeRemaining) {
        String time = context.getString(R.string.in) + " ";

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
