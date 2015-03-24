package com.tiwence.instacine.listener;


import com.tiwence.instacine.model.ShowTimesFeed;

/**
 * Created by temarill on 26/01/2015.
 */
public interface OnRetrieveShowTimesCompleted {
    void onRetrieveShowTimesCompleted(ShowTimesFeed result);
    void onRetrieveShowTimesError(String errorMessage);
}
