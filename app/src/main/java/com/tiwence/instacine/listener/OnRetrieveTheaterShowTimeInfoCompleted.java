package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.Movie;
import com.tiwence.instacine.model.ShowTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 18/02/2015.
 */
public interface OnRetrieveTheaterShowTimeInfoCompleted {
    void onRetrieveTheaterShowTimeInfoCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset);
    void onRetrieveTheaterShowTimeInfoError(String errorMessage);
}
