package com.tiwence.cinenow.model;

import com.tiwence.cinenow.utils.ApplicationUtils;

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
            int timeRemaining = ApplicationUtils.getTimeRemaining(st.mShowTimeStr);
            if (timeRemaining > 0 && st.mMovieId.equals(movieId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                st.mTimeRemaining = timeRemaining;
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

    public LinkedHashMap<MovieTheater, ArrayList<ShowTime>> getShowTimesByTheatersForMovie(String movieId) {
        LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();
        for (ShowTime st : mShowTimes) {
            if (st.mMovieId.equals(movieId)) {
                if (dataset.containsKey(this.mTheaters.get(st.mTheaterId))) {
                    ArrayList<ShowTime> sts = dataset.get(this.mTheaters.get(st.mTheaterId));
                    sts.add(st);
                } else {
                    ArrayList<ShowTime> sts = new ArrayList<>();
                    sts.add(st);
                    if (this.mTheaters.get(st.mTheaterId) != null) {
                        dataset.put(this.mTheaters.get(st.mTheaterId), sts);
                    }
                }
            }
        }

        return dataset;
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
