package com.tiwence.cinenow2.model;

import com.tiwence.cinenow2.utils.ApplicationUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
    public ArrayList<ShowTime> mFirstShowTimes;

    public ArrayList<ShowTime> getNextShowtimesByMovieId(String movieId) {
        //mNextShowTimes = filterNewNextShowTimes();
        ArrayList<ShowTime> showTimes = null;
        for (ShowTime st : mNextShowTimes) {
            if (st.mMovieId.equals(movieId)) {
                if (showTimes == null) showTimes = new ArrayList<ShowTime>();
                showTimes.add(st);
            }
        }
        return showTimes;
    }

    public void filterNewNextShowTimes() {
        ArrayList<ShowTime> newNextShowTimes = new ArrayList<>();
        if (mShowTimes != null) {
            for (Iterator<String> it = mShowTimes.keySet().iterator(); it.hasNext();) {
                String idStKey = it.next();
                ShowTime st = mShowTimes.get(idStKey);
                int timeRemaining = ApplicationUtils.getTimeRemaining(st.mShowTimeStr);
                if (timeRemaining > 0 && timeRemaining < 95) {
                    st.mTimeRemaining = timeRemaining;
                    newNextShowTimes.add(st);
                }
            }
            Collections.sort(newNextShowTimes, ShowTime.ShowTimeComparator);
            mNextShowTimes = newNextShowTimes;
        }
    }

    public ArrayList<ShowTime> getNextShowTimesByTheaterId(String theaterId) {
        //mNextShowTimes = filterNewNextShowTimes();
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

    /**
     *
     * @param movieId
     * @return
     */
    public LinkedHashMap<MovieTheater, ArrayList<ShowTime>> getShowTimesByTheatersForMovie(String movieId) {
        LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();

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

    /**
     *
     * @param theaterId
     * @return
     */
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

    /**
     *
     * @param theater
     * @param dataset
     */
    public void addNewTheaterInfos(MovieTheater theater, LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
        if (this.mTheaters == null)
            this.mTheaters = new LinkedHashMap<>();
        if (mTheaters.containsKey(theater.mId))
            mTheaters.put(theater.mId, theater);

        Iterator<Movie> it = dataset.keySet().iterator();
        while (it.hasNext()) {
            Movie movieKey = it.next();
            if (this.mMovies == null) this.mMovies = new LinkedHashMap<>();
            if (!this.mMovies.containsKey(movieKey.title))
                this.mMovies.put(movieKey.title, movieKey);

            for (ShowTime st : dataset.get(movieKey)) {
                if (!this.mShowTimes.containsKey(st.mId)) {
                    mShowTimes.put(st.mId, st);
                    if (st.mTimeRemaining > 0 && st.mTimeRemaining < 95) {
                        if (!mNextShowTimes.contains(st)) mNextShowTimes.add(st);
                    }
                }
            }
            //filterNewNextShowTimes();
        }
        Collections.sort(mNextShowTimes, ShowTime.ShowTimeComparator);
    }

    public ArrayList<Movie> getNextMovies() {
        /*mNextShowTimes = filterNewNextShowTimes();*/
        ArrayList<Movie> nextMovies = new ArrayList<>();
        if (mNextShowTimes == null)
            mNextShowTimes = new ArrayList<>();
        for (ShowTime st : mNextShowTimes) {
            Movie movie = mMovies.get(st.mMovieId);
            if(!nextMovies.contains(movie))
                nextMovies.add(movie);
        }
        mNextMovies = nextMovies;
        return mNextMovies;
    }

    /**
     *
     * @param movie
     * @param dataset
     */
    public void addNewMovieInfos(Movie movie, LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset) {
        if (this.mMovies == null)
            this.mMovies = new LinkedHashMap<>();
        mMovies.put(movie.title, movie);

        Iterator<MovieTheater> it = dataset.keySet().iterator();
        while (it.hasNext()) {
            MovieTheater theaterKey = it.next();
            if (this.mTheaters == null) this.mTheaters = new LinkedHashMap<>();
            if (!this.mTheaters.containsKey(theaterKey.mId))
                this.mTheaters.put(theaterKey.mId, theaterKey);

            if (dataset.get(theaterKey)!= null) {
                for (ShowTime st : dataset.get(theaterKey)) {
                    if (!this.mShowTimes.containsKey(st.mId)) {
                        mShowTimes.put(st.mId, st);
                        if (st.mTimeRemaining > 0 && st.mTimeRemaining < 95) {
                            if (!mNextShowTimes.contains(st)) mNextShowTimes.add(st);
                        }
                    }
                }
            }
        }
        Collections.sort(mNextShowTimes, ShowTime.ShowTimeComparator);
    }
}
