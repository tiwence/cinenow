package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 18/02/2015.
 */
public interface OnRetrieveTheaterInfoCompleted {
    void onRetrieveTheaterCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset);
    void onRetrieveTheaterError(String errorMessage);
}
