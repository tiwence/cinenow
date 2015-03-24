package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.Movie;

/**
 * Created by temarill on 04/02/2015.
 */
public interface OnRetrieveMovieMoreInfosCompleted {
    public void onRetrieveMovieMoreInfosCompleted(Movie movie);
    public void onRetrieveMovieMoreInfosError(String errorMessage);
}
