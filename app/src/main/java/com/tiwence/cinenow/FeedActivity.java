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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
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

            if (queryName.length() > 3) {
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            refresh();
        } else if (id == R.id.action_theaters) {
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

        }

        return super.onOptionsItemSelected(item);
    }

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
                    }
                }
            }, REFRESH_LOCATION_TIMEOUT);
        }
    }

    /*public void refreshTheatersFragment() {
        if (mTheatersFragment != null && mTheatersFragment.isVisible()) {
            mTheatersFragment.requestData(mLocation);
        }
    }*/

    /**
     * Get provider name.
     *
     * @return Name of best suiting provider.
     */
    private String getProviderName() {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW); // Chose your desired power consumption level.
        criteria.setAccuracy(Criteria.ACCURACY_COARSE); // Choose your accuracy requirement.
        criteria.setSpeedRequired(true); // Chose if speed for first location fix is required.
        criteria.setAltitudeRequired(false); // Choose if you use altitude.
        criteria.setBearingRequired(false); // Choose if you use bearing.
        criteria.setCostAllowed(false); // Choose if this provider can waste money :-)

        // Provide your criteria and flag enabledOnly that tells
        // LocationManager only to return active providers.
        return mLocationManager.getBestProvider(criteria, true);
    }

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
    public void onRetrieveQueryMovieCompleted(Movie movie) {
        if (movie != null) {
            Log.d("Movie query completed", movie.title + ", " + movie.id_g);

            List<Object> m = new ArrayList<>();
            m.add(movie);

            SearchResultsAdapter searchedAppsAdapter = new SearchResultsAdapter(
                    getApplicationContext(), R.layout.spinner_search_item, m);
            mEditSearch.setAdapter(searchedAppsAdapter);
            mEditSearch.showDropDown();
            /*mEditSearch.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        int arg2, long arg3) {
                    if (arg2 < result.size()) {
                        if (result.get(arg2) != null) {
                            mEditSearch.setText(result.get(arg2).mName);
                            Intent i = new Intent(getApplicationContext(),
                                    AppDetailActivity.class);
                            i.putExtra("app", result.get(arg2));
                            startActivity(i);
                            MainTabsActivity.this
                                    .overridePendingTransition(
                                            android.R.anim.slide_in_left,
                                            android.R.anim.slide_out_right);
                        }
                    }

                }
            });*/
        }
    }

    @Override
    public void onRetrieveQueryTheaterCompleted(MovieTheater theater) {

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

    public ActionBar getMActionBar() {
        return mActionBar;
    }

    /**
     *
     */
    public class SearchResultsAdapter extends ArrayAdapter<Object> {

        private Context mContext;
        private List<Object> dataset;

        public SearchResultsAdapter(Context context, int resource, List<Object> objects) {
            super(context, resource, objects);
            mContext = context;
            dataset = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.spinner_search_item, null);
                vh = new ViewHolder();
                vh.searchImage = (ImageView) convertView.findViewById(R.id.spinnerSearchImage);
                vh.searchName = (TextView) convertView.findViewById(R.id.spinnerSearchName);
                vh.searchInfos = (TextView) convertView.findViewById(R.id.spinnerSearchInfos);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            Object data = getItem(position);
            if (data instanceof Movie) {
                vh.searchName.setText(((Movie)data).title);
                vh.searchInfos.setText(((Movie)data).infos_g);
                if (((Movie)data).poster_path != null) {
                    Picasso.with(mContext).load(((Movie)data).poster_path).placeholder(R.drawable.poster_placeholder).into(vh.searchImage);
                } else {
                    final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.searchImage);

                    ApiUtils.instance().retrieveMovieInfo((Movie)data, new OnRetrieveMovieInfoCompleted() {
                        @Override
                        public void onRetrieveMovieInfoCompleted(Movie movie) {
                            if (imgViewRef != null && imgViewRef.get() != null)
                                Picasso.with(mContext).load(movie.poster_path).placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                        }

                        @Override
                        public void onRetrieveMovieError(String message) {

                        }
                    });
                }
            } else if (data instanceof MovieTheater) {

            }

            return convertView;
        }

        class ViewHolder {
            ImageView searchImage;
            TextView searchName;
            TextView searchInfos;
        }
    }
}
