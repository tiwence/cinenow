package com.tiwence.cinenow.model;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Created by temarill on 20/01/2015.
 */
public class ShowTime implements Serializable {

    public String mShowTimeStr;
    public int mTimeRemaining;
    public String mTheaterId;
    public String mMovieId;

    public static Comparator<ShowTime> ShowTimeComparator = new Comparator<ShowTime>() {
        @Override
        public int compare(ShowTime s1, ShowTime s2) {
            return s1.mTimeRemaining - s2.mTimeRemaining;
        }
    };
}
