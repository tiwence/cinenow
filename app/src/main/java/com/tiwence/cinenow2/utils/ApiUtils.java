package com.tiwence.cinenow2.utils;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.tiwence.cinenow2.listener.OnRetrieveMovieCreditsCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveMovieMoreInfosCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveMovieShowTimesCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveTheaterShowTimeInfoCompleted;
import com.tiwence.cinenow2.model.Cast;
import com.tiwence.cinenow2.model.Crew;
import com.tiwence.cinenow2.model.ShowTime;
import com.tiwence.cinenow2.model.ShowTimesFeed;
import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.MovieTheater;

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
    public static final String MOVIE_DB_MOVIE_ROOT_URL = "http://api.themoviedb.org/3/movie/";
    public static final String MAPQUEST_GEOCODING_ROOT_URL = "http://open.mapquestapi.com/geocoding/v1/address?key=" + MAPQUEST_API_KEY + "&location=";
    public static final String MOVIE_DB_POSTER_ROOT_URL = "https://image.tmdb.org/t/p/w396";
    public static final String URL_API_MOVIE_THEATERS = "http://www.google.fr/movies?near=";
    public static final String URL_API_THEATER = "http://www.google.fr/movies?tid=";

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
                    TheatersUtils.instance().loadTheatersLocation(context);
                    if (location != null) {
                        String query = location.getLatitude() + "," + location.getLongitude();
                        doc = Jsoup.connect(URL_API_MOVIE_THEATERS + query).get();
                        return createMovieTheatersList(context, doc);
                    }
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
                    TheatersUtils.instance().writeJSONTheaterLocationsFile(context);
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
                    theater = geocodeSpecificTheater(theater, location);
                    //Log.d("Theater distance", theater.mName + " : " +theater.mDistance);
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
     * @param theater
     * @param location
     * @return
     */
    public MovieTheater geocodeSpecificTheater(MovieTheater theater, Location location) {
        if (theater.mLatitude == -10000 && theater.mLongitude == -10000) {
            theater = TheatersUtils.instance().getTheaterDistance(theater, location);
            //Log.d("Theater distance local", theater.mName + " : " + theater.mDistance);
            if (theater.mDistance < 0) {
                String geocodingUrl = MAPQUEST_GEOCODING_ROOT_URL + Uri.encode(theater.mAddress);
                //Log.d("Geocoding url", geocodingUrl);
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
                            theater.mDistance = (double)Math.round(theater.mDistance * 100) / 100;
                            //Log.d("Geocoding : ", theater.mName + ", " + theater.mLatitude + ", " + theater.mLongitude + ", " + theater.mDistance);
                            TheatersUtils.instance().putTheaterDistance(theater);
                        } else {
                            theater.mDistance = 1000;
                        }
                    }
                } catch (JSONException e) {
                    theater.mDistance = 1000;
                    e.printStackTrace();
                }
            }
        } else {
            Location dest = new Location("");
            dest.setLatitude(theater.mLatitude);
            dest.setLongitude(theater.mLongitude);
            dest.setLongitude(theater.mLongitude);
            theater.mDistance = (double)Math.round(theater.mDistance * 100) / 100;;
        }

        return theater;
    }

    /**
     *
     * @param doc
     * @return
     */
    private ShowTimesFeed createMovieTheatersList(Context context, Document doc) {
        LinkedHashMap<String, MovieTheater> theaters = null;
        LinkedHashMap<String, Movie> moviesCached = (LinkedHashMap<String, Movie>)
                ApplicationUtils.getDataInCache(context, ApplicationUtils.MOVIES_FILE_NAME);
        LinkedHashMap<String, MovieTheater> theatersCached = (LinkedHashMap<String, MovieTheater>)
                ApplicationUtils.getDataInCache(context, ApplicationUtils.THEATERS_FILE_NAME);
        LinkedHashMap<String, Movie> movies = new LinkedHashMap<String, Movie>();
        LinkedHashMap<String, ShowTime> showTimes = null;
        ArrayList<ShowTime> nextShowtimes = null;
        ArrayList<ShowTime> firstShowTimes = null;
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

                    //Log.d("THEATER GET", movieTheater.mId + ", " + movieTheater.mName);

                    //Getting movies according to the theater
                    Elements movieDivs = theaterDiv.getElementsByClass("movie");
                    for (Element movieDiv : movieDivs) {
                        Movie movie = null;
                        String[] movieUrlSplit = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").attr("href").split("mid=");
                        String idG = movieUrlSplit[movieUrlSplit.length - 1];
                        String movieName = movieDiv.getElementsByClass("name").get(0).getElementsByTag("a").get(0).text();
                        if (moviesCached != null && moviesCached.containsKey(movieName)) {
                            movie = moviesCached.get(movieName);
                            movie.isOnDataset = true;
                        } else {
                            movie = new Movie();
                            movie.id_g = idG;
                            movie.title = movieName;
                            movie.duration_time = movieDiv.getElementsByClass("info").get(0).text().split(" - ")[0];
                            movie.infos_g = movieDiv.getElementsByClass("info").get(0).text();
                            if (movieDiv.getElementsByClass("info").get(0).text().split(" - ").length > 2)
                                movie.kind = movieDiv.getElementsByClass("info").get(0).text().split(" - ")[2].trim();
                        }
                        movie.mFirstTimeRemaining = -1;

                        //Loop on showtimes according to the movie and the theater
                        Element timeDiv = movieDiv.getElementsByClass("times").get(0);

                        int stIndex = 0;
                        for (Element timeSpan : timeDiv.getElementsByTag("span")) {
                            //Comparison between the current time and the showtime
                            String showTime = timeSpan.text().replaceAll("\\s", "");

                            if (showTime.length() > 0) {
                                if (showTimes == null && nextShowtimes == null) {
                                    showTimes = new LinkedHashMap<>();
                                    nextShowtimes = new ArrayList<>();
                                }
                                int timeRemaining = ApplicationUtils.getTimeRemaining(showTime);

                                ShowTime st = new ShowTime();
                                st.mMovieId = movie.title;
                                st.mTheaterId = movieTheater.mId;
                                st.mShowTimeStr = showTime;
                                st.mTimeRemaining = timeRemaining;
                                st.mId = st.mMovieId + st.mTheaterId + st.mShowTimeStr;
                                showTimes.put(st.mId, st);
                                if (stIndex == 0) {
                                    if (firstShowTimes == null) firstShowTimes = new ArrayList<>();
                                    firstShowTimes.add(st);
                                }
                                if (timeRemaining > 0 && timeRemaining < 95) {
                                    showTimeNb++;
                                    nextShowtimes.add(st);
                                    if (!nextMovies.containsKey(st.mMovieId)) {
                                        nextMovies.put(movie.title, movie);
                                    }
                                }
                                stIndex++;
                            }
                        }
                        //We sort showtimes by their time remaining
                        //Collections.sort(showTimes, ShowTime.ShowTimeComparator);
                        if (!movies.containsKey(movie.title)) {
                            movies.put(movie.title, movie);
                        }
                    }
                    //if (showTimeNb > 0)
                        theaters.put(movieTheater.mId, movieTheater);
                }
            }
        }
        Collections.sort(nextShowtimes, ShowTime.ShowTimeComparator);
        ShowTimesFeed result = new ShowTimesFeed();
        result.mTheaters = theaters;
        result.mMovies = movies;
        result.mNextShowTimes = nextShowtimes;
        result.mShowTimes = showTimes;
        result.mNextMovies = new ArrayList<Movie>(nextMovies.values());
        result.mFirstShowTimes = firstShowTimes;

        return result;
    }



    /**
     *
     * @param result
     */
    private void calculateRatioAndNextTimeRemaining(ShowTimesFeed result) {
        for (Movie m : result.mNextMovies) {
            ArrayList<ShowTime> sts = result.getNextShowtimesByMovieId(m.title);
            Collections.sort(sts, ShowTime.ShowTimeComparator);
            m.mBestDistance = 10000;
            //Distance
            for (ShowTime st : sts) {
                //double ratio = st.mTimeRemaining / (result.mTheaters.get(st.mTheaterId).mDistance * 10);

                if (m.mBestDistance > result.mTheaters.get(st.mTheaterId).mDistance) {
                    m.mBestDistance = result.mTheaters.get(st.mTheaterId).mDistance;
                    m.mBestNextShowtime = st;
                    m.mFirstTimeRemaining = st.mTimeRemaining;
                    result.mMovies.get(m.title).mBestNextShowtime = st;
                    result.mMovies.get(m.title).mFirstTimeRemaining = st.mTimeRemaining;
                    result.mMovies.get(m.title).mBestDistance = m.mBestDistance;
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

    /**
     *
     * @param movie
     * @param listener
     */
    public void retrieveMovieInfo(final Movie movie, final OnRetrieveMovieInfoCompleted listener) {
        new AsyncTask<Void, Void, Movie>() {
            @Override
            protected Movie doInBackground(Void... params) {

                String query = Uri.encode(movie.title.replaceAll("\\s", "+").replaceAll("3D", ""));
                String searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY +
                        "&language=" + Locale.getDefault().getLanguage().trim() + "&year=" + ApplicationUtils.getYear();
                String movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                if (movieJSONString != null) {
                    try {
                        JSONObject movieJSON = new JSONObject(movieJSONString);
                        if (movieJSON.optInt("total_results") > 0) {
                            fillMovieInfos(movie, (JSONObject)movieJSON.optJSONArray("results").get(0));
                        } else {
                            searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY +
                                    "&language=" + Locale.getDefault().getLanguage().trim();
                            movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                            if (movieJSONString != null) {
                                movieJSON = new JSONObject(movieJSONString);
                                if (movieJSON.optInt("total_results") > 0)
                                    fillMovieInfos(movie, (JSONObject)movieJSON.optJSONArray("results").get(0));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
     * @param movie
     * @param movieJSON
     */
    public void fillMovieInfos(Movie movie, JSONObject movieJSON) {
        movie.id = movieJSON.optInt("id");
        movie.poster_path = movieJSON.optString("poster_path");
        movie.backdrop_path = movieJSON.optString("backdrop_path");
        movie.release_date = movieJSON.optString("release_date");
        movie.vote_average = movieJSON.optDouble("vote_average");
        movie.runtime = movieJSON.optInt("runtime");
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
                        String query = Uri.encode(movie.title).replaceAll("\\s", "+").replaceAll("3D", "");
                        String searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY
                                + "&language=" + Locale.getDefault().getLanguage().trim() + "&year=" + ApplicationUtils.getYear();
                        //Log.d("Movie infos", searchMoVieUrl);
                        String movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                        if (movieJSONString != null) {
                            try {
                                JSONObject movieJSON = new JSONObject(movieJSONString);
                                if (movieJSON.optInt("total_results") > 0) {
                                    fillMovieInfos(movie, (JSONObject)movieJSON.optJSONArray("results").get(0));
                                } else {
                                    searchMoVieUrl = MOVIE_DB_SEARCH_MOVIE_ROOT_URL + query + "&api_key=" + MOVIE_DB_API_KEY +
                                            "&language=" + Locale.getDefault().getLanguage().trim();
                                    movieJSONString = HttpUtils.httpGet(searchMoVieUrl);
                                    if (movieJSONString != null) {
                                        movieJSON = new JSONObject(movieJSONString);
                                        if (movieJSON.optInt("total_results") > 0)
                                            fillMovieInfos(movie, (JSONObject)movieJSON.optJSONArray("results").get(0));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
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
        new AsyncTask<Void, Void, List<Object>>() {

            @Override
            protected List<Object> doInBackground(Void... params) {
                if (queryName.trim().length() > 0) {
                    Document doc = null;

                    String query = location.getLatitude() + "," + location.getLongitude() +
                            "&q=" + Uri.encode(queryName).replaceAll("\\s", "+") + "&sort=1";
                    //Log.d("Google movies query", URL_API_MOVIE_THEATERS + query);

                    try {
                        doc = Jsoup.connect(URL_API_MOVIE_THEATERS + query).get();
                        return createMovieOrTheater(doc);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(List<Object> result) {
                super.onPostExecute(result);
                if (result != null) {
                    listener.onRetrieveQueryCompleted(result);
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
    private List<Object> createMovieOrTheater(Document doc) {
        List<Object> data = null;
        if (doc.getElementsByClass("movie_results") != null
                && doc.getElementsByClass("movie_results").size() > 0) { //movies or theaters fetched

            Element result = doc.getElementsByClass("movie_results").get(0);
            String className = result.child(0).className();
            Elements moviesElement = result.getElementsByClass("movie");
            Elements theatersElement = result.getElementsByClass("theater");

            if (className.equals("movie")) {
                for (Element movieElement : moviesElement) {
                    if (movieElement != null && movieElement.getElementsByTag("h2") != null
                            && movieElement.getElementsByTag("h2").size() > 0) {
                        Movie movie = new Movie();
                        movie.title = movieElement.getElementsByTag("h2").get(0).text();
                        movie.infos_g = movieElement.getElementsByClass("info").get(0).text();
                        movie.overview = movieElement.getElementsByClass("syn").get(0).text();

                        if (doc.getElementsByClass("movie_results").get(0).getElementsByClass("movie").size() > 1) { //we got id in the href element
                            String movieLink = movieElement.getElementsByTag("a").get(0).attr("href");
                            movie.id_g = movieLink.split("mid=")[movieLink.split("mid=").length - 1];
                        } else { //we got id in the left section...
                            Element leftSection = doc.getElementsByClass("section").get(0);
                            if (leftSection.getElementsByAttributeValue("name", "mid") != null
                                    && leftSection.getElementsByAttributeValue("name", "mid").size() > 0) {
                                movie.id_g = leftSection.getElementsByAttributeValue("name", "mid").get(0).attr("value");
                            }
                        }

                        //Log.d("PARSE QUERY RESULT", "Movie : " + movie.title + ", " + movie.id_g);

                        //if (movie.id_g != null && !movie.id_g.equals("")) {
                            if (data == null) data = new ArrayList<>();
                                data.add(movie);
                        //}
                    }
                }
            } else if (className.equals("theater")) {
                for (Element theaterElement : theatersElement) {
                    if (theaterElement != null) {
                        MovieTheater movieTheater = new MovieTheater();
                        String movieLink = theaterElement.getElementsByTag("a").get(0).attr("href");
                        movieTheater.mId = movieLink.split("tid=")[movieLink.split("tid=").length - 1];
                        if (movieTheater.mId == null || movieTheater.mId.isEmpty()) {
                            movieLink = doc.getElementById("left_nav").getElementsByTag("a").get(0).attr("href");
                            movieTheater.mId = movieLink.split("tid=")[movieLink.split("tid=").length - 1];
                        }
                        movieTheater.mName = theaterElement.getElementsByTag("h2").get(0).text();
                        if (theaterElement.getElementsByClass("info").get(0).text().split(" - ").length > 1)
                            movieTheater.mAddress = theaterElement.getElementsByClass("info").get(0).text().split(" - ")[0];
                        else
                            movieTheater.mAddress = theaterElement.getElementsByClass("info").get(0).text();
                        movieTheater.mDistance = 10000.0;

                        if (movieTheater.mName != null && !movieTheater.mName.equals("")) {
                            if (data == null) data = new ArrayList<>();
                            data.add(movieTheater);
                        }
                    }
                }
            }
        }

        return data;
    }

    /**
     *
     * @param movie
     * @param listener
     */
    public void retrieveMoreMovieInfos(final Movie movie, final OnRetrieveMovieMoreInfosCompleted listener) {
        new AsyncTask<Void, Void, Movie>() {

            @Override
            protected Movie doInBackground(Void... params) {

                String movieUrl = MOVIE_DB_MOVIE_ROOT_URL + movie.id + "?api_key=" + MOVIE_DB_API_KEY +
                        "&language=" + Locale.getDefault().getLanguage().trim();
                //Log.d("Movie infos", movieUrl);
                String movieJSONString = HttpUtils.httpGet(movieUrl);
                try {
                    if (movieJSONString != null) {
                        JSONObject movieJSON = new JSONObject(movieJSONString);
                        movie.release_date = movieJSON.optString("release_date");
                        movie.overview = movieJSON.optString("overview");
                        for(int i = 0; i < movieJSON.optJSONArray("genres").length(); i++) {
                            if (movie.kinds == null) movie.kinds = new ArrayList<String>();
                            if (!movie.kinds.contains(movieJSON.optJSONArray("genres").getJSONObject(i).optString("name")))
                                movie.kinds.add(movieJSON.optJSONArray("genres").getJSONObject(i).optString("name"));
                        }
                        return movie;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Movie movie) {
                super.onPostExecute(movie);
                if (movie != null) {
                    listener.onRetrieveMovieMoreInfosCompleted(movie);
                } else {
                    listener.onRetrieveMovieMoreInfosError("Error");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param movie
     * @param listener
     */
    public void retrieveMovieCredits(final Movie movie, final OnRetrieveMovieCreditsCompleted listener) {
        new AsyncTask<Void, Void, Movie>() {

            @Override
            protected Movie doInBackground(Void... params) {

                String movieUrl = MOVIE_DB_MOVIE_ROOT_URL + movie.id + "/credits?api_key=" + MOVIE_DB_API_KEY +
                        "&language=" + Locale.getDefault().getLanguage().trim();
                //Log.d("Credits infos", movieUrl);
                String creditsJSONString = HttpUtils.httpGet(movieUrl);
                if (creditsJSONString != null) {
                    try {
                        JSONObject creditsJSON = new JSONObject(creditsJSONString);
                        JSONArray castJSONArray = creditsJSON.optJSONArray("cast");
                        //Getting cast member
                        for (int i = 0; i < castJSONArray.length(); i++) {
                            JSONObject castJSON = castJSONArray.optJSONObject(i);
                            if (castJSON != null) {
                                if (movie.mCasts == null) movie.mCasts = new ArrayList<Cast>();
                                Cast cast = new Cast();
                                cast.character = castJSON.optString("character");
                                cast.credit_id = castJSON.optString("credit_id");
                                cast.id = castJSON.optInt("id");
                                cast.name = castJSON.optString("name");
                                cast.order = castJSON.optInt("order");
                                cast.profile_path = castJSON.optString("profile_path");
                                movie.mCasts.add(cast);
                            }
                        }
                        JSONArray crewJSONArray = creditsJSON.optJSONArray("crew");
                        //Getting crew member
                        for (int i = 0; i < crewJSONArray.length(); i++) {
                            JSONObject crewJSON = crewJSONArray.optJSONObject(i);
                            if (crewJSON != null) {
                                if (movie.mCrew == null) movie.mCrew = new ArrayList<Crew>();
                                Crew crew = new Crew();
                                crew.job = crewJSON.optString("job");
                                crew.credit_id = crewJSON.optString("credit_id");
                                crew.id = crewJSON.optInt("id");
                                crew.name = crewJSON.optString("name");
                                crew.department = crewJSON.optString("department");
                                crew.profile_path = crewJSON.optString("profile_path");
                                movie.mCrew.add(crew);
                            }
                        }
                        if (movie.mCasts != null || movie.mCrew != null) {
                            movie.movieInfosCompleted = true;
                        }
                        return movie;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Movie movie) {
                super.onPostExecute(movie);
                if (movie != null) {
                    listener.onRetrieveMovieCreditsCompleted(movie);
                } else {
                    listener.onRetrieveMovieCreditsError("Error");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *  @param theater
     * @param listener
     */
    public void retrieveShowTimesTheaterInfos(final Context context, final MovieTheater theater, final OnRetrieveTheaterShowTimeInfoCompleted listener) {
        new AsyncTask<Void, Void, LinkedHashMap<Movie, ArrayList<ShowTime>>>() {

            @Override
            protected LinkedHashMap<Movie, ArrayList<ShowTime>> doInBackground(Void... params) {
                Document doc = null;
                String theaterUrl;
                if (theater.mId != null && theater.mId.length() > 0) {
                    theaterUrl = URL_API_THEATER + theater.mId;
                } else {
                    theaterUrl = URL_API_MOVIE_THEATERS + "&q=" + theater.mName;
                }

                try {
                    doc = Jsoup.connect(theaterUrl).get();
                    return createMoviesDatasetForTheater(context, theater, doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
                super.onPostExecute(dataset);
                if (dataset != null) {
                    listener.onRetrieveTheaterShowTimeInfoCompleted(dataset);
                } else {
                    listener.onRetrieveTheaterShowTimeInfoError("Unable to get theater infos");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param doc
     * @return
     */
    private LinkedHashMap<Movie, ArrayList<ShowTime>> createMoviesDatasetForTheater(Context context, MovieTheater theater, Document doc) {
        LinkedHashMap<Movie, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();
        Elements moviesElements = doc.getElementsByClass("movie");
        LinkedHashMap<String, Movie> cachedMovies  =
                (LinkedHashMap<String, Movie>)ApplicationUtils.getDataInCache(context, ApplicationUtils.MOVIES_FILE_NAME);
        for (Element movieElement : moviesElements) {
            String movieLink = movieElement.getElementsByTag("a").get(0).attr("href");
            String movieId = movieLink.split("mid=")[movieLink.split("mid=").length - 1];
            Movie movie = new Movie();
            if (cachedMovies != null && cachedMovies.containsKey(movieId)) {
                movie = cachedMovies.get(movieId);
            } else {
                movie.duration_time = movieElement.getElementsByClass("info").get(0).text().split("-")[0];
                movie.infos_g = movieElement.getElementsByClass("info").get(0).text();
                movie.title = movieElement.getElementsByClass("name").get(0).text();
            }

            Element showTimesElement = movieElement.getElementsByClass("times").get(0);
            ArrayList<ShowTime> sts = null;

            for (Element timeSpan : showTimesElement.getElementsByTag("span")) {
                //Comparison between the current time and the showtime
                String showTime = timeSpan.text().replaceAll("\\s", "");
                if (showTime.length() > 0) {
                    if (sts == null) {
                        sts = new ArrayList<ShowTime>();
                    }
                    int timeRemaining = ApplicationUtils.getTimeRemaining(showTime);

                    ShowTime st = new ShowTime();
                    st.mMovieId = movie.title;
                    st.mTheaterId = theater.mId;
                    st.mShowTimeStr = showTime;
                    st.mTimeRemaining = timeRemaining;
                    st.mId = st.mMovieId + st.mTheaterId + st.mShowTimeStr;
                    sts.add(st);
                }
            }
            dataset.put(movie, sts);
        }

        return dataset;
    }

    /**
     *
     * @param context
     * @param movie
     * @param listener
     */
    public void retrieveShowTimesMovieInfos(final Context context, final Location location, final Movie movie, final OnRetrieveMovieShowTimesCompleted listener) {
        new AsyncTask<Void, Void, LinkedHashMap<MovieTheater, ArrayList<ShowTime>>>() {

            @Override
            protected LinkedHashMap<MovieTheater, ArrayList<ShowTime>> doInBackground(Void... params) {

                String movieShowtimesUrl = URL_API_MOVIE_THEATERS + location.getLatitude() + "," + location.getLongitude();

                if (movie.id_g != null && movie.id_g.length() > 0) {
                    movieShowtimesUrl += "&mid=" + movie.id_g;
                } else {
                    movieShowtimesUrl += "&q=" + Uri.encode(movie.title).replaceAll("\\s", "+");
                }

                Document doc;
                try {
                    doc = Jsoup.connect(movieShowtimesUrl).get();
                    return createTheatersDatasetForMovie(context, movie, doc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset) {
                super.onPostExecute(dataset);
                if (dataset != null) {
                    listener.onRetrieveMovieShowTimesCompleted(dataset);
                } else {
                    listener.onRetrieveMovieShowTimesError("Unable to get showtimes for this movie. Please try again later");
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     * @param context
     * @param movie
     * @param doc
     * @return
     */
    private LinkedHashMap<MovieTheater, ArrayList<ShowTime>> createTheatersDatasetForMovie(Context context, Movie movie, Document doc) {
        LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset = new LinkedHashMap<>();
        LinkedHashMap<String, MovieTheater> cachedTheaters  =
                (LinkedHashMap<String, MovieTheater>)ApplicationUtils.getDataInCache(context, ApplicationUtils.THEATERS_FILE_NAME);

        Elements theaterElements = doc.getElementsByClass("theater");
        for (Element theaterElement : theaterElements) {
            String theaterLink = theaterElement.getElementsByTag("a").get(0).attr("href");
            String theaterId = theaterLink.split("tid=")[theaterLink.split("tid=").length - 1];
            MovieTheater theater = new MovieTheater();

            if (cachedTheaters != null && cachedTheaters.containsKey(theaterId)) {
                theater = cachedTheaters.get(theaterId);
            } else {
                theater.mId = theaterId;
                theater.mAddress = theaterElement.getElementsByClass("address").get(0).text();
                theater.mName = theaterElement.getElementsByClass("name").get(0).text();
            }

            Element showTimesElement = theaterElement.getElementsByClass("times").get(0);
            ArrayList<ShowTime> sts = null;

            for (Element timeSpan : showTimesElement.getElementsByTag("span")) {
                //Comparison between the current time and the showtime
                String showTime = timeSpan.text().replaceAll("\\s", "");
                if (showTime.length() > 0) {
                    if (sts == null) {
                        sts = new ArrayList<ShowTime>();
                    }
                    int timeRemaining = ApplicationUtils.getTimeRemaining(showTime);
                    ShowTime st = new ShowTime();
                    st.mMovieId = movie.title;
                    st.mTheaterId = theater.mId;
                    st.mShowTimeStr = showTime;
                    st.mTimeRemaining = timeRemaining;
                    st.mId = st.mMovieId + st.mTheaterId + st.mShowTimeStr;
                    sts.add(st);
                }
            }
            dataset.put(theater, sts);
        }
        return dataset;
    }

}
