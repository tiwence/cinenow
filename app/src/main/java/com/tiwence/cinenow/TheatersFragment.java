package com.tiwence.cinenow;

/**
 * Created by temarill on 19/01/2015.
 */

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
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

import it.sephiroth.android.library.widget.AdapterView;
import it.sephiroth.android.library.widget.HListView;

/**
 * A placeholder fragment containing a simple view.
 */
public class TheatersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    public ListView mListView;
    public SwipeRefreshLayout mSwipeRefresh;

    //ArrayList<MovieTheater> mTheaters;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private ArrayList<MovieTheater> mTheaters;
    private TheaterAdapter mTheaterAdapter;
    private boolean mIsFirstLocation = true;
    private int mKindIndex = 0;
    private ArrayList<ShowTime> mNextShowtimes;

    public TheatersFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(this);

        mListView = (ListView) rootView.findViewById(R.id.listView);

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        //mResult = (ShowTimesFeed) getArguments().getSerializable("result");
        mKindIndex =  getArguments().getInt("kindIndex");

        if (getResults().mTheaters != null && getResults().mTheaters.size() > 0)
            updateDataList(getResults(), mKindIndex);

        return rootView;
    }

    private ShowTimesFeed getResults() {
        if (getActivity() != null)
            return ((FeedActivity)getActivity()).getResults();
        else
            return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && ((FeedActivity) getActivity()).getMActionBar() != null) {
            ((FeedActivity)getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
            mKindIndex = ((FeedActivity) getActivity()).getMActionBar().getSelectedNavigationIndex();
            filterFragment(mKindIndex);
        }
    }

    public void updateDataList(ShowTimesFeed result, int kindIndex) {
        mKindIndex = kindIndex;
        filterFragment(mKindIndex);
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        ((FeedActivity)getActivity()).refresh();
        //TODO refresh correctly
    }

    /**
     *
     * @param kindIndex
     */
    public void filterFragment(int kindIndex) {
        mKindIndex = kindIndex;
        filterTheaters();
        Collections.sort(mTheaters, MovieTheater.MovieTheatersDistanceComparator);
        if (getActivity() != null) {
            mTheaterAdapter = new TheaterAdapter(mTheaters);
            mListView.setAdapter(mTheaterAdapter);
            mSwipeRefresh.setRefreshing(false);
        }
    }

    /**
     *
     */
    private void filterTheaters() {
        if (this.getResults() != null) {
            mTheaters = new ArrayList<MovieTheater>(this.getResults().mTheaters.values());
            if (mKindIndex > 0) {
                ArrayList<MovieTheater> filteredTheaters = new ArrayList<>();
                int totalNbST = 0;
                for (MovieTheater theater : mTheaters) {
                    totalNbST = 0;
                    for (ShowTime st : this.getResults().getNextShowTimesByTheaterId(theater.mId)) {
                        if (getResults().mMovies.get(st.mMovieId).kind != null &&
                                getResults().mMovies.get(st.mMovieId).kind.equals(getResults().mMovieKinds.get(mKindIndex))) {
                            totalNbST ++;
                        }
                    }
                    if (totalNbST > 0) {
                        filteredTheaters.add(theater);
                    }
                }
                mTheaters = filteredTheaters;
            }
        }
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
            final MovieTheater mt = mTheaters.get(position);
            vh.mTheaterName.setText(mt.mName);

            vh.mHListView.setAdapter(new ShowtimeAdapter(filteredShowTime(getResults().getNextShowTimesByTheaterId(mt.mId))));
            vh.mHListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    ShowTime st = filteredShowTime(getResults().getNextShowTimesByTheaterId(mt.mId)).get(i);
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    b.putString("movie_id", st.mMovieId);
                    mf.setArguments(b);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf)
                            .addToBackStack(null)
                            .commit();
                }
            });

            return convertView;
        }

        public class ViewHolder {
            TextView mTheaterName;
            HListView mHListView;
        }
    }

    /**
     *
     * @param nextShowTimesByTheaterId
     * @return
     */
    private ArrayList<ShowTime> filteredShowTime(ArrayList<ShowTime> nextShowTimesByTheaterId) {
        if (mKindIndex == 0)
            return nextShowTimesByTheaterId;
        else {
            ArrayList<ShowTime> showTimes = new ArrayList<>();
            for (ShowTime st : nextShowTimesByTheaterId) {

                if (getResults().mMovies.get(st.mMovieId).kind != null &&
                        getResults().mMovies.get(st.mMovieId).kind.equals(getResults().mMovieKinds.get(mKindIndex))) {
                    showTimes.add(st);
                }
            }
            return showTimes;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
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
            vh.mMovieTitle.setText(getResults().mMovies.get(mShowTimes.get(position).mMovieId).title);

            //Get poster
            if (getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path != null &&
                    !getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(mShowTimes.get(position).mMovieId)
                    && mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path != null
                    && !mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(getResults().mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
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
