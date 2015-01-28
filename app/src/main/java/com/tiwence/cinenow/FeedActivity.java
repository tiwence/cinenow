package com.tiwence.cinenow;

import android.app.Activity;
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
import android.widget.Spinner;
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
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

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
    private EditText mEditSearch;
    private SpinnerAdapter mMoviesKindAdapter;

    private LocationManager mLocationManager;
    private Location mLocation;
    private boolean mIsFirstLocation = true;
    private boolean mNewLocationFound = false;

    private SwipeFlingAdapterView mFeedContainer;
    //private ShowTimesAdapter mFeedAdapter;
    private MoviesAdapter mFeedAdapter;

    private ShowTimesFeed mResult;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private ArrayList<Movie> mNextMovies;

    private ActionBar mActionBar;

    private static final int REFRESH_LOCATION_TIMEOUT = 5000;

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
    private int i;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle("");

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        mCachedMovies = ApplicationUtils.getMoviesInCache(this);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {

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
                        mLocationManager.removeUpdates(FeedActivity.this);
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
                updateDataList();
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
                        ApplicationUtils.saveMoviesInCache(FeedActivity.this, mResult.mMovies);
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

    private void updateDataList() {
        //Adding feed view
        setContentView(R.layout.fragment_showtimes_feed);

        if (mResult.mMovies != null) {
            //ActionBar spinner adapter
            mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mMoviesKindAdapter = new ArrayAdapter(this, R.layout.kind_item, mResult.mMovieKinds);
            mActionBar.setListNavigationCallbacks(mMoviesKindAdapter, this);

            mNextMovies =  filterNextMovies(new ArrayList<>(mResult.mMovies.values()));
            mFeedAdapter = new MoviesAdapter(FeedActivity.this, R.layout.feed_item,
                    mNextMovies);
            mFeedContainer = (SwipeFlingAdapterView) this.findViewById(R.id.frame);

            //set the listener and the adapter
            mFeedContainer.setAdapter(mFeedAdapter);
            mFeedContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
                @Override
                public void removeFirstObjectInAdapter() {
                    // this is the simplest way to delete an object from the Adapter (/AdapterView)
                    Log.d("LIST", "removed object!");
                    mNextMovies.remove(0);
                    mFeedAdapter.notifyDataSetChanged();
                }

                @Override
                public void onLeftCardExit(Object dataObject) {
                    //Do something on the left!
                    //You also have access to the original object.
                    //If you want to use it just cast it (String) dataObject
                    makeToast(FeedActivity.this, "Left!");
                }

                @Override
                public void onRightCardExit(Object dataObject) {
                    makeToast(FeedActivity.this, "Right!");
                }

                @Override
                public void onAdapterAboutToEmpty(int itemsInAdapter) {
                    // Ask for more data here
                    mFeedAdapter.notifyDataSetChanged();
                    Log.d("LIST", "notified");
                    i++;
                }

                @Override
                public void onScroll(float scrollProgressPercent) {
                    View view = mFeedContainer.getSelectedView();
                    if (view != null && view.findViewById(R.id.item_swipe_right_indicator) != null)
                        view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                    if (view != null && view.findViewById(R.id.item_swipe_left_indicator) != null)
                        view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
                }
            });

            // Optionally add an OnItemClickListener
            mFeedContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
                @Override
                public void onItemClicked(int itemPosition, Object dataObject) {
                    Toast.makeText(FeedActivity.this, "Clicked ! " + itemPosition, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private ArrayList<Movie> filterNextMovies(ArrayList<Movie> movies) {
        ArrayList<Movie> nextMovies = new ArrayList<>();
        for (Movie m : movies) {
            if (mResult.getNextShowtimesByMovieId(m.id_g) != null
                    && mResult.getNextShowtimesByMovieId(m.id_g).size() > 0)
                nextMovies.add(m);
        }
        return nextMovies;
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
    public void onRetrieveQueryMovieCompleted(Movie movie) {

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
    private void filterNextMoviesByKind(int kindIndex) {
        mNextMovies = filterNextMovies(new ArrayList<>(mResult.mMovies.values()));
        ArrayList<Movie> filteredMovies = new ArrayList<>();
        if (kindIndex > 0) {
            for (Movie m : mNextMovies) {
                if (m.kind.equals(mResult.mMovieKinds.get(kindIndex))) {
                    filteredMovies.add(m);
                }
            }
            mNextMovies = filteredMovies;
        }

        mFeedAdapter = new MoviesAdapter(this, R.layout.feed_item, mNextMovies);
        mFeedContainer.setAdapter(mFeedAdapter);
        mFeedAdapter.notifyDataSetChanged();
    }

    /**
     *
     */
    public class ShowTimesAdapter extends ArrayAdapter<ShowTime> {

        private Context mContext;

        public ShowTimesAdapter(Context context, int resource, List<ShowTime> objects) {
            super(context, resource, objects);
            this.mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.feed_item, parent, false);
                vh = new ViewHolder();
                vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.showtimeTimeRemainingTextView);
                vh.mPoster = (ImageView) convertView.findViewById(R.id.showtimePoster);
                vh.mMovieTitle = (TextView) convertView.findViewById(R.id.showtimeTitleTextView);
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.showtimeTheaterTextView);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            ShowTime st = getItem(position);

            Log.d("ST", "" + st);

            vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(st.mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(st.mMovieId).title);
            vh.mTheaterName.setText(mResult.mTheaters.get(st.mTheaterId).mName);

            //Get poster
            if (mResult.mMovies.get(st.mMovieId).poster_path != null &&
                    !mResult.mMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(st.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(st.mMovieId)
                    && mCachedMovies.get(st.mMovieId).poster_path != null
                    && !mCachedMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(st.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(st.mMovieId), new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        if (imgViewRef != null && imgViewRef.get() != null)
                            Picasso.with(FeedActivity.this).load(posterPath)
                                    .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                    }

                    @Override
                    public void onRetrieveMovieError(String message) {

                    }
                });
            }

            return convertView;
        }

        public class ViewHolder {
            ImageView mPoster;
            TextView mMovieTitle;
            TextView mTimeRemaining;
            TextView mTheaterName;
        }
    }

    public class MoviesAdapter extends ArrayAdapter<Movie> {

        private Context mContext;

        public MoviesAdapter(Context context, int resource, List<Movie> objects) {
            super(context, resource, objects);
            this.mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.feed_item, parent, false);
                vh = new ViewHolder();
                vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.showtimeTimeRemainingTextView);
                vh.mPoster = (ImageView) convertView.findViewById(R.id.showtimePoster);
                vh.mMovieTitle = (TextView) convertView.findViewById(R.id.showtimeTitleTextView);
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.showtimeTheaterTextView);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            Movie movie = getItem(position);
            ArrayList<ShowTime> sts = mResult.getNextShowtimesByMovieId(movie.id_g);
            ShowTime st = sts.get(0);

            vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(st.mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(st.mMovieId).title);
            vh.mTheaterName.setText(mResult.mTheaters.get(st.mTheaterId).mName);

            //Get poster
            if (mResult.mMovies.get(st.mMovieId).poster_path != null &&
                    !mResult.mMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(st.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(st.mMovieId)
                    && mCachedMovies.get(st.mMovieId).poster_path != null
                    && !mCachedMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(st.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(st.mMovieId), new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        if (imgViewRef != null && imgViewRef.get() != null)
                            Picasso.with(FeedActivity.this).load(posterPath)
                                    .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                    }

                    @Override
                    public void onRetrieveMovieError(String message) {

                    }
                });
            }

            return convertView;
        }

        public class ViewHolder {
            ImageView mPoster;
            TextView mMovieTitle;
            TextView mTimeRemaining;
            TextView mTheaterName;
        }
    }
}
