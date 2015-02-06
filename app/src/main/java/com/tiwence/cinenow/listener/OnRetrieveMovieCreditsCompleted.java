package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;

/**
 * Created by temarill on 06/02/2015.
 */
public interface OnRetrieveMovieCreditsCompleted {
    public void onRetrieveMovieCreditsCompleted(Movie movie);
    public void onRetrieveMovieCreditsError(String message);
}
