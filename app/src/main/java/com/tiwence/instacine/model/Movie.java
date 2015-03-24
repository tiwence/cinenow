package com.tiwence.instacine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by temarill on 15/01/2015.
 */
public class Movie implements Serializable {

    public String id_g;
    public String infos_g;
    public int id;
    public String imd_id;
    public String title;
    public String overview;
    public String poster_path;
    public String backdrop_path; //size = 396 ?
    public String release_date;
    public double vote_average;
    public String duration_time;
    public int runtime;
    public String kind;
    public ArrayList<String> kinds;
    public int mFirstTimeRemaining = -1;
    public ShowTime mBestNextShowtime;
    public double mBestDistance;

    public ArrayList<Cast> mCasts;
    public ArrayList<Crew> mCrew;

    public boolean isOnDataset;
    public double mRatio;

    public static Comparator<Movie> MovieRatioComparator = new Comparator<Movie>() {
        @Override
        public int compare(Movie s1, Movie s2) {
            if (s1.mRatio < s2.mRatio) return -1;
            if (s1.mRatio > s2.mRatio) return 1;
            return 0;
        }
    };

    public static Comparator<Movie> MovieDistanceComparator = new Comparator<Movie>() {
        @Override
        public int compare(Movie s1, Movie s2) {
            int comp = 0;

            if (s1.mBestDistance < s2.mBestDistance) comp = -1;
            if (s1.mBestDistance > s2.mBestDistance) comp = 1;

            if (comp != 0) {
                return comp;
            } else {
                return s1.mFirstTimeRemaining - s2.mFirstTimeRemaining;
            }
        }
    };


    public boolean movieInfosCompleted;

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Movie movie = (Movie) obj;
        if (title == null || movie.title == null) {
            return false;
        }
        if (this.title.equals(movie.title))
            return true;

        return false;
    }
}
