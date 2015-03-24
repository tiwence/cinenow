package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.MovieTheater;
import com.tiwence.instacine.model.ShowTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 20/02/2015.
 */
public interface OnRetrieveMovieShowTimesCompleted {
    void onRetrieveMovieShowTimesCompleted(LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset);
    void onRetrieveMovieShowTimesError(String errorMessage);
}

