package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveQueryCompleted {
    public void onRetrieveQueryMovieCompleted(Movie movie);
    public void onRetrieveQueryTheaterCompleted(MovieTheater theater);
    public void onRetrieveQueryError(String errorMessage);

}
