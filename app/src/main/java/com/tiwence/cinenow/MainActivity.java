package com.tiwence.cinenow;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.OnRetrieveQueryCompleted;

import java.util.Locale;


public class MainActivity extends ActionBarActivity implements OnRetrieveQueryCompleted, LocationListener {

    private PlaceHolderFragment mCurrentFragment;
    private EditText mEditSearch;

    private LocationManager mLocationManager;
    private Location mLocation;
    private boolean mIsFirstLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mCurrentFragment = new PlaceHolderFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mCurrentFragment)
                    .commit();
        }
        setSupportProgressBarIndeterminateVisibility(true);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        refreshLocation();
    }

    public LocationManager getLocationManager() {
        return this.mLocationManager;
    }

    public Location getLocation() {
        return this.mLocation;
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
                    ApiUtils.instance().retrieveQueryInfo(mLocation, queryName, MainActivity.this);
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
    public void onRetrieveQueryMovieCompleted(Movie movie) {
        Log.d("QUERY OK", movie.title);
    }

    @Override
    public void onRetrieveQueryTheaterCompleted(MovieTheater theater) {

    }

    @Override
    public void onRetrieveQueryError(String errorMessage) {
        Log.d("Query", errorMessage);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location", location.getLatitude() + ", " + location.getLongitude());
        mLocation = location;
        if (mIsFirstLocation) {
            mIsFirstLocation = false;
            Toast.makeText(this, getString(R.string.location_done), Toast.LENGTH_LONG).show();
            mCurrentFragment.requestData(mLocation);
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

    public void refreshLocation() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }
}
