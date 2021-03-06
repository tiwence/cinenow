package com.tiwence.cinenow2.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by temarill on 15/01/2015.
 */
public class MovieTheater implements Serializable {

    public String mId;
    public String mName;
    public String mAddress;
    //public HashMap<String, ArrayList<String>> mShowtimesMap;
    //public HashMap<String, ArrayList<Integer>> mShowtimesRemainingMap;
    public ArrayList<ShowTime> mShowTimes;
    public ArrayList<ShowTime> mNextShowTimes;

    public double mDistance = 10000;
    public double mLatitude = -10000;
    public double mLongitude = -10000;

    //public ArrayList<Movie> mMovies;

    public static Comparator<MovieTheater> MovieTheatersDistanceComparator = new Comparator<MovieTheater>() {
        @Override
        public int compare(MovieTheater lhs, MovieTheater rhs) {
            if (lhs.mDistance < rhs.mDistance) return  -1;
            else if (lhs.mDistance > rhs.mDistance) return 1;
            return 0;
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        MovieTheater movie = (MovieTheater) obj;
        if (mName == null || movie.mName == null) {
            return false;
        }
        if (this.mName.equals(movie.mName))
            return true;

        return false;
    }
}
