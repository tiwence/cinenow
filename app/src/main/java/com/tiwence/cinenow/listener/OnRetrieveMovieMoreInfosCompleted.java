package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;

/**
 * Created by temarill on 04/02/2015.
 */
public interface OnRetrieveMovieMoreInfosCompleted {
    public void onRetrieveMovieMoreInfosCompleted(Movie movie);
    public void onRetrieveMovieMoreInfosError(String errorMessage);
}
