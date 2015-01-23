package com.tiwence.cinenow.utils;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.TheaterResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by temarill on 15/01/2015.
 */
public class ApiUtils {

    private static ApiUtils apiUtils;

    public static final String MOVIE_DB_API_KEY = "1a9b19642b2c7882115d38072489d252";
    public static final String MOVIE_DB_SEARCH_MOVIE_ROOT_URL = "http://api.themoviedb.org/3/search/movie?query=";
    public static final String MOVIE_DB_POSTER_ROOT_URL = "https://image.tmdb.org/t/p/w396";
    public static final String URL_API_MOVIE_THEATERS = "http://www.google.fr/movies?near=";

    public static ApiUtils instance() {
        if (apiUtils == null) {
            apiUtils = new ApiUtils();
        }
        return  apiUtils;
    }

    /**
     *
     * @param location
     * @param listener
     */
    public void retrieveMovieShowTimeTheaters(final Context context, Location location, final OnRetrieveTheatersCompleted listener) {
        new AsyncTask<Location, Void, TheaterResult>() {
            @Override
            protected TheaterResult doInBackground(Location... params) {

                Document doc = null;
                try {
                    Location l = params[0];
                    String query = l.getLatitude() + "," + l.getLongitude();
                    Log.d("Google theaters query", URL_API_MOVIE_THEATERS + query);
                    doc = Jsoup.connect(URL_API_MOVIE_THEATERS + query).get();
                    return createMovieTheatersList(context, doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(TheaterResult result) {
                super.onPostExecute(result);
                if (result != null)
                    listener.onRetrieveTheatersCompleted(result);
                else
                    listener.onRetrieveTheatersError("Impossible de récupérer les données");
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, location);
    }

    /**
     *
     * @param doc
     * @return
     */
    private TheaterResult createMovieTheatersList(Context context, Document doc) {
        ArrayList<MovieTheater> theaters = null;

        HashMap<String, Movie> moviesCached = ApplicationUtils.getMoviesInCache(context);

        HashMap<String, Movie> movies = new HashMap<>();
        if (doc != null) {
            theaters = new ArrayList<MovieTheater>();
            if (doc.getElementsByClass("movie_results").size() > 0) {
                Element content = doc.getElementsByClass("movie_results").get(0);
                Elements theatersDivs = content.getElementsByClass("theater");

                //Getting movie theaters
                for (Element theaterDiv : theatersDivs) {
                    MovieTheater movieTheater = new MovieTheater();
                    movieTheater.mName = theaterDiv.getElementsByTag("h2").get(0).text();
                    if (theaterDiv.getElementsByTag("h2").get(0).getElementsByTag("a") != null &&
                            theaterDiv.getElementsByTag("h2").get(0).getElementsByTag("a").size() > 0) {
                        String[] theaterUrlSplit = theaterDiv.getElementsByClass("name").get(0).getElementsByTag("a").attr("href").split("tid=");
                        movieTheater.mId = theaterUrlSplit[theaterUrlSplit.length - 1];
                        Log.d("THEATER", movieTheater.mName + " : " + movieTheater.mId);
                    }
                    movieTheater.mAddress = theaterDiv.getElementsByClass("info").get(0).text();

                    //Getting movies according to the theater
                    Elements movieDivs = theaterDiv.getElementsByClass("movie");
                    movieTheater.mShowtimesMap = new HashMap<String, ArrayList<String>>(movieDivs.size());
                    movieTheater.mShowtimesRemainingMap = new HashMap<String, ArrayList<Integer>>();
                    movieTheater.mShowTimes = new ArrayList<ShowTime>();

                    for (Element movieDiv : movieDivs) {
                        Movie movie = null;
                        String[] movieUrlSplit = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").attr("href").split("mid=");
                        String idG = movieUrlSplit[movieUrlSplit.length - 1];

                        if (moviesCached != null && moviesCached.containsKey(idG)) {
                            movie = moviesCached.get(idG);
                            movie.isOnDataset = true;
                        } else {
                            movie = new Movie();
                            movie.id_g = idG;
                            movie.title = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").get(0).text();
                            movie.duration_time = movieDiv.getElementsByClass("info").get(0).text().split("-")[0];
                        }

                        Element timeDiv = movieDiv.getElementsByClass("times").get(0);
                        ArrayList<String> showTimes = new ArrayList<String>();
                        ArrayList<Integer> showTimesRemaining = new ArrayList<Integer>();
                        for (Element timeSpan : timeDiv.getElementsByTag("span")) {
                            //Comparison between now and the showtime
                            String showTime = timeSpan.text().replaceAll("\\s", "");
                            if (showTime.length() > 0) {
                                int timeRemaining = ApplicationUtils.getTimeRemaining(showTime);
                                showTimes.add(timeSpan.text());
                                if (timeRemaining > 0) {
                                    ShowTime st = new ShowTime();
                                    st.mMovieId = movie.id_g;
                                    st.mTheaterId = movieTheater.mId;
                                    st.mShowTimeStr = showTime;
                                    st.mTimeRemaining = timeRemaining;
                                    movieTheater.mShowTimes.add(st);
                                    showTimesRemaining.add(timeRemaining);
                                }
                            }
                        }
                        if (!movies.containsKey(movie.id_g)) {
                            movies.put(movie.id_g, movie);
                        }
                        movieTheater.mShowtimesRemainingMap.put(movie.id_g, showTimesRemaining);
                        movieTheater.mShowtimesMap.put(movie.id_g, showTimes);
                        Collections.sort(movieTheater.mShowTimes, ShowTime.ShowTimeComparator);
                    }
                    //movieTheater.mShowTimes;
                    if (movieTheater.mShowTimes != null && movieTheater.mShowTimes.size() > 0)
                        theaters.add(movieTheater);
                }
            }
        }
        TheaterResult result = new TheaterResult();
        result.mMovieTheaters = theaters;
        result.mMovies = movies;
        return result;
    }

    public void retrieveMovieInfo(final Movie movie, final OnRetrieveMovieInfoCompleted listener) {
        new AsyncTask<Void, Void, Movie>() {
            @Override
            protected Movie doInBackground(Void... params) {

                String query = Uri.encode(movie.title.replaceAll("\\s", "+"));
                String searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY + "&language=fr&year=" + ApplicationUtils.getYear();
                String movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                Log.d("SEARCH MOVIE", searchMoVieUrl);
                try {
                    JSONObject movieJSON = new JSONObject(movieJSONString);
                    if (movieJSON.optInt("total_results") > 0) {
                        movie.id = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optInt("id");
                        movie.poster_path = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("poster_path");
                        //Log.d("MOVIE SEARCH", movie.title + "," + movie.poster_path);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return movie;
            }

            @Override
            protected void onPostExecute(Movie movie) {
                super.onPostExecute(movie);
                if (movie != null) {
                    listener.onRetrieveMovieInfoCompleted(movie);
                } else {
                    listener.onRetrieveMovieError("Movie info error");
                }
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    /**
     *
     * @param movies
     * @param listener
     */
    public void retrieveMoviesInfo(Context context, final HashMap<String, Movie> movies, final OnRetrieveMoviesInfoCompleted listener) {
        final HashMap<String, Movie> moviesCached = ApplicationUtils.getMoviesInCache(context);
        new AsyncTask<Void, Movie, HashMap<String, Movie>>() {
            @Override
            protected HashMap<String, Movie> doInBackground(Void... params) {

                Iterator it = movies.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry mapEntry = (Map.Entry) it.next();
                    Movie movie = (Movie) mapEntry.getValue();
                    if (movie.id == 0) {
                        String query = Uri.encode(movie.title).replaceAll("\\s", "+");
                        String searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY + "&language=fr";
                        String movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                        try {
                            JSONObject movieJSON = new JSONObject(movieJSONString);
                            if (movieJSON.optInt("total_results") > 0) {
                                movie.id = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optInt("id");
                                movie.poster_path = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("poster_path");
                                this.publishProgress(movie);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                return movies;
            }

            @Override
            protected void onProgressUpdate(Movie... values) {
                super.onProgressUpdate(values);
                listener.onProgressMovieInfoCompleted(values[0]);
            }

            @Override
            protected void onPostExecute(HashMap<String, Movie> movies) {
                super.onPostExecute(movies);
                if (movies != null) {
                    listener.onRetrieveMoviesInfoCompleted(movies);
                } else {
                    listener.onRetrieveMoviesError("Error");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param location
     * @param queryName
     * @param listener
     */
    public void retrieveQueryInfo(final Location location,  final String queryName, final OnRetrieveQueryCompleted listener) {
        new AsyncTask<Void, Void, Object>() {

            @Override
            protected Object doInBackground(Void... params) {
                Document doc = null;

                String query = location.getLatitude() + "," + location.getLongitude() +
                        "&q=" + Uri.encode(queryName).replaceAll("\\s", "+") + "&sort=1";
                Log.d("Google movies query", URL_API_MOVIE_THEATERS + query);

                try {
                    doc = Jsoup.connect(URL_API_MOVIE_THEATERS + query).get();
                    return createMovieOrTheater(doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (result instanceof MovieTheater) {
                        listener.onRetrieveQueryTheaterCompleted((MovieTheater) result);
                    } else if (result instanceof  Movie) {
                        listener.onRetrieveQueryMovieCompleted((Movie) result);
                    }
                } else {
                    listener.onRetrieveQueryError("Error");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param doc
     * @return
     */
    private Object createMovieOrTheater(Document doc) {

        if (doc.getElementsByClass("movie_results") != null
                && doc.getElementsByClass("movie_results").size() > 0) {
            Element content = doc.getElementsByClass("movie_results").get(0);

            if (content.getAllElements().get(0).getElementsByClass("movie") != null) {
                Element movieContent = content.getElementsByClass("movie").get(0);
                Movie movie = new Movie();
                movie.title = movieContent.getElementsByTag("h2").get(0).text();
                movie.infos_g = movieContent.getElementsByClass("info").get(0).text();
                movie.overview = movieContent.getElementsByClass("syn").get(0).text();
                if (movieContent.getElementsByClass("showtimes") != null
                        && movieContent.getElementsByClass("showtimes").get(0).getElementsByClass("theater") != null
                        && movieContent.getElementsByClass("showtimes").get(0).getElementsByClass("theater").size() > 0) {

                    for (Element showtimeElement : movieContent.getElementsByClass("showtimes").get(0).getElementsByClass("theater")) {
                        MovieTheater theater = new MovieTheater();
                        theater.mName = showtimeElement.getElementsByClass("name").get(0).text();
                        theater.mAddress = showtimeElement.getElementsByClass("address").get(0).text();

                        if (showtimeElement.getElementsByClass("times") != null
                                && showtimeElement.getElementsByClass("name").size() > 0) {
                            theater.mShowTimes = new ArrayList<ShowTime>();
                            for (Element timeElement : showtimeElement.getElementsByClass("name").get(0).getElementsByTag("span")) {
                                ShowTime showTime = new ShowTime();
                                showTime.mShowTimeStr = timeElement.text().replaceAll("\\s", "");
                                showTime.mTimeRemaining = ApplicationUtils.getTimeRemaining(showTime.mShowTimeStr);
                                theater.mShowTimes.add(showTime);
                            }
                        }
                    }
                }
                return movie;
            } else if (content.getAllElements().get(0).getElementsByClass("theater") != null) {

            }

        }

        return null;
    }
}
