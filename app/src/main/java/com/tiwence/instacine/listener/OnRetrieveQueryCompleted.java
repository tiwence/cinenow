package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.ShowTimesFeed;

import java.util.List;

/**
 * Created by temarill on 22/01/2015.
 */
public interface OnRetrieveQueryCompleted {
    public void onRetrieveQueryDataset(ShowTimesFeed stf);
    public void onRetrieveQueryCompleted(List<Object> dataset);
    public void onRetrieveQueryError(String errorMessage);
}