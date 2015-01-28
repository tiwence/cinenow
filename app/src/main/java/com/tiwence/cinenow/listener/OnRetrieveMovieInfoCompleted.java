package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveMovieInfoCompleted {
    public void onRetrieveMovieInfoCompleted(Movie movie);
    public void onRetrieveMovieError(String message);
}
