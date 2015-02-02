package com.tiwence.cinenow.model;

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
    //public ArrayList<String> genres;
    public String overview;
    public String poster_path;
    public String backdrop_path; //size = 396 ?
    public String release_date;
    public float vote_average;
    public String duration_time;
    public String kind;
    public int mFirstTimeRemaining = -1;
    public ShowTime mBestNextShowtime;
    public double mBestDistance;

    public boolean isOnDataset;

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

    public static Comparator<Movie> MovieNextShowTimeComparator = new Comparator<Movie>() {
        @Override
        public int compare(Movie s1, Movie s2) {
            return s1.mFirstTimeRemaining - s2.mFirstTimeRemaining;
        }
    };

    /**
     * Used when query
     */
    public ArrayList<MovieTheater> mTheathers;

    public double mRatio;


}
