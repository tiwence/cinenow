package com.tiwence.cinenow.utils;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.mapquest.android.Geocoder;
import com.tiwence.cinenow.R;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by temarill on 15/01/2015.
 */
public class ApiUtils {

    private static ApiUtils apiUtils;

    public static final String MOVIE_DB_API_KEY = "1a9b19642b2c7882115d38072489d252";
    public static final String MAPQUEST_API_KEY = "Fmjtd%7Cluu8290y2d%2Cr5%3Do5-9470qr";
    public static final String MOVIE_DB_SEARCH_MOVIE_ROOT_URL = "http://api.themoviedb.org/3/search/movie?query=";
    public static final String MAPQUEST_GEOCODING_ROOT_URL = "http://open.mapquestapi.com/geocoding/v1/address?key=" + MAPQUEST_API_KEY + "&location=";
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
    public void retrieveMovieShowTimeTheaters(final Context context, final Location location, final OnRetrieveShowTimesCompleted listener) {
        new AsyncTask<Void, Void, ShowTimesFeed>() {
            @Override
            protected ShowTimesFeed doInBackground(Void... params) {

                Document doc = null;
                try {
                    String query = location.getLatitude() + "," + location.getLongitude();
                    Log.d("Google theaters query", URL_API_MOVIE_THEATERS + query);
                    doc = Jsoup.connect(URL_API_MOVIE_THEATERS + query).get();
                    return createMovieTheatersList(context, doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(ShowTimesFeed result) {
                super.onPostExecute(result);

                if (result != null) {
                    geocodingAndDistanceCalculation(location, result, listener);
                } else {
                    listener.onRetrieveShowTimesError("Impossible de récupérer les données");
                }
                /*if (result != null)
                    listener.onRetrieveShowTimesCompleted(result);
                else
                    listener.onRetrieveShowTimesError("Impossible de récupérer les données");*/
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void geocodingAndDistanceCalculation(final Location location, final ShowTimesFeed result, final OnRetrieveShowTimesCompleted listener) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Iterator<String> it = result.mTheaters.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    MovieTheater theater = result.mTheaters.get(key);
                    if (theater.mLatitude == -10000 && theater.mLongitude == -10000) {
                        String geocodingUrl = MAPQUEST_GEOCODING_ROOT_URL + Uri.encode(theater.mAddress);
                        String geocodingJSONString = HttpUtils.httpGet(geocodingUrl);
                        try {
                            JSONObject geocodingJSON = new JSONObject(geocodingJSONString);
                            if (geocodingJSON.optJSONArray("results") != null && geocodingJSON.optJSONArray("results").length() > 0) {
                                JSONObject resultJSON = (JSONObject) geocodingJSON.optJSONArray("results").get(0);
                                if (resultJSON.optJSONArray("locations") != null) {
                                    JSONObject latLngJSON = ((JSONObject) resultJSON.optJSONArray("locations").get(0)).optJSONObject("latLng");
                                    theater.mLatitude = latLngJSON.optDouble("lat");
                                    theater.mLongitude = latLngJSON.optDouble("lng");
                                    Location dest = new Location("");
                                    dest.setLatitude(theater.mLatitude);
                                    dest.setLongitude(theater.mLongitude);
                                    theater.mDistance = location.distanceTo(dest) / 1000;
                                    Log.d("Geocoding : ", theater.mName + ", " + theater.mLatitude + ", " + theater.mLongitude + ", " + theater.mDistance);
                                } else {
                                    theater.mDistance = 1000;
                                }
                            }
                        } catch (JSONException e) {
                            theater.mDistance = 1000;
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void _result) {
                super.onPostExecute(_result);
                calculateRatioAndNextTimeRemaining(result);
                listener.onRetrieveShowTimesCompleted(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param doc
     * @return
     */
    private ShowTimesFeed createMovieTheatersList(Context context, Document doc) {
        LinkedHashMap<String, MovieTheater> theaters = null;
        LinkedHashMap<String, Movie> moviesCached = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(context, ApplicationUtils.MOVIES_FILE_NAME);
        LinkedHashMap<String, MovieTheater> theatersCached = (LinkedHashMap<String, MovieTheater>) ApplicationUtils.getDataInCache(context, ApplicationUtils.THEATERS_FILE_NAME);
        LinkedHashMap<String, Movie> movies = new LinkedHashMap<String, Movie>();
        ArrayList<ShowTime> showTimes = null;
        ArrayList<ShowTime> nextShowtimes = null;
        LinkedHashMap<String, Movie> nextMovies = new LinkedHashMap<>();

        if (doc != null) {
            theaters = new LinkedHashMap<String, MovieTheater>();
            if (doc.getElementsByClass("movie_results").size() > 0) {
                Element content = doc.getElementsByClass("movie_results").get(0);
                Elements theatersDivs = content.getElementsByClass("theater");

                //Getting movie theaters
                for (Element theaterDiv : theatersDivs) {
                    int showTimeNb = 0;

                    String theaterId = null;
                    if (theaterDiv.getElementsByTag("h2").get(0).getElementsByTag("a") != null &&
                            theaterDiv.getElementsByTag("h2").get(0).getElementsByTag("a").size() > 0) {
                        String[] theaterUrlSplit = theaterDiv.getElementsByClass("name").get(0).getElementsByTag("a").attr("href").split("tid=");
                        theaterId = theaterUrlSplit[theaterUrlSplit.length - 1];
                    }

                    //Check if theater already exists in cache
                    MovieTheater movieTheater = new MovieTheater();
                    if (theatersCached != null && theatersCached.containsKey(theaterId)) {
                        movieTheater = theatersCached.get(theaterId);
                    } else {
                        movieTheater.mId = theaterId;
                        movieTheater.mName = theaterDiv.getElementsByTag("h2").get(0).text();
                        if (theaterDiv.getElementsByClass("info").get(0).text().split(" - ").length > 1)
                            movieTheater.mAddress = theaterDiv.getElementsByClass("info").get(0).text().split(" - ")[0];
                        else
                            movieTheater.mAddress = theaterDiv.getElementsByClass("info").get(0).text();

                    }

                    //Getting movies according to the theater
                    Elements movieDivs = theaterDiv.getElementsByClass("movie");

                    for (Element movieDiv : movieDivs) {
                        Movie movie = null;
                        String[] movieUrlSplit = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").attr("href").split("mid=");
                        String idG = movieUrlSplit[movieUrlSplit.length - 1];

                        if (moviesCached != null && moviesCached.containsKey(idG)) {
                            movie = moviesCached.get(idG);
                            movie.isOnDataset = true;
                            movie.mFirstTimeRemaining = -1;
                        } else {
                            movie = new Movie();
                            movie.id_g = idG;
                            movie.title = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").get(0).text();
                            movie.duration_time = movieDiv.getElementsByClass("info").get(0).text().split(" - ")[0];
                            if (movieDiv.getElementsByClass("info").get(0).text().split(" - ").length > 2)
                                movie.kind = movieDiv.getElementsByClass("info").get(0).text().split(" - ")[2].trim();
                            movie.mFirstTimeRemaining = -1;
                        }

                        //Loop on showtimes feed by Google
                        Element timeDiv = movieDiv.getElementsByClass("times").get(0);

                        for (Element timeSpan : timeDiv.getElementsByTag("span")) {
                            //Comparison between the current time and the showtime
                            String showTime = timeSpan.text().replaceAll("\\s", "");
                            if (showTime.length() > 0) {
                                if (showTimes == null && nextShowtimes == null) {
                                    showTimes = new ArrayList<ShowTime>();
                                    nextShowtimes = new ArrayList<>();
                                }
                                int timeRemaining = ApplicationUtils.getTimeRemaining(showTime);

                                //showTimes.add(timeSpan.text());
                                ShowTime st = new ShowTime();
                                st.mMovieId = movie.id_g;
                                st.mTheaterId = movieTheater.mId;
                                st.mShowTimeStr = showTime;
                                st.mTimeRemaining = timeRemaining;
                                showTimes.add(st);
                                if (timeRemaining > 0 && timeRemaining < 95) {
                                    showTimeNb++;
                                    nextShowtimes.add(st);

                                    if (!nextMovies.containsKey(st.mMovieId)) {
                                        nextMovies.put(movie.id_g, movie);
                                    }
                                }
                            }
                        }
                        Collections.sort(showTimes, ShowTime.ShowTimeComparator);
                        Collections.sort(nextShowtimes, ShowTime.ShowTimeComparator);
                        if (!movies.containsKey(movie.id_g)) {
                            movies.put(movie.id_g, movie);
                        }
                    }
                    if (showTimeNb > 0)
                        theaters.put(movieTheater.mId, movieTheater);
                }
            }
        }
        ShowTimesFeed result = new ShowTimesFeed();
        result.mTheaters = theaters;
        result.mMovies = movies;
        result.mNextShowTimes = nextShowtimes;
        result.mShowTimes = showTimes;
        result.mNextMovies = new ArrayList<Movie>(nextMovies.values());

        return result;
    }

    private void calculateRatioAndNextTimeRemaining(ShowTimesFeed result) {
        for (Movie m : result.mNextMovies) {
            ArrayList<ShowTime> sts = result.getNextShowtimesByMovieId(m.id_g);
            Collections.sort(sts, ShowTime.ShowTimeComparator);
            m.mBestDistance = 10000;
            //Distance
            for (ShowTime st : sts) {
                //double ratio = st.mTimeRemaining / (result.mTheaters.get(st.mTheaterId).mDistance * 10);

                if (m.mBestDistance > result.mTheaters.get(st.mTheaterId).mDistance) {
                    m.mBestDistance = result.mTheaters.get(st.mTheaterId).mDistance;
                    m.mBestNextShowtime = st;
                    m.mFirstTimeRemaining = st.mTimeRemaining;
                    result.mMovies.get(m.id_g).mFirstTimeRemaining = st.mTimeRemaining;
                }

                /*if (ratio > 0.9 && ratio < 1.7) {
                    m.mRatio = ratio;
                    result.mMovies.get(m.id_g).mRatio = ratio;
                    m.mBestNextShowtime = st;
                    result.mMovies.get(m.id_g).mBestNextShowtime = st;
                    break;
                }*/
            }
            /*if (m.mRatio <= 0) {
                m.mRatio = 1000;
                result.mMovies.get(m.id_g).mRatio = 1000;
            }

            Log.d("Ratio", m.title + " : " + m.mRatio);*/

        }
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
    public void retrieveMoviesInfo(Context context, final LinkedHashMap<String, Movie> movies, final OnRetrieveMoviesInfoCompleted listener) {
        new AsyncTask<Void, Movie, LinkedHashMap<String, Movie>>() {
            @Override
            protected LinkedHashMap<String, Movie> doInBackground(Void... params) {

                Iterator it = movies.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry mapEntry = (Map.Entry) it.next();
                    Movie movie = (Movie) mapEntry.getValue();
                    if (movie.id == 0) {
                        String query = Uri.encode(movie.title).replaceAll("\\s", "+");
                        String searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY + "&language=fr";
                        Log.d("Movie infos", searchMoVieUrl);
                        String movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                        try {
                            JSONObject movieJSON = new JSONObject(movieJSONString);
                            if (movieJSON.optInt("total_results") > 0) {
                                movie.id = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optInt("id");
                                movie.poster_path = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("poster_path");
                                movie.backdrop_path = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("backdrop_path");
                                //movie.overview = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("overview");
                                movie.release_date = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optString("release_date");
                                movie.vote_average = ((JSONObject)movieJSON.optJSONArray("results").get(0)).optLong("vote_average");
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
            protected void onPostExecute(LinkedHashMap<String, Movie> movies) {
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
