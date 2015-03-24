package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.Movie;

import java.util.LinkedHashMap;

/**
 * Created by temarill on 16/01/2015.
 */
public interface OnRetrieveMoviesInfoCompleted {
    public void onProgressMovieInfoCompleted(Movie movie);
    public void onRetrieveMoviesInfoCompleted(LinkedHashMap<String, Movie> movies);
    public void onRetrieveMoviesError(String message);
}
