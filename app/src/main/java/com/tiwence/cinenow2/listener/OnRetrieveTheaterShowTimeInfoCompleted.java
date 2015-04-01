package com.tiwence.cinenow2.listener;

import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.ShowTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 18/02/2015.
 */
public interface OnRetrieveTheaterShowTimeInfoCompleted {
    void onRetrieveTheaterShowTimeInfoCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset);
    void onRetrieveTheaterShowTimeInfoError(String errorMessage);
}
