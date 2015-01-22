package com.tiwence.cinenow.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by temarill on 20/01/2015.
 */
public class TheaterResult implements Serializable {

    public ArrayList<MovieTheater> mMovieTheaters;
    public HashMap<String, Movie> mMovies;

}
