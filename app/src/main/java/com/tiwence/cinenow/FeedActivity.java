package com.tiwence.cinenow;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by temarill on 27/01/2015.
 */
public class FeedActivity extends ActionBarActivity implements OnRetrieveQueryCompleted, LocationListener, ActionBar.OnNavigationListener, SwipeFlingAdapterView.onFlingListener {

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

    private int i;
    private PlaceHolderFragment mTheatersFragment;

    private ActionBar mActionBar;

    private static final int REFRESH_LOCATION_TIMEOUT = 20000;

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
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(this, ApplicationUtils.MOVIES_FILE_NAME);

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
            refresh();
        } else if (id == R.id.action_theaters) {
            mTheatersFragment = new PlaceHolderFragment();
            Bundle b = new Bundle();
            b.putSerializable("result", mResult);
            mTheatersFragment.setArguments(b);
            getSupportFragmentManager().beginTransaction()
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
                        refreshTheatersFragment();
                        mLocationManager.removeUpdates(FeedActivity.this);
                    }
                }
            }, REFRESH_LOCATION_TIMEOUT);
        }
    }

    public void refreshTheatersFragment() {
        if (mTheatersFragment != null && mTheatersFragment.isVisible()) {
            mTheatersFragment.requestData(mLocation);
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
                ApplicationUtils.saveDataInCache(FeedActivity.this, mResult.mTheaters, ApplicationUtils.THEATERS_FILE_NAME);
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

    private void updateDataList() {
        //Adding feed view

        if (mTheatersFragment == null || !mTheatersFragment.isVisible()) {
            setContentView(R.layout.fragment_showtimes_feed);
        }

        if (mResult.mNextMovies != null && mResult.mNextMovies.size() > 0) {
            //ActionBar spinner adapter
            mActionBar.setDisplayShowTitleEnabled(false);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mResult.mMovieKinds = getMovieKindsList(mResult.mNextMovies);
            Log.d("FeedActivity", "Kinds : " + mResult.mMovieKinds.toString());
            mMoviesKindAdapter = new ArrayAdapter(this, R.layout.kind_item, mResult.mMovieKinds);
            mActionBar.setListNavigationCallbacks(mMoviesKindAdapter, this);

            mNextMovies = new ArrayList<>(mResult.mNextMovies);

            Collections.sort(mNextMovies, Movie.MovieDistanceComparator);

            mFeedAdapter = new MoviesAdapter(FeedActivity.this, R.layout.feed_item,
                    mNextMovies);
            mFeedContainer = (SwipeFlingAdapterView) this.findViewById(R.id.frame);

            //set the listener and the adapter
            mFeedContainer.init(this, mFeedAdapter);

            // Optionally add an OnItemClickListener
            mFeedContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
                @Override
                public void onItemClicked(int itemPosition, Object dataObject) {
                    Toast.makeText(FeedActivity.this, "Clicked ! " + itemPosition, Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            this.refreshTheatersFragment();
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
        resetContainer();
        mNextMovies = new ArrayList<>(mResult.mNextMovies);
        ArrayList<Movie> filteredMovies = new ArrayList<>();
        if (kindIndex > 0) {
            for (Movie m : mNextMovies) {
                if (m.kind != null && m.kind.equals(mResult.mMovieKinds.get(kindIndex))) {
                    filteredMovies.add(m);
                }
            }
            mNextMovies = filteredMovies;
        }
        Collections.sort(mNextMovies, Movie.MovieDistanceComparator);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Reload
                mFeedAdapter = new MoviesAdapter(FeedActivity.this, R.layout.feed_item, mNextMovies);
                mFeedContainer.init(FeedActivity.this, mFeedAdapter);
                mFeedAdapter.notifyDataSetChanged();
            }
        }, 200);
    }

    private void resetContainer() {
        mNextMovies = new ArrayList<Movie>();
        mFeedAdapter = new MoviesAdapter(this, R.layout.feed_item, mNextMovies);
        mFeedContainer.init(this, mFeedAdapter);
        mFeedAdapter.notifyDataSetChanged();
    }

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

    public void refresh() {
        mIsFirstLocation = true;
        refreshLocation();
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
                vh.mOtherShowTimesLayout = (LinearLayout) convertView.findViewById(R.id.otherShowTimesLayout);
                vh.showOtherShowTimesButton = (ImageButton) convertView.findViewById(R.id.buttonMoreShowTimes);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            Movie movie = getItem(position);
            ArrayList<ShowTime> sts = mResult.getNextShowtimesByMovieId(movie.id_g);

            ShowTime bst = sts.get(0);
            if (movie.mBestNextShowtime != null)
                bst = movie.mBestNextShowtime;

            for (int i = 1; i < sts.size(); i++) {
                ShowTime s = sts.get(i);
                TextView tv = new TextView(FeedActivity.this);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14.0f);
                tv.setPadding(5, 5, 5, 5);
                tv.setText("" + ApplicationUtils.getTimeString(s.mTimeRemaining) + " " + mResult.mTheaters.get(s.mTheaterId).mName);
                vh.mOtherShowTimesLayout.addView(tv);
                vh.mOtherShowTimesLayout.requestLayout();
            }

            vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(bst.mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(bst.mMovieId).title);
            vh.mTheaterName.setText(mResult.mTheaters.get(bst.mTheaterId).mName);

            final WeakReference<View> convertViewRef = new WeakReference<View>(convertView);

            vh.showOtherShowTimesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (convertViewRef != null && convertViewRef.get() != null) {
                        if (((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.getVisibility() == View.VISIBLE) {
                            ((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.setVisibility(View.INVISIBLE);
                        } else {
                            ((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.setVisibility(View.VISIBLE);
                        }
                    }
                    ((ViewGroup)convertViewRef.get()).invalidate();

                }
            });

            //Get poster
            if (mResult.mMovies.get(bst.mMovieId).poster_path != null &&
                    !mResult.mMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(bst.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(bst.mMovieId)
                    && mCachedMovies.get(bst.mMovieId).poster_path != null
                    && !mCachedMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(bst.mMovieId).poster_path;
                Picasso.with(FeedActivity.this).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(bst.mMovieId), new OnRetrieveMovieInfoCompleted() {
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
            LinearLayout mOtherShowTimesLayout;
            ImageButton showOtherShowTimesButton;
        }
    }
}
