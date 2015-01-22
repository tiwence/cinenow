package com.tiwence.cinenow.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by temarill on 15/01/2015.
 */
public class Movie implements Serializable {

    public String id_g;
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

    public boolean isOnDataset;

    public ArrayList<Credit> credits;

}
