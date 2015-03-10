package com.tiwence.cinenow.adapter;

import android.location.Location;
import android.os.AsyncTask;
import android.widget.TextView;

import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;

import java.lang.ref.WeakReference;

/**
 * Created by temarill on 09/03/2015.
 */
public class TheaterDistanceHelper extends AsyncTask<MovieTheater, Void, MovieTheater> {

    private Location mLocation;
    WeakReference<TextView> mDistanceRef;
    private boolean mHasToDisplayName;

    public TheaterDistanceHelper(Location location, ShowTimesFeed result,  WeakReference<TextView> distanceRef, boolean hasToDisplayName) {
        this.mLocation = location;
        this.mDistanceRef = distanceRef;
        this.mHasToDisplayName = hasToDisplayName;
    }

    @Override
    protected MovieTheater doInBackground(MovieTheater... params) {
        params[0] = ApiUtils.instance().geocodeSpecificTheater(params[0], mLocation);
        return params[0];
    }

    @Override
    protected void onPostExecute(MovieTheater theater) {
        super.onPostExecute(theater);
        if (mDistanceRef != null && mDistanceRef.get() != null) {
            if (theater.mDistance < 1000) {
                if (mHasToDisplayName) {
                    mDistanceRef.get().setText(theater.mName + " (" + theater.mDistance + " km)");
                } else {
                    mDistanceRef.get().setText(theater.mDistance + " km");
                }
            } else {
                if (mHasToDisplayName) {
                    mDistanceRef.get().setText(theater.mName);
                }
            }
        }
    }
}
