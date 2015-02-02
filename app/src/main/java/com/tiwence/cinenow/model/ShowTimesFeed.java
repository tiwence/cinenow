package com.tiwence.cinenow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 26/01/2015.
 */
public class ShowTimesFeed implements Serializable {

    public LinkedHashMap<String, MovieTheater> mTheaters;
    public LinkedHashMap<String, Movie> mMovies;
    public ArrayList<Movie> mNextMovies;
    public ArrayList<ShowTime> mNextShowTimes;
    public ArrayList<ShowTime> mShowTimes;
    public ArrayList<String> mMovieKinds;

    public ArrayList<ShowTime> getNextShowtimesByMovieId(String movieId) {
        ArrayList<ShowTime> showTimes = null;
        for (ShowTime st : mNextShowTimes) {
            if (st.mMovieId.equals(movieId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public ArrayList<ShowTime> getNextShowTimesByTheaterId(String theaterId) {
        ArrayList<ShowTime> showTimes = null;
        for (ShowTime st : mNextShowTimes) {
            if (st.mTheaterId.equals(theaterId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public ArrayList<ShowTime> getShowtimesByMovieId(String movieId) {
        ArrayList<ShowTime> showTimes = null;
        for (ShowTime st : mShowTimes) {
            if (st.mMovieId.equals(movieId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public ArrayList<ShowTime> getShowTimesByTheaterId(String theaterId) {
        ArrayList<ShowTime> showTimes = null;
        for (ShowTime st : mShowTimes) {
            if (st.mTheaterId.equals(theaterId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

}
