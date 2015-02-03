package com.tiwence.cinenow;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Created by temarill on 02/02/2015.
 */
public class MovieFragment extends android.support.v4.app.Fragment {

    private View mRootView;
    private ImageView mBackdropView;
    private ImageView mPosterView;
    private TextView mMovieTitleView;
    private Movie mCurrentMovie;
    private ShowTimesFeed mResult;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private TextView mMovieOverview;
    private TextView mMovieAverageView;

    private LinkedHashMap<MovieTheater, ArrayList<ShowTime>> mShowtimeDataset;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        mCurrentMovie = getResult().mMovies.get(getArguments().getString("movie_id"));

        configureView();
        requestMovieInfos();

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public ShowTimesFeed getResult() {
        if (getActivity() != null) {
            return ((FeedActivity) getActivity()).getResults();
        }
        return null;
    }

    private void configureView() {
        mBackdropView = (ImageView) mRootView.findViewById(R.id.backdropImageView);
        mMovieTitleView = (TextView) mRootView.findViewById(R.id.movieTitleTextView);
        mMovieOverview = (TextView) mRootView.findViewById(R.id.overviewText);
        mMovieAverageView = (TextView) mRootView.findViewById(R.id.voteAverageTextView);
        mPosterView = (ImageView) mRootView.findViewById(R.id.moviePosterImageView);

        mMovieTitleView.setText(mCurrentMovie.title);
        mMovieAverageView.setText("" + mCurrentMovie.vote_average + "/10");
        mMovieOverview.setText(mCurrentMovie.overview);

        if (mCurrentMovie.poster_path != null && mCurrentMovie.poster_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mPosterView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.id_g)
                && mCachedMovies.get(mCurrentMovie.id_g).poster_path != null
                && mCachedMovies.get(mCurrentMovie.id_g).poster_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.id_g).poster_path;
        }

        if (mCurrentMovie.backdrop_path != null && mCurrentMovie.backdrop_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.id_g)
                && mCachedMovies.get(mCurrentMovie.id_g).backdrop_path != null
                && mCachedMovies.get(mCurrentMovie.id_g).backdrop_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.id_g).backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        }

        displayShowTimes();
    }

    /**
     *
     */
    private void displayShowTimes() {
        mShowtimeDataset = getResult().getShowTimesByTheatersForMovie(mCurrentMovie.id_g);

        for (Map.Entry<MovieTheater, ArrayList<ShowTime>> entry : mShowtimeDataset.entrySet()) {
            LinearLayout theaterShowTimesLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.theater_for_movie_item, null);
            ((TextView) theaterShowTimesLayout.findViewById(R.id.theaterNameForMovieText)).setText(entry.getKey().mName);

            for (ShowTime st : entry.getValue()) {
                TextView stv = new TextView(getActivity());
                stv.setTextSize(16.0f);
                stv.setText(st.mShowTimeStr);
                stv.setPadding(5, 5, 5, 5);
                ((LinearLayout) theaterShowTimesLayout.findViewById(R.id.showtimesForMovieLayout)).addView(stv);
            }

            ((LinearLayout) mRootView.findViewById(R.id.allShowTimesLayout)).addView(theaterShowTimesLayout);
            theaterShowTimesLayout.requestLayout();
        }

    }

    private void requestMovieInfos() {

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_movie, menu);

        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_theaters).setVisible(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
    }

}
