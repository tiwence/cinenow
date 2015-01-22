package com.tiwence.cinenow;

/**
 * Created by temarill on 19/01/2015.
 */

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.TheaterResult;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;
import com.tiwence.cinenow.utils.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.utils.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow.utils.OnRetrieveTheatersCompleted;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import it.sephiroth.android.library.widget.HListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceHolderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, LocationListener {

    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefresh;

    public LocationManager mLocationManager;
    public Location mLocation;

    //ArrayList<MovieTheater> mTheaters;
    private TheaterResult mResult;
    private HashMap<String, Movie> mCachedMovies;
    private TheaterAdapter mTheaterAdapter;
    private boolean mIsFirstLocation = true;

    public PlaceHolderFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mLocationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);
        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mListView = (ListView) rootView.findViewById(R.id.listView);

        mCachedMovies = ApplicationUtils.getMoviesInCache(getActivity());

        if (mLocation != null)
            requestData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Toast.makeText(getActivity(), "Localisation en cours", Toast.LENGTH_LONG).show();
    }

    /**
     *
     */
    private void requestData() {
        ApiUtils.instance().retrieveMovieShowTimeTheaters(getActivity(), mLocation, new OnRetrieveTheatersCompleted() {
            @Override
            public void onRetrieveTheatersCompleted(TheaterResult result) {
                mResult = result;
                updateDataList();
                mSwipeRefresh.setRefreshing(false);
                ApiUtils.instance().retrieveMoviesInfo(getActivity(), result.mMovies, new OnRetrieveMoviesInfoCompleted() {
                    @Override
                    public void onProgressMovieInfoCompleted(Movie movie) {
                        //mResult.mMovies.put(movie.id_g, movie);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesInfoCompleted(HashMap<String, Movie> movies) {
                        Log.d("MOVIE SEARCH", "All movies get");
                        mResult.mMovies = movies;
                        ApplicationUtils.saveMoviesInCache(getActivity(), mResult.mMovies);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesError(String message) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRetrieveTheatersError(String errorMessage) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateDataList() {
        mTheaterAdapter = new TheaterAdapter(mResult.mMovieTheaters);
        mListView.setAdapter(mTheaterAdapter);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location", location.getLatitude() + ", " + location.getLongitude());
        mLocation = location;
        if (mIsFirstLocation) {
            mIsFirstLocation = false;
            Toast.makeText(getActivity(), getString(R.string.location_done), Toast.LENGTH_LONG).show();
        }
        mLocationManager.removeUpdates(this);
        requestData();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Provider status changed", provider + ", status " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Provider enabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Provider disabled", provider);
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        requestData();
        //Toast.makeText(getActivity(), "Localisation en cours", Toast.LENGTH_LONG).show();
    }

    /**
     *
     */
    public class TheaterAdapter extends BaseAdapter {

        ArrayList<MovieTheater> mTheaters;
        LayoutInflater mInflater;
        ShowtimeAdapter mShowTimeAdapter;

        public TheaterAdapter(ArrayList<MovieTheater> theaters) {
            this.mTheaters = theaters;
            this.mInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            if (mTheaters != null)
                return mTheaters.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mTheaters != null)
                return mTheaters.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return this.getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.theater_item, parent, false);
                vh = new ViewHolder();
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.theaterNameText);
                vh.mHListView = (HListView) convertView.findViewById(R.id.hListView);
                convertView.setTag(vh);
            }
            vh = (ViewHolder) convertView.getTag();
            vh.mTheaterName.setText(mTheaters.get(position).mName);
            vh.mHListView.setAdapter(new ShowtimeAdapter(mTheaters.get(position).mShowTimes));

            return convertView;
        }

        public class ViewHolder {
            TextView mTheaterName;
            HListView mHListView;
        }
    }

    /**
     *
     */
    public class ShowtimeAdapter extends BaseAdapter {

        private ArrayList<ShowTime> mShowTimes;
        private LayoutInflater mInflater;

        public ShowtimeAdapter(ArrayList<ShowTime> showTimes) {
            this.mShowTimes = showTimes;
            this.mInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            if (mShowTimes != null)
                return mShowTimes.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mShowTimes != null)
                return mShowTimes.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return this.getItem(position).hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.movie_item, parent, false);
                vh = new ViewHolder();
                vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.movieTimeRemainingText);
                vh.mPoster = (ImageView) convertView.findViewById(R.id.moviePosterView);
                vh.mMovieTitle = (TextView) convertView.findViewById(R.id.movieTitleText);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();
            vh.mTimeRemaining.setText("" + mShowTimes.get(position).mTimeRemaining);
            vh.mMovieTitle.setText(mResult.mMovies.get(mShowTimes.get(position).mMovieId).title);

            if (mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path != null &&
                    !mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(mShowTimes.get(position).mMovieId)) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        if (imgViewRef != null)
                            Picasso.with(getActivity()).load(posterPath)
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
        }
    }

}
