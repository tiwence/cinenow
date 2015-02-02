package com.tiwence.cinenow;

/**
 * Created by temarill on 19/01/2015.
 */

import android.location.Location;
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
import com.tiwence.cinenow.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMoviesInfoCompleted;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

import it.sephiroth.android.library.widget.HListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceHolderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    public ListView mListView;
    public SwipeRefreshLayout mSwipeRefresh;

    //ArrayList<MovieTheater> mTheaters;
    private ShowTimesFeed mResult;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private TheaterAdapter mTheaterAdapter;
    private boolean mIsFirstLocation = true;

    public PlaceHolderFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(this);

        mListView = (ListView) rootView.findViewById(R.id.listView);

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);

        mResult = (ShowTimesFeed) getArguments().getSerializable("result");

        if (mResult.mTheaters != null && mResult.mTheaters.size() > 0)
            updateDataList();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     *
     */
    public void requestData(Location location) {
        ApiUtils.instance().retrieveMovieShowTimeTheaters(getActivity(), location, new OnRetrieveShowTimesCompleted() {
            @Override
            public void onRetrieveShowTimesCompleted(ShowTimesFeed result) {
                mResult = result;
                updateDataList();
                ApplicationUtils.saveDataInCache(getActivity(), mResult.mTheaters, ApplicationUtils.THEATERS_FILE_NAME);
                mSwipeRefresh.setRefreshing(false);

                //Getting movies infos such as his poster, casting etc.
                ApiUtils.instance().retrieveMoviesInfo(getActivity(), result.mMovies, new OnRetrieveMoviesInfoCompleted() {
                    @Override
                    public void onProgressMovieInfoCompleted(Movie movie) {
                        //mResult.mMovies.put(movie.id_g, movie);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesInfoCompleted(LinkedHashMap<String, Movie> movies) {
                        Log.d("MOVIE SEARCH", "All movies get");
                        mResult.mMovies = movies;
                        ApplicationUtils.saveDataInCache(getActivity(), mResult.mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesError(String message) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRetrieveShowTimesError(String errorMessage) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                mSwipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateDataList() {
        ArrayList<MovieTheater> theaters = new ArrayList<MovieTheater>(mResult.mTheaters.values());
        Collections.sort(theaters, new Comparator<MovieTheater>() {
            @Override
            public int compare(MovieTheater lhs, MovieTheater rhs) {
                if (lhs.mDistance < rhs.mDistance) return  -1;
                else if (lhs.mDistance > rhs.mDistance) return 1;
                return 0;
            }
        });
        mTheaterAdapter = new TheaterAdapter(theaters);
        mListView.setAdapter(mTheaterAdapter);
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        ((FeedActivity)getActivity()).refresh();
        //TODO refresh correctly
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("Fragment", "Stop");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("Fragment", "Pause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Fragment", "Destroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("Fragment", "Detach");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("Fragment", "Destroy view");

    }

    /**
     *
     */
    public class TheaterAdapter extends BaseAdapter {

        ArrayList<MovieTheater> mTheaters;
        LayoutInflater mInflater;

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
            MovieTheater mt = mTheaters.get(position);
            vh.mTheaterName.setText(mt.mName);
            vh.mHListView.setAdapter(new ShowtimeAdapter(mResult.getNextShowTimesByTheaterId(mt.mId)));

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
            vh.mTimeRemaining.setText(ApplicationUtils.getTimeString(mShowTimes.get(position).mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(mShowTimes.get(position).mMovieId).title);

            //Get poster
            if (mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path != null &&
                    !mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(mShowTimes.get(position).mMovieId)
                    && mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path != null
                    && !mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        if (imgViewRef != null && imgViewRef.get() != null)
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
