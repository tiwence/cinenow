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
    public String mId;

    public boolean equals(Object obj) {
        if (!(obj instanceof ShowTime))
            return false;
        if (obj == this)
            return true;

        ShowTime st = (ShowTime) obj;
        if (this.mId.equals(st.mId))
            return true;
        else
            return false;
    }
}
