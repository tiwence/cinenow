package com.tiwence.cinenow;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.adapter.SearchResultAdapter;
import com.tiwence.cinenow.listener.OnRetrieveMovieCreditsCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMovieMoreInfosCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMovieShowTimesCompleted;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.model.Cast;
import com.tiwence.cinenow.model.Crew;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Created by temarill on 02/02/2015.
 */
public class MovieFragment extends android.support.v4.app.Fragment implements OnRetrieveQueryCompleted {

    private View mRootView;
    private ImageView mBackdropView;
    private ImageView mPosterView;
    private TextView mMovieTitleView;
    private Movie mCurrentMovie;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private ArrayList<ShowTime> mNextShowTimes;
    private TextView mMovieOverview;
    private TextView mMovieAverageView;

    private AutoCompleteTextView mEditSearch;
    private MenuItem mSearchItem;

    private LinkedHashMap<MovieTheater, ArrayList<ShowTime>> mShowtimeDataset;

    private Location mLocation;

    private boolean needToGetPosterPath;

    /**
     * Text watcher used to search movies or theaters
     */
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String queryName = mEditSearch.getText().toString()
                    .toLowerCase(Locale.getDefault()).trim();

            if (queryName.length() > 2) {
                if (mLocation != null) {
                    ApiUtils.instance().retrieveQueryInfo(mLocation, queryName, MovieFragment.this);
                } else {
                    //TODO ?
                }
            }
        }
        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                  int arg3) {
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);

        if (getArguments().getString("movie_id") != null) {
            mCurrentMovie = getResult().mMovies.get(getArguments().getString("movie_id"));
        }

        mLocation = ((FeedActivity)getActivity()).getLocation();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                configureView();
                requestMovieInfos();
            }
        }, 100);

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

    /**
     *
     */
    private void configureView() {
        //Getting UI element
        mBackdropView = (ImageView) mRootView.findViewById(R.id.backdropImageView);
        mMovieTitleView = (TextView) mRootView.findViewById(R.id.movieTitleTextView);
        mMovieOverview = (TextView) mRootView.findViewById(R.id.movieOverviewTextView);
        mMovieAverageView = (TextView) mRootView.findViewById(R.id.voteAverageTextView);
        mPosterView = (ImageView) mRootView.findViewById(R.id.moviePosterImageView);

        //Setting element
        mMovieTitleView.setText(mCurrentMovie.title);
        mMovieAverageView.setText("" + mCurrentMovie.vote_average + "/10");
        mMovieOverview.setText(mCurrentMovie.overview);

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
                nextShowTimesLayout.addView(tv);
            }
        }
    }

    /**
     *
     */
    private void displayShowTimes() {
        mShowtimeDataset = getResult().getShowTimesByTheatersForMovie(mCurrentMovie.title);
        ((ViewGroup) mRootView.findViewById(R.id.allShowTimesLayout)).removeAllViews();

        if (mShowtimeDataset != null && mShowtimeDataset.size() > 0) {
            for (Map.Entry<MovieTheater, ArrayList<ShowTime>> entry : mShowtimeDataset.entrySet()) {
                LinearLayout theaterShowTimesLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.theater_for_movie_item, null);
                ((TextView) theaterShowTimesLayout.findViewById(R.id.theaterNameForMovieText)).setText(entry.getKey().mName);

                for (ShowTime st : entry.getValue()) {
                    TextView stv = new TextView(getActivity());
                    stv.setTextSize(16.0f);
                    stv.setText(st.mShowTimeStr);
                    stv.setPadding(8, 5, 8, 5);
                    ((ViewGroup) theaterShowTimesLayout.findViewById(R.id.showtimesForMovieLayout)).addView(stv);
                }

                ((ViewGroup) mRootView.findViewById(R.id.allShowTimesLayout)).addView(theaterShowTimesLayout);
                theaterShowTimesLayout.requestLayout();
            }
        } else {
            requestMovieShowtimes();
        }
    }

    /**
     *
     */
    private void requestMovieShowtimes() {
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
                    getResult().mMovies.put(mCurrentMovie.title, mCurrentMovie);
                    displayMovieOverview();
                    ApplicationUtils.saveDataInCache(getActivity(), getResult().mMovies, ApplicationUtils.MOVIES_FILE_NAME);
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
            ((TextView)mRootView.findViewById(R.id.movieCastTextView)).setText(castStr);
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
                ((TextView)mRootView.findViewById(R.id.movieDirectorNameTextView)).setText("De " + crewStr);
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_movie, menu);

        mSearchItem = menu.findItem(R.id.action_movie_search);
        mEditSearch = (AutoCompleteTextView) MenuItemCompat.getActionView(mSearchItem);
        mEditSearch.addTextChangedListener(textWatcher);
        mEditSearch.setHint(getString(R.string.search_placeholder));

        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mEditSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mEditSearch.setText("");
                mEditSearch.clearFocus();
                return true;
            }
        });

        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_theaters).setVisible(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                getFragmentManager().popBackStack();
                break;
        }

        return true;
    }

    @Override
    public void onRetrieveQueryDataset(ShowTimesFeed stf) {

    }

    @Override
    public void onRetrieveQueryCompleted(final List<Object> dataset) {
        if (dataset != null && dataset.size() > 0) {

            SearchResultAdapter searchedAppsAdapter = new SearchResultAdapter(
                    getActivity(), R.layout.spinner_search_item, dataset);
            mEditSearch.setAdapter(searchedAppsAdapter);
            mEditSearch.showDropDown();
            mEditSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position < dataset.size()) {
                        if (dataset.get(position) != null) {

                            MenuItemCompat.collapseActionView(mSearchItem);
                            if (dataset.get(position) instanceof Movie) {
                                Movie movie = (Movie) dataset.get(position);
                                mEditSearch.setText("");
                                MovieFragment mf = new MovieFragment();
                                Bundle b = new Bundle();
                                if (!getResult().mMovies.containsKey(movie.title)) {
                                    getResult().mMovies.put(movie.title, movie);
                                }
                                b.putString("movie_id", movie.title);
                                mf.setArguments(b);
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                        .replace(R.id.mainContainer, mf)
                                        .addToBackStack(null)
                                        .commit();
                            } else if (dataset.get(position) instanceof MovieTheater) {
                                MovieTheater theater = (MovieTheater) dataset.get(position);
                                mEditSearch.setText("");
                                TheaterFragment tf = new TheaterFragment();
                                Bundle b = new Bundle();
                                if (!getResult().mTheaters.containsKey(theater.mId)) {
                                    getResult().mTheaters.put(theater.mId, theater);
                                }
                                b.putString("theater_id", theater.mId);
                                tf.setArguments(b);
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                        .replace(R.id.mainContainer, tf)
                                        .addToBackStack(null)
                                        .commit();
                            }

                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRetrieveQueryError(String errorMessage) {
        //TODO
    }
}
