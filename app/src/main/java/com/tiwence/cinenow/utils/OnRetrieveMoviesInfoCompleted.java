package com.tiwence.cinenow.utils;

import com.tiwence.cinenow.model.Movie;

import java.util.HashMap;

/**
 * Created by temarill on 16/01/2015.
 */
public interface OnRetrieveMoviesInfoCompleted {
    public void onProgressMovieInfoCompleted(Movie movie);
    public void onRetrieveMoviesInfoCompleted(HashMap<String, Movie> movies);
    public void onRetrieveMoviesError(String message);
}
