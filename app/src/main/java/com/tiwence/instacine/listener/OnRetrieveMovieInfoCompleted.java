package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.Movie;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveMovieInfoCompleted {
    public void onRetrieveMovieInfoCompleted(Movie movie);
    public void onRetrieveMovieError(String message);
}
