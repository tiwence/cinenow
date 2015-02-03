package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTimesFeed;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveQueryCompleted {
    public void onRetrieveQueryDataset(ShowTimesFeed stf);
    public void onRetrieveQueryMovieCompleted(Movie movie);
    public void onRetrieveQueryTheaterCompleted(MovieTheater theater);
    public void onRetrieveQueryError(String errorMessage);
}
