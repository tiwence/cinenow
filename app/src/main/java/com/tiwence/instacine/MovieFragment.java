package com.tiwence.instacine;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.tiwence.instacine.adapter.TheaterDistanceHelper;
import com.tiwence.instacine.listener.OnRetrieveMovieCreditsCompleted;
import com.tiwence.instacine.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.instacine.listener.OnRetrieveMovieMoreInfosCompleted;
import com.tiwence.instacine.listener.OnRetrieveMovieShowTimesCompleted;
import com.tiwence.instacine.model.Cast;
import com.tiwence.instacine.model.Crew;
import com.tiwence.instacine.model.Movie;
import com.tiwence.instacine.model.MovieTheater;
import com.tiwence.instacine.model.ShowTime;
import com.tiwence.instacine.model.ShowTimesFeed;
import com.tiwence.instacine.utils.ApiUtils;
import com.tiwence.instacine.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by temarill on 02/02/2015.
 */
public class MovieFragment extends android.support.v4.app.Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private View mRootView;
    private ImageView mBackdropView;
    private ImageView mPosterView;
    private TextView mMovieTitleView;
    private Movie mCurrentMovie;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private ArrayList<ShowTime> mNextShowTimes;
    private TextView mMovieOverview;
    private TextView mMovieAverageView;
    private TextView mDurationView;
    private TextView mKindView;
    private FloatingActionButton mFavoritesButton;
    private SwipeRefreshLayout mSwipeRefresh;

    private boolean mShowTimesFullyLoaded = false;


    private LinkedHashMap<MovieTheater, ArrayList<ShowTime>> mShowtimeDataset;
    private ArrayList<Movie> mFavoritesMovies;

    private Location mLocation;

    private boolean needToGetPosterPath;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        mFavoritesMovies = (ArrayList<Movie>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
        if (mFavoritesMovies == null)
            mFavoritesMovies = new ArrayList<>();

        if (getArguments().getString("movie_id") != null) {
            mCurrentMovie = getResult().mMovies.get(getArguments().getString("movie_id"));
        }

        mLocation = ((FeedActivity)getActivity()).getLocation();

        configureView();
        requestMovieInfos();

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        getResult().filterNewNextShowTimes();
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(false);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
        ((FeedActivity)getActivity()).getMActionBar().setElevation(0);
        ((FeedActivity)getActivity()).getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_transparent));
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(false);
    }

    public ShowTimesFeed getResult() {
        if (getActivity() != null) {
            return ((FeedActivity) getActivity()).getResults();
        }
        return null;
    }

    /**
     *
     */
    private void configureView() {
        //Getting UI element
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshMovie);
        mBackdropView = (ImageView) mRootView.findViewById(R.id.backdropImageView);
        mMovieTitleView = (TextView) mRootView.findViewById(R.id.movieTitleTextView);
        mMovieOverview = (TextView) mRootView.findViewById(R.id.movieOverviewTextView);
        mMovieAverageView = (TextView) mRootView.findViewById(R.id.voteAverageTextView);
        mPosterView = (ImageView) mRootView.findViewById(R.id.moviePosterImageView);
        mDurationView = (TextView) mRootView.findViewById(R.id.movieDurationTextView);
        mKindView = (TextView) mRootView.findViewById(R.id.movieKindTextView);
        mFavoritesButton = (FloatingActionButton) mRootView.findViewById(R.id.favoritesFloatingButton);

        //Setting element
        mMovieTitleView.setText(mCurrentMovie.title);
        mMovieAverageView.setText(getString(R.string.vote_average) + " " + String.valueOf(mCurrentMovie.vote_average) + "/10");
        mMovieOverview.setText(mCurrentMovie.overview);
        mDurationView.setText(getString(R.string.duration) + " " + mCurrentMovie.duration_time);

        if(mFavoritesMovies.contains(mCurrentMovie)) {
            mFavoritesButton.setIcon(R.drawable.ic_remove_favorite);
        } else {
            mFavoritesButton.setIcon(R.drawable.ic_add_favorite);
        }
        mFavoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFavoritesMovies.contains(mCurrentMovie)) {
                    mFavoritesMovies.remove(mCurrentMovie);
                    mFavoritesButton.setIcon(R.drawable.ic_add_favorite);
                    Toast.makeText(getActivity(), String.format(getString(R.string.remove_favorite_movie), mCurrentMovie.title),
                            Toast.LENGTH_LONG).show();
                } else {
                    mFavoritesMovies.add(mCurrentMovie);
                    mFavoritesButton.setIcon(R.drawable.ic_remove_favorite);
                    Toast.makeText(getActivity(), String.format(getString(R.string.add_favorite_movie), mCurrentMovie.title),
                            Toast.LENGTH_LONG).show();
                }
                ApplicationUtils.saveDataInCache(getActivity(), mFavoritesMovies, ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
            }
        });

        mSwipeRefresh.setOnRefreshListener(this);

        displayPosterAndBackdrop();
        displayNextShowTimes();
        displayShowTimes();
    }

    private void displayPosterAndBackdrop() {
        //Poster
        if (mCurrentMovie.poster_path != null && mCurrentMovie.poster_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mPosterView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.title)
                && mCachedMovies.get(mCurrentMovie.title).poster_path != null
                && mCachedMovies.get(mCurrentMovie.title).poster_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.title).poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        } else {
            needToGetPosterPath = true;
        }

        //Backdrop
        if (mCurrentMovie.backdrop_path != null && mCurrentMovie.backdrop_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.title)
                && mCachedMovies.get(mCurrentMovie.title).backdrop_path != null
                && mCachedMovies.get(mCurrentMovie.title).backdrop_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.title).backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        } else {
            needToGetPosterPath = true;
        }
    }

    /**
     *
     */
    private void displayNextShowTimes() {
        //getResult().filterNewNextShowTimes();
        mNextShowTimes = getResult().getNextShowtimesByMovieId(mCurrentMovie.title);
        LinearLayout nextShowTimesLayout = (LinearLayout) mRootView.findViewById(R.id.nextShowTimesLayout);
        nextShowTimesLayout.removeAllViews();
        if (mNextShowTimes != null) {
            for (int i = 0; i < mNextShowTimes.size(); i++) {
                ShowTime s = mNextShowTimes.get(i);
                TextView tv = new TextView(getActivity());
                tv.setTextColor(mMovieOverview.getCurrentTextColor());
                tv.setTextSize(15.0f);
                if (i == 0) {
                    tv.setPadding(5, 0, 5, 5);
                } else {
                    tv.setPadding(5, 5, 5, 5);
                }
                tv.setText(Html.fromHtml(ApplicationUtils.getTimeString(s.mTimeRemaining)
                        + " <strong>" + getResult().mTheaters.get(s.mTheaterId).mName + "</strong>"));
                tv.setTag(s);
                nextShowTimesLayout.addView(tv);
                tv.setOnClickListener(this);
            }
        }
    }

    /**
     *
     */
    private void displayShowTimes() {
        mShowtimeDataset = getResult().getShowTimesByTheatersForMovie(mCurrentMovie.title);

        if (mShowtimeDataset != null && mShowtimeDataset.size() > 0) {

            List<Map.Entry<MovieTheater, ArrayList<ShowTime>>> entries = new ArrayList<>(mShowtimeDataset.entrySet());
            Collections.sort(entries, new Comparator<Map.Entry<MovieTheater, ArrayList<ShowTime>>>() {
                @Override
                public int compare(Map.Entry<MovieTheater, ArrayList<ShowTime>> lhs, Map.Entry<MovieTheater, ArrayList<ShowTime>> rhs) {
                    if (lhs.getKey().mDistance < rhs.getKey().mDistance) {
                        return -1;
                    } else if (lhs.getKey().mDistance > rhs.getKey().mDistance){
                        return 1;
                    }
                    return 0;
                }
            });

            LinkedHashMap<MovieTheater, ArrayList<ShowTime>> sortedMap = new LinkedHashMap<>();
            for (Map.Entry<MovieTheater, ArrayList<ShowTime>> entry : entries) {
                sortedMap.put(entry.getKey(), entry.getValue());
            }
            mShowtimeDataset = sortedMap;

            for (Map.Entry<MovieTheater, ArrayList<ShowTime>> entry : mShowtimeDataset.entrySet()) {
                LinearLayout theaterShowTimesLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.theater_for_movie_item, null);
                TextView theaterNameTextView = (TextView) theaterShowTimesLayout.findViewById(R.id.theaterNameForMovieText);
                if (entry.getKey().mDistance >= 1000) {
                    final WeakReference<TextView> distanceRef = new WeakReference<TextView>(theaterNameTextView);
                    new TheaterDistanceHelper(mLocation, getResult(), distanceRef, true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, entry.getKey());
                } else {
                    theaterNameTextView.setText(entry.getKey().mName + " (" + entry.getKey().mDistance + " km)");
                }
                theaterNameTextView.setOnClickListener(this);
                ShowTime tempSt = new ShowTime();
                tempSt.mTheaterId = entry.getKey().mName;
                theaterNameTextView.setTag(tempSt);

                for (ShowTime st : entry.getValue()) {
                    TextView stv = new TextView(getActivity());
                    stv.setTextSize(16.0f);
                    stv.setText(st.mShowTimeStr);
                    stv.setPadding(8, 5, 8, 5);
                    stv.setTag(st);
                    stv.setOnClickListener(this);
                    ((ViewGroup) theaterShowTimesLayout.findViewById(R.id.showtimesForMovieLayout)).addView(stv);
                }

                ((ViewGroup) mRootView.findViewById(R.id.allShowTimesLayout)).addView(theaterShowTimesLayout);
                theaterShowTimesLayout.requestLayout();
            }
            mSwipeRefresh.setRefreshing(false);
        } else {
            requestMovieShowtimes();
        }
    }

    /**
     *
     */
    private void requestMovieShowtimes() {
        mSwipeRefresh.setRefreshing(true);
        ApiUtils.instance().retrieveShowTimesMovieInfos(getActivity(), mLocation, mCurrentMovie, new OnRetrieveMovieShowTimesCompleted() {
            @Override
            public void onRetrieveMovieShowTimesCompleted(LinkedHashMap<MovieTheater, ArrayList<ShowTime>> dataset) {
                if (getResult() != null) {
                    getResult().addNewMovieInfos(mCurrentMovie, dataset);
                    configureView();
                }
            }

            @Override
            public void onRetrieveMovieShowTimesError(String errorMessage) {
                mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    /**
     *
     */
    private void requestMovieInfos() {
        if (mCurrentMovie.id == 0) {
            ApiUtils.instance().retrieveMovieInfo(mCurrentMovie, new OnRetrieveMovieInfoCompleted() {
                @Override
                public void onRetrieveMovieInfoCompleted(Movie movie) {
                    mCurrentMovie = movie;
                    displayMovieOverview();
                    if (needToGetPosterPath) {
                        needToGetPosterPath = false;
                        displayPosterAndBackdrop();
                    }
                    ApplicationUtils.saveDataInCache(getActivity(), getResult().mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                }

                @Override
                public void onRetrieveMovieError(String message) {

                }
            });
        } else {
            ApiUtils.instance().retrieveMoreMovieInfos(mCurrentMovie, new OnRetrieveMovieMoreInfosCompleted() {
                @Override
                public void onRetrieveMovieMoreInfosCompleted(Movie movie) {
                    mCurrentMovie = movie;
                    if (getResult() != null && getResult().mMovies != null
                            && mCurrentMovie != null && mCurrentMovie.title != null) {
                        getResult().mMovies.put(mCurrentMovie.title, mCurrentMovie);
                        displayMovieOverview();
                        ApplicationUtils.saveDataInCache(getActivity(), getResult().mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                    }
                }
                @Override
                public void onRetrieveMovieMoreInfosError(String errorMessage) {

                }
            });

            if ((mCurrentMovie.mCasts == null || mCurrentMovie.mCasts.size() == 0)
                    && (mCurrentMovie.mCrew == null || mCurrentMovie.mCrew.size() == 0)) {
                ApiUtils.instance().retrieveMovieCredits(mCurrentMovie, new OnRetrieveMovieCreditsCompleted() {
                    @Override
                    public void onRetrieveMovieCreditsCompleted(Movie movie) {
                        mCurrentMovie = movie;
                        getResult().mMovies.put(mCurrentMovie.title, mCurrentMovie);
                        displayCreditsInfos();
                        ApplicationUtils.saveDataInCache(getActivity(), getResult().mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                    }

                    @Override
                    public void onRetrieveMovieCreditsError(String message) {
                        Log.e("MovieFragment", message);
                    }
                });
            } else {
                displayCreditsInfos();
            }
        }

    }

    /**
     *
     */
    private void displayCreditsInfos() {
        int maxIndex = 3;
        if (mCurrentMovie.mCasts != null) {
            String castStr = "";
            if (mCurrentMovie.mCasts.size() < 3)
                maxIndex = mCurrentMovie.mCasts.size() - 1;
            for (int i = 0; i < maxIndex; i++) {
                Cast cast = mCurrentMovie.mCasts.get(i);
                castStr += cast.name;
                if (i < maxIndex - 1) castStr += ", ";
            }
            ((TextView)mRootView.findViewById(R.id.movieCastTextView)).setText(getString(R.string.with) + " " + castStr);
        } else {
            mRootView.findViewById(R.id.movieCastTextView).setVisibility(View.GONE);
        }
        if (mCurrentMovie.mCrew != null) {
            String crewStr = "";
            for (Crew crew : mCurrentMovie.mCrew) {
                if (crew.job.equals("Director")) {
                    crewStr += crew.name + ", ";
                }
            }
            if (!crewStr.equals("")) {
                crewStr = crewStr.substring(0, crewStr.length() - 2);
                ((TextView)mRootView.findViewById(R.id.movieDirectorNameTextView)).setText(getString(R.string.from) + " " + crewStr);
                return;
            }
        }
        mRootView.findViewById(R.id.movieDirectorNameTextView).setVisibility(View.GONE);
    }

    /**
     *
     */
    private void displayMovieOverview() {
        ((TextView)mRootView.findViewById(R.id.movieOverviewTextView)).setText(mCurrentMovie.overview);
        if (mCurrentMovie.kind != null && mCurrentMovie.kind.length() > 0) {
            mKindView.setText(getString(R.string.kind) + " " + mCurrentMovie.kind);
        }
        if (mCurrentMovie.kinds != null) {
            String genres = "";
            for (String genre : mCurrentMovie.kinds) {
                genres += genre + ", ";
            }
            if (genres.length() > 0) {
                genres = genres.substring(0, genres.length() - 2);
                mKindView.setText(getString(R.string.kind) + " " + genres);
                mKindView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            ((FeedActivity) getActivity()).showTheaterChoiceFragment((ShowTime)v.getTag());
        }
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        configureView();
    }
}
