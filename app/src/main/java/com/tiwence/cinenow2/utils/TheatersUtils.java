package com.tiwence.cinenow2.utils;

import android.content.Context;
import android.location.Location;

import com.tiwence.cinenow2.model.MovieTheater;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 09/03/2015.
 */
public class TheatersUtils {

    private static TheatersUtils theatersUtils;
    private static JSONArray mTheatersArr;
    private static LinkedHashMap<String, Location> mTheatersLocations;

    public static TheatersUtils instance() {
        if (theatersUtils == null) {
            theatersUtils = new TheatersUtils();
        }
        return  theatersUtils;
    }

    public static void loadTheatersLocation(Context context) {
        try {
            String theaterLocationFile = loadTheatersLocationForApplication(context);
            boolean hasToBeSaved = false;
            if (theaterLocationFile == null) {
                hasToBeSaved = true;
                InputStream inStr = context.getResources().getAssets().open("theaters.json");
                theaterLocationFile = HttpUtils.getStringFromInputStream(inStr);
            }
            mTheatersArr = new JSONArray(theaterLocationFile);
            if (hasToBeSaved)
                writeJSONTheaterLocationsFile(context);
            mTheatersLocations = new LinkedHashMap<>();
            for (int i = 0; i < mTheatersArr.length(); i++) {
                JSONObject theater = mTheatersArr.optJSONObject(i);
                Location l = new Location("");
                l.setLatitude(theater.optDouble("lat"));
                l.setLongitude(theater.optDouble("lng"));
                mTheatersLocations.put(theater.optString("Google"), l);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String loadTheatersLocationForApplication(Context context) {
        try {
            FileInputStream fis = context.openFileInput("theaters_saved.json");
            return HttpUtils.getStringFromInputStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MovieTheater getTheaterDistance(MovieTheater movieTheater, Location userLocation) {
        Location dest = mTheatersLocations.get(movieTheater.mName);
        if (dest != null) {
            double distance = userLocation.distanceTo(dest) / 1000;
            distance = (double)Math.round(distance * 100) / 100;
            movieTheater.mDistance = distance;
            movieTheater.mLatitude = dest.getLatitude();
            movieTheater.mLongitude = dest.getLongitude();
            return movieTheater;
        } else {
            movieTheater.mDistance = -1.0;
            return movieTheater;
        }
    }

    public void putTheaterDistance(MovieTheater movieTheater) {
        JSONObject theaterJSON = new JSONObject();
        try {
            theaterJSON.putOpt("Google", movieTheater.mName);
            theaterJSON.putOpt("adrcommune", movieTheater.mAddress);
            theaterJSON.putOpt("adr", movieTheater.mAddress);
            theaterJSON.putOpt("lat", movieTheater.mLatitude);
            theaterJSON.putOpt("lng", movieTheater.mLongitude);
            mTheatersArr.put(theaterJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static  void writeJSONTheaterLocationsFile(Context context) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput("theater_locations.json", Context.MODE_PRIVATE);
            fos.write(mTheatersArr.toString().getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
