package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 20/02/2015.
 */
public interface OnRetrieveMovieShowTimesCompleted {
    void onRetrieveMovieShowTimesCompleted(LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset);
    void onRetrieveMovieShowTimesError(String errorMessage);
}

