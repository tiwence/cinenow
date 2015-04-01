package com.tiwence.cinenow2.listener;


import com.tiwence.cinenow2.model.ShowTimesFeed;

/**
 * Created by temarill on 26/01/2015.
 */
public interface OnRetrieveShowTimesCompleted {
    void onRetrieveShowTimesCompleted(ShowTimesFeed result);
    void onRetrieveShowTimesError(String errorMessage);
}
