package com.tiwence.cinenow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by temarill on 15/01/2015.
 */
public class MovieTheater implements Serializable {

    public String mId;
    public String mName;
    public String mAddress;
    public HashMap<String, ArrayList<String>> mShowtimesMap;
    public HashMap<String, ArrayList<Integer>> mShowtimesRemainingMap;
    public ArrayList<ShowTime> mShowTimes;

    //public ArrayList<Movie> mMovies;

    //TODO gérer les séances

}
