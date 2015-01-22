package com.tiwence.cinenow.utils;

import com.tiwence.cinenow.model.TheaterResult;

/**
 * Created by temarill on 16/01/2015.
 */
public interface OnRetrieveTheatersCompleted {
    void onRetrieveTheatersCompleted(TheaterResult result);
    void onRetrieveTheatersError(String errorMessage);
}
