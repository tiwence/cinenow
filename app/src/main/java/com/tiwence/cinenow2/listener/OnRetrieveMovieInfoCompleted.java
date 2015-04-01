package com.tiwence.cinenow2.listener;

import com.tiwence.cinenow2.model.Movie;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveMovieInfoCompleted {
    public void onRetrieveMovieInfoCompleted(Movie movie);
    public void onRetrieveMovieError(String message);
}
