package com.tiwence.cinenow2;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.tiwence.cinenow2.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow2.listener.OnSelectChoiceListener;
import com.tiwence.cinenow2.model.ShowTime;
import com.tiwence.cinenow2.utils.ApplicationUtils;
import com.tiwence.cinenow2.adapter.SearchResultAdapter;
import com.tiwence.cinenow2.listener.OnQueryResultClickListener;
import com.tiwence.cinenow2.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.MovieTheater;
import com.tiwence.cinenow2.model.ShowTimesFeed;
import com.tiwence.cinenow2.utils.ApiUtils;
import com.tiwence.cinenow2.utils.SearchLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by temarill on 27/01/2015.
 */
public class FeedActivity extends ActionBarActivity implements OnRetrieveQueryCompleted, LocationListener,
        ActionBar.OnNavigationListener, OnQueryResultClickListener, OnSelectChoiceListener {

    private static final long SPLASH_ANIM_DURATION = 500;
    private static final long LOADING_ANIM_DURATION = 1000;
    //ActionBar stuff
    private AutoCompleteTextView mEditSearch;
    private MenuItem mSearchItem;
    private SpinnerAdapter mMoviesKindAdapter;

    //private LocationManager mLocationManager;
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

    private Menu mMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.splash_screen);

        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.hide();
        mActionBar.setTitle("");
        mActionBar.setIcon(R.drawable.app_icon);

        mMoviesCached = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(this, ApplicationUtils.MOVIES_FILE_NAME);
        mTheatersCached = (LinkedHashMap<String, MovieTheater>) ApplicationUtils.getDataInCache(this, ApplicationUtils.THEATERS_FILE_NAME);
        mResult = (ShowTimesFeed) ApplicationUtils.getDataInCache(this, ApplicationUtils.SHOWTIMES_FEED_FILE_NAME);

        this.findViewById(R.id.splashTextViewAPI).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW);
                webIntent.setData(Uri.parse(ApplicationUtils.MOVIE_DB_API_DOC));
                startActivity(webIntent);
            }
        });

        loadingSplashAnimations();

        refreshLocation();
    }

    int[] animatedViews = new int[] {
            R.id.splashTextViewAppName,
            R.id.android_splash_icon,
            R.id.splashTextView
    };

    /**
     *
     */
    private void loadingSplashAnimations() {
        for (int i = 0; i < animatedViews.length; i++) {
            final int view_id = animatedViews[i];
            final int index = i;
            Handler splashHandler = new Handler();
            splashHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(findViewById(view_id), "alpha", 0f, 1f);
                    animator.setDuration(SPLASH_ANIM_DURATION);
                    animator.start();
                    if (index == (animatedViews.length - 1)) {
                        ObjectAnimator animator1 = ObjectAnimator.ofFloat(findViewById(R.id.splashTextView), "alpha", 0f, 1f);
                        animator1.setDuration(LOADING_ANIM_DURATION);
                        animator1.setRepeatMode(ValueAnimator.REVERSE);
                        animator1.setRepeatCount(ValueAnimator.INFINITE);
                        animator1.start();
                    }
                }
            }, (i + 1) * SPLASH_ANIM_DURATION);
        }

    }

    /*@Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("results", mResult);
        super.onSaveInstanceState(outState);
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        /*if (mResult != null && mMoviesFeedFragment == null) {
            displayResult();
        }*/
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mResult = (ShowTimesFeed) savedInstanceState.get("results");
            if (mResult != null) {
                displayResult();
            }
        }
    }

    static void makeToast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMenu = menu;

        mSearchItem = menu.findItem(R.id.action_search);
        mEditSearch = (AutoCompleteTextView) MenuItemCompat.getActionView(mSearchItem);
        mEditSearch.addTextChangedListener(textWatcher);
        mEditSearch.setHint(R.string.search_placeholder);

        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
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

    public Menu getMenu() { return this.mMenu; }

    /**
     *
     * @return
     */
    public Fragment getActiveFragment() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            return null;
        }
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        String tag = fragments.get(getSupportFragmentManager().getBackStackEntryCount() - 1).getTag();
        return getSupportFragmentManager().findFragmentByTag(tag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                Fragment activeFragment = getActiveFragment();
                if (activeFragment != null) {
                    activeFragment.getActivity().getSupportFragmentManager().popBackStack();
                }
                break;
            case R.id.action_about:
                displayAbout();
                return true;
            case R.id.action_refresh:
                //refresh();
                if (mTheatersFragment != null && mTheatersFragment.isVisible()) {
                    mTheatersFragment.onRefresh();
                } else {
                    refresh();
                }
                break;
            case R.id.action_favorites_movies:
                FavoritesMoviesFragment fm = new FavoritesMoviesFragment();
                this.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, fm, "favorites_movies")
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.action_favorites_theaters:
                FavoritesTheatersFragment ft = new FavoritesTheatersFragment();
                this.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, ft, "favorites_theaters")
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
    private void displayAbout() {
        AboutFragment aboutFragment = new AboutFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.mainContainer, aboutFragment, "theaters")
                .addToBackStack(null)
                .commit();
    }

    /**
     *
     */
    protected void displayTheatersFragment() {
        mTheatersFragment = new TheatersFragment();
        Bundle b = new Bundle();
        b.putSerializable("result", mResult);
        b.putSerializable("kindIndex", mActionBar.getSelectedNavigationIndex());
        mTheatersFragment.setArguments(b);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.mainContainer, mTheatersFragment, "theaters")
                .addToBackStack(null)
                .commit();
    }

    /**
     *
     */
    public void refreshLocation() {
        ((TextView)findViewById(R.id.splashTextView)).setText(R.string.location);
        SearchLocation myLocation = new SearchLocation();
        boolean isLocationEnabled = myLocation.getLocation(this, new SearchLocation.LocationResult() {
            @Override
            public void gotLocation(Location location) {
                if (mIsFirstLocation) {
                    if (location != null) {
                        mLocation = location;
                    }
                    if (mIsFirstLocation) {
                        mNewLocationFound = true;
                        mIsFirstLocation = false;
                        //Toast.makeText(FeedActivity.this, getString(R.string.location_done), Toast.LENGTH_LONG).show();
                        requestData();
                        ((TextView)findViewById(R.id.splashTextView)).setText(R.string.loading);
                        //this.refreshTheatersFragment();
                    }
                }
            }
        });

        if (!isLocationEnabled) {
            AlertDialog.Builder locationAlert = new AlertDialog.Builder(this);
            locationAlert.setTitle(R.string.informations);
            locationAlert.setMessage(getString(R.string.please_turn_on_gps));
            locationAlert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    FeedActivity.this.startActivity(myIntent);
                }
            });
            locationAlert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    //TODO
                }
            });
            locationAlert.show();
        }
    }

    /**
     *
     */
    private void requestData() {
        ApiUtils.instance().retrieveMovieShowTimeTheaters(this, mLocation, new OnRetrieveShowTimesCompleted() {
            @Override
            public void onRetrieveShowTimesCompleted(ShowTimesFeed result) {
                mResult = result;
                displayResult();
            }

            @Override
            public void onRetrieveShowTimesError(String errorMessage) {
                Toast.makeText(FeedActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                if (mResult == null) mResult = new ShowTimesFeed();
                if (mResult.mTheaters == null) mResult.mTheaters = new LinkedHashMap<String, MovieTheater>();
                if (mResult.mMovies == null) mResult.mMovies = new LinkedHashMap<String, Movie>();
                if (mResult.mNextShowTimes == null) mResult.mNextShowTimes = new ArrayList<ShowTime>();
                if (mResult.mShowTimes == null) mResult.mShowTimes = new LinkedHashMap<String, ShowTime>();
                if (mResult.mNextMovies == null) mResult.mNextMovies = new ArrayList<Movie>();
                displayResult();
            }
        });
    }

    /**
     *
     */
    private void displayResult() {
        ApplicationUtils.saveDataInCache(FeedActivity.this, mResult.mTheaters, ApplicationUtils.THEATERS_FILE_NAME);
        ApplicationUtils.saveDataInCache(FeedActivity.this, mResult, ApplicationUtils.SHOWTIMES_FEED_FILE_NAME);
        updateFilters();
        if (mMoviesFeedFragment != null)
            mMoviesFeedFragment.updateDataList();
        if (mTheatersFragment != null)
            mTheatersFragment.updateDataList(0);
        if (mMoviesFeedFragment == null && mTheatersFragment == null)
            displayMoviesFeed();

        //For udpating showtimes time remaining
        launchingTimerTask();
        requestMoviesData();
    }

    private void requestMoviesData() {
        ApiUtils.instance().retrieveMoviesInfo(FeedActivity.this, mResult.mMovies, new OnRetrieveMoviesInfoCompleted() {
            @Override
            public void onProgressMovieInfoCompleted(Movie movie) {
                //mResult.mMovies.put(movie.id_g, movie);
                //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onRetrieveMoviesInfoCompleted(LinkedHashMap<String, Movie> movies) {
                mResult.mMovies = movies;
                ApplicationUtils.saveDataInCache(FeedActivity.this, mResult.mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                mMoviesCached = (LinkedHashMap<String, Movie>) ApplicationUtils
                        .getDataInCache(FeedActivity.this, ApplicationUtils.MOVIES_FILE_NAME);
                //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onRetrieveMoviesError(String message) {
                Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    Timer mTimer;
    /**
     * Function used to upadte each minute showtimes time remaining...
     */
    private void launchingTimerTask() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("FeedActivity", "Run");
                mResult.filterNewNextShowTimes();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //if (mMoviesFeedFragment != null)
                          //  mMoviesFeedFragment.filterFragment(mActionBar.getSelectedNavigationIndex());
                        if (mTheatersFragment != null)
                            mTheatersFragment.updateDataList(mActionBar.getSelectedNavigationIndex());
                    }
                });
            }
        }, 30 * 1000, 60 * 1000);
    }

    /**
     *
     */
    private void displayMoviesFeed() {
        mActionBar.show();
        findViewById(R.id.splashScreen).setVisibility(View.GONE);
        //new Handler().post(new Runnable() {
            //@Override
          //  public void run() {

        try {
            mMoviesFeedFragment = new MoviesFeedFragment();
            Bundle b = new Bundle();
            b.putSerializable("results", mResult);
            mMoviesFeedFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mainContainer, mMoviesFeedFragment, "feeds")
                    .commit();
        } catch (IllegalStateException e) {
            mMoviesFeedFragment = null;
            e.printStackTrace();
        }
           // }
        //});
    }

    /**
     *
     */
    private void updateFilters() {
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mResult.mMovieKinds = getMovieKindsList(mResult.mNextMovies);
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
        /*if (location != null) {
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
        mLocationManager.removeUpdates(this);*/

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
                            onQueryResultClicked(dataset, position, mSearchItem, mEditSearch);
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
        if (mMoviesFeedFragment != null && mMoviesFeedFragment.isVisible())
            mMoviesFeedFragment.setRefreshing(true);
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

    @Override
    public void onQueryResultClicked(List<Object> dataset, int position, MenuItem searchItem, AutoCompleteTextView editSearch) {
        MenuItemCompat.collapseActionView(searchItem);
        if (position < dataset.size()) {
            if (dataset.get(position) != null) {
                if (dataset.get(position) instanceof Movie) {
                    Movie movie = (Movie) dataset.get(position);
                    editSearch.setText("");
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    if (!mResult.mMovies.containsKey(movie.title)) {
                        mResult.mMovies.put(movie.title, movie);
                    }
                    b.putString("movie_id", movie.title);
                    mf.setArguments(b);
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf, movie.title)
                            .addToBackStack(null)
                            .commit();

                } else if (dataset.get(position) instanceof MovieTheater) {
                    MovieTheater theater = (MovieTheater) dataset.get(position);
                    this.mEditSearch.setText("");
                    TheaterFragment tf = new TheaterFragment();
                    Bundle b = new Bundle();
                    if (!mResult.mTheaters.containsKey(theater.mId)) {
                        mResult.mTheaters.put(theater.mId, theater);
                    }
                    b.putString("theater_id", theater.mId);
                    tf.setArguments(b);
                    getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, tf, theater.mId)
                            .addToBackStack(null)
                            .commit();
                }
            }
        }
    }

    @Override
    public void onSelectedChoice(String movieTheaterId, ShowTime showTime, int position) {
        MovieTheater theater = null;
        switch(position) {
            case 0:
                if (showTime == null)
                    theater = mResult.mTheaters.get(movieTheaterId);
                else
                    theater = mResult.mTheaters.get(showTime.mTheaterId);

                if (theater == null) {
                    theater = new MovieTheater();
                    theater.mId = movieTheaterId;
                }
                TheaterFragment tf = new TheaterFragment();
                Bundle b = new Bundle();
                if (!mResult.mTheaters.containsKey(theater.mId)) {
                    mResult.mTheaters.put(theater.mId, theater);
                }
                b.putString("theater_id", theater.mId);
                tf.setArguments(b);
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, tf, theater.mId)
                        .addToBackStack(null)
                        .commit();
                break;
            case 1:
                theater = mResult.mTheaters.get(movieTheaterId);
                String uri;
                if (theater.mLatitude == -10000 && theater.mLongitude == -10000) {
                    uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%s (%s)",
                            theater.mAddress, getResults().mTheaters.get(movieTheaterId).mName);
                } else {
                    uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%f,%f (%s)",
                            theater.mLatitude, theater.mLongitude, getResults().mTheaters.get(movieTheaterId).mName);
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
                break;
            case 2:
                if (showTime != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.sharing_string),
                            showTime.mMovieId, showTime.mShowTimeStr, getResults().mTheaters.get(showTime.mTheaterId).mName));
                    shareIntent.setType("text/plain");
                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_with)));
                }
                break;
        }
    }

    public void showTheaterChoiceFragment(ShowTime showTime) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Create and show the dialog.
        TheaterFragmentDialog newFragment = new TheaterFragmentDialog ();
        newFragment.setMovieTheaterName(showTime.mTheaterId);
        if (showTime.mMovieId != null)
            newFragment.setShowtime(showTime);
        newFragment.show(ft, "dialog");
    }

    public LinkedHashMap<String,Movie> getCachedMovies() {
        return mMoviesCached;
    }
}
