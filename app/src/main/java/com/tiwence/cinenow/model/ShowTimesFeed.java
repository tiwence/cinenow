package com.tiwence.cinenow.model;

import android.util.Log;

import com.tiwence.cinenow.utils.ApplicationUtils;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 26/01/2015.
 */
public class ShowTimesFeed implements Serializable {

    public LinkedHashMap<String, MovieTheater> mTheaters;
    public LinkedHashMap<String, Movie> mMovies;
    public ArrayList<Movie> mNextMovies;
    public ArrayList<ShowTime> mNextShowTimes;
    public LinkedHashMap<String, ShowTime> mShowTimes;
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
        for (Iterator<String> it = mShowTimes.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            ShowTime st = mShowTimes.get(key);
            if (st.mMovieId.equals(movieId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public LinkedHashMap<MovieTheater, ArrayList<ShowTime>> getShowTimesByTheatersForMovie(String movieId) {
        LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();
        Log.d("SHOWTIMES SIZE", "" + mShowTimes.size() + " MOVIE ID " + movieId);

        for (Iterator<String> it = mShowTimes.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            ShowTime st = mShowTimes.get(key);
            if (st.mMovieId != null && st.mMovieId.equals(movieId)) {
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

    public LinkedHashMap<Movie, ArrayList<ShowTime>> getShowTimesByMoviesForTheater(String theaterId) {
        LinkedHashMap<Movie, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();
        ArrayList<ShowTime> showTimes = getShowTimesByTheaterId(theaterId);
        if (showTimes != null) {
            for (ShowTime st : showTimes) {
                if (dataset.containsKey(this.mMovies.get(st.mMovieId))) {
                    ArrayList<ShowTime> sts = dataset.get(this.mMovies.get(st.mMovieId));
                    boolean alreadyInd = false;
                    for (ShowTime sttemp : sts ) {
                        if (sttemp.mShowTimeStr.equals(st.mShowTimeStr))
                            alreadyInd = true;
                    }
                    if (!alreadyInd) sts.add(st);
                } else {
                    ArrayList<ShowTime> sts = new ArrayList<>();
                    sts.add(st);
                    if (this.mMovies.get(st.mMovieId) != null) {
                        dataset.put(this.mMovies.get(st.mMovieId), sts);
                    }
                }
            }
        }
        return dataset;
    }

    public ArrayList<ShowTime> getShowTimesByTheaterId(String theaterId) {
        ArrayList<ShowTime> showTimes = null;
        for (Iterator<String> it = mShowTimes.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            ShowTime st = mShowTimes.get(key);
            if (st.mTheaterId.equals(theaterId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public void addNewTheaterInfos(MovieTheater theater, LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
        if (this.mTheaters == null)
            this.mTheaters = new LinkedHashMap<>();
        mTheaters.put(theater.mId, theater);

        Iterator<Movie> it = dataset.keySet().iterator();
        while (it.hasNext()) {
            Movie movieKey = it.next();
            if (this.mMovies == null) this.mMovies = new LinkedHashMap<>();
            if (this.mMovies.containsKey(movieKey.id_g)) this.mMovies.put(movieKey.id_g, movieKey);

            for (ShowTime st : dataset.get(movieKey)) {
                if (!this.mShowTimes.containsKey(st.mId)) {
                    Log.d("Adding SHOWTIME", "Movie : " + movieKey.title + ", " + st.mId);
                    mShowTimes.put(st.mId, st);
                }
            }

        }
        Log.d("SHOWTIMES SIZE", "" + mShowTimes.size());
    }
}
