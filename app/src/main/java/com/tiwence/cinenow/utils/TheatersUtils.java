package com.tiwence.cinenow.utils;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.tiwence.cinenow.model.MovieTheater;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            Log.d("Adding theaters", "start");
            InputStream inStr = context.getResources().getAssets().open("theaters.json");
            mTheatersArr = new JSONArray(HttpUtils.getStringFromInputStream(inStr));
            mTheatersLocations = new LinkedHashMap<>();
            for (int i = 0; i < mTheatersArr.length(); i++) {
                JSONObject theater = mTheatersArr.optJSONObject(i);
                Location l = new Location("");
                l.setLatitude(theater.optDouble("lat"));
                l.setLongitude(theater.optDouble("lng"));
                mTheatersLocations.put(theater.optString("Google"), l);
            }
            Log.d("Adding theaters", "stop");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
}
