package com.tiwence.cinenow2.listener;

import com.tiwence.cinenow2.model.MovieTheater;
import com.tiwence.cinenow2.model.ShowTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 20/02/2015.
 */
public interface OnRetrieveMovieShowTimesCompleted {
    void onRetrieveMovieShowTimesCompleted(LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset);
    void onRetrieveMovieShowTimesError(String errorMessage);
}

