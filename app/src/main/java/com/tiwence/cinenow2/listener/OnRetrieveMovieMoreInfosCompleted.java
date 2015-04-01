package com.tiwence.cinenow2.listener;

import com.tiwence.cinenow2.model.Movie;

/**
 * Created by temarill on 04/02/2015.
 */
public interface OnRetrieveMovieMoreInfosCompleted {
    public void onRetrieveMovieMoreInfosCompleted(Movie movie);
    public void onRetrieveMovieMoreInfosError(String errorMessage);
}
