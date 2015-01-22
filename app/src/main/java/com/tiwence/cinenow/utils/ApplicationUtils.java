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
import java.util.TimeZone;

/**
 * Created by temarill on 21/01/2015.
 */
public class ApplicationUtils {

    private static final String MOVIES_FILE_NAME = "movies";

    /**
     *
     * @param context
     * @param movies
     */
    public static void saveMoviesInCache(Context context, HashMap<String, Movie> movies) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(MOVIES_FILE_NAME, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(movies);
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
     *
     * @param context
     * @return
     */
    public static HashMap<String, Movie> getMoviesInCache(Context context) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        HashMap<String, Movie> movies = null;
        try {
            fis = context.openFileInput(MOVIES_FILE_NAME);
            ois = new ObjectInputStream(fis);
            movies = (HashMap<String, Movie>) ois.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return movies;
    }

    /**
     *
     * @param showTime
     * @return
     */
    public  static int getTimeRemaining(String showTime) {
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

        return (int)((timeRemain / 1000) / 60);
    }


}
