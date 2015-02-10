package com.tiwence.cinenow;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.tiwence.cinenow.adapter.SearchResultAdapter;
import com.tiwence.cinenow.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by temarill on 27/01/2015.
 */
public class FeedActivity extends ActionBarActivity implements OnRetrieveQueryCompleted, LocationListener, ActionBar.OnNavigationListener {

    //ActionBar stuff
    private AutoCompleteTextView mEditSearch;
    private SpinnerAdapter mMoviesKindAdapter;

    private LocationManager mLocationManager;
    private Location mLocation;
    private boolean mIsFirstLocation = true;
    private boolean mNewLocationFound = false;

    private SwipeFlingAdapterView mFeedContainer;
    //private ShowTimesAdapter mFeedAdapter;

    private ShowTimesFeed mResult;
    private LinkedHashMap<String, Movie> mMoviesCached;
    private LinkedHashMap<String, MovieTheater> mTheatersCached;

    private TheatersFragment mTheatersFragment;
    private MoviesFeedFragment mMoviesFeedFragment;

    private ActionBar mActionBar;

    private static final int REFRESH_LOCATION_TIMEOUT = 15000;

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
                    ApiUtils.instance().retrieveQueryInfo(mLocation, queryName, FeedActivity.this);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle("");

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(getProviderName());

        mMoviesCached = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(this, ApplicationUtils.MOVIES_FILE_NAME);
        mTheatersCached = (LinkedHashMap<String, MovieTheater>) ApplicationUtils.getDataInCache(this, ApplicationUtils.THEATERS_FILE_NAME);

        refreshLocation();
    }

    static void makeToast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        mEditSearch = (AutoCompleteTextView) MenuItemCompat.getActionView(searchItem);
        mEditSearch.addTextChangedListener(textWatcher);
        mEditSearch.setHint(R.string.search_placeholder);

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mEditSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
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

        return true;
    }

    public ShowTimesFeed getResults() {
        return this.mResult;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                if (mTheatersFragment != null && mTheatersFragment.isVisible())
                    mTheatersFragment.getActivity().getSupportFragmentManager().popBackStack();
                break;
            case R.id.action_settings:
                return true;
            case R.id.action_refresh:
                refresh();
                break;
            case R.id.action_theaters:
                mTheatersFragment = new TheatersFragment();
                Bundle b = new Bundle();
                b.putSerializable("result", mResult);
                b.putSerializable("kindIndex", mActionBar.getSelectedNavigationIndex());
                mTheatersFragment.setArguments(b);

                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, mTheatersFragment)
                        .addToBackStack(null)
                        .commit();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     *
     */
    public void refreshLocation() {
        mLocationManager.requestLocationUpdates(getProviderName(), 0, 0, this);
        if (mIsFirstLocation) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mNewLocationFound && mLocation != null) {
                        makeToast(FeedActivity.this, "Location timeout, using last known location");
                        requestData();
                        //refreshTheatersFragment();
                        mLocationManager.removeUpdates(FeedActivity.this);
                    } else if (mLocation == null) {
                        makeToast(FeedActivity.this, "Unable to detect your location. Please try again later.");
                    }
                }
            }, REFRESH_LOCATION_TIMEOUT);
        }
    }

    /**
     * Get provider name.
     *
     * @return Name of best suiting provider.
     */
    private String getProviderName() {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_COARSE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);

        String provider = mLocationManager.getBestProvider(c, true);
        Log.d("PROVIDER", provider);
        return provider;
    }

    /**
     *
     */
    private void requestData() {
        ApiUtils.instance().retrieveMovieShowTimeTheaters(this, mLocation, new OnRetrieveShowTimesCompleted() {
            @Override
            public void onRetrieveShowTimesCompleted(ShowTimesFeed result) {
                mResult = result;
                ApplicationUtils.saveDataInCache(FeedActivity.this, mResult.mTheaters, ApplicationUtils.THEATERS_FILE_NAME);
                updateFilters();
                if (mMoviesFeedFragment != null)
                    mMoviesFeedFragment.updateDataList(mResult);
                if (mTheatersFragment != null)
                    mTheatersFragment.updateDataList(mResult, 0);
                if (mMoviesFeedFragment == null && mTheatersFragment == null)
                    displayMoviesFeed();

                ApiUtils.instance().retrieveMoviesInfo(FeedActivity.this, result.mMovies, new OnRetrieveMoviesInfoCompleted() {
                    @Override
                    public void onProgressMovieInfoCompleted(Movie movie) {
                        //mResult.mMovies.put(movie.id_g, movie);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesInfoCompleted(LinkedHashMap<String, Movie> movies) {
                        Log.d("MOVIE SEARCH", "All movies get");
                        mResult.mMovies = movies;
                        ApplicationUtils.saveDataInCache(FeedActivity.this, mResult.mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesError(String message) {
                        Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRetrieveShowTimesError(String errorMessage) {
                Toast.makeText(FeedActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     *
     */
    private void displayMoviesFeed() {
        setContentView(R.layout.activity_main);
        mMoviesFeedFragment = new MoviesFeedFragment();
        Bundle b = new Bundle();
        b.putSerializable("result", mResult);
        mMoviesFeedFragment.setArguments(b);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainContainer, mMoviesFeedFragment)
                .commit();
    }

    /**
     *
     */
    private void updateFilters() {
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mResult.mMovieKinds = getMovieKindsList(mResult.mNextMovies);
        Log.d("FeedActivity", "Kinds : " + mResult.mMovieKinds.toString());
        mMoviesKindAdapter = new ArrayAdapter(this, R.layout.kind_item, mResult.mMovieKinds);
        mActionBar.setListNavigationCallbacks(mMoviesKindAdapter, this);
    }

    /**
     *
     * @param nextMovies
     * @return
     */
    private ArrayList<String> getMovieKindsList(ArrayList<Movie> nextMovies) {
        ArrayList<String> movieKinds = new ArrayList<>();
        movieKinds.add(getString(R.string.all));
        for (Movie m : nextMovies) {
            if (!movieKinds.contains(m.kind) && m.kind != null)
                movieKinds.add(m.kind);
        }
        return movieKinds;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d("Location", location.getLatitude() + ", " + location.getLongitude());
            mLocation = location;
        }
        if (mIsFirstLocation) {
            mNewLocationFound = true;
            mIsFirstLocation = false;
            Toast.makeText(this, getString(R.string.location_done), Toast.LENGTH_LONG).show();
            this.requestData();
            //this.refreshTheatersFragment();
        }
        mLocationManager.removeUpdates(this);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onRetrieveQueryDataset(ShowTimesFeed stf) {

    }

    @Override
    public void onRetrieveQueryCompleted(final List<Object> dataset) {
        if (dataset != null && dataset.size() > 0) {
            SearchResultAdapter searchedAppsAdapter = new SearchResultAdapter(
                    getApplicationContext(), R.layout.spinner_search_item, dataset);
            mEditSearch.setAdapter(searchedAppsAdapter);
            mEditSearch.showDropDown();

            mEditSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position < dataset.size()) {
                        if (dataset.get(position) != null) {
                            if (dataset.get(position) instanceof Movie) {
                                Movie movie = (Movie) dataset.get(position);
                                mEditSearch.setText("");
                                MovieFragment mf = new MovieFragment();
                                Bundle b = new Bundle();
                                if (mResult.mMovies.containsKey(movie.id_g)) {
                                    b.putString("movie_id", movie.id_g);
                                } else {
                                    b.putSerializable("movie", movie);
                                }
                                mf.setArguments(b);
                                getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                        .replace(R.id.mainContainer, mf)
                                        .addToBackStack(null)
                                        .commit();

                            } else if (dataset.get(position) instanceof MovieTheater) {
                                MovieTheater theater = (MovieTheater) dataset.get(position);
                                mEditSearch.setText(theater.mName);
                            }

                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRetrieveQueryError(String errorMessage) {

    }

    @Override
    public boolean onNavigationItemSelected(int i, long l) {
        makeToast(this, "Selected " + i);
        filterNextMoviesByKind(i);
        return false;
    }

    /**
     *
     * @param kindIndex
     */
    public void filterNextMoviesByKind(int kindIndex) {
        if (mMoviesFeedFragment != null) {
            mMoviesFeedFragment.filterFragment(kindIndex);
        }
        if (mTheatersFragment != null) {
            mTheatersFragment.filterFragment(kindIndex);
        }
    }

    /**
     *
     */
    public void refresh() {
        mIsFirstLocation = true;
        refreshLocation();
    }

    /**
     *
     * @return
     */
    public ActionBar getMActionBar() {
        return mActionBar;
    }

    /**
     *
     * @return
     */
    public Location getLocation() {
        return mLocation;
    }
}
