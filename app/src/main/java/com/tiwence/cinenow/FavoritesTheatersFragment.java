package com.tiwence.cinenow;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tiwence.cinenow.adapter.ShowtimeAdapter;
import com.tiwence.cinenow.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow.listener.OnRetrieveTheaterShowTimeInfoCompleted;
import com.tiwence.cinenow.listener.OnSelectChoiceListener;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import it.sephiroth.android.library.widget.HListView;

/**
 * Created by temarill on 16/03/2015.
 */
public class FavoritesTheatersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    private View mRootView;
    private ListView mFavoritesListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<MovieTheater> mFavoritesList;
    private Location mLocation;
    private OnSelectChoiceListener mSelectChoiceListener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_theaters, container, false);

        mFavoritesListView = (ListView) mRootView.findViewById(R.id.listView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLocation = ((FeedActivity)getActivity()).getLocation();

        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSelectChoiceListener = (OnSelectChoiceListener) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListView();
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(false);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(true);
        ((FeedActivity) getActivity()).getMActionBar().setTitle(R.string.action_favorites_theaters);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
        ((FeedActivity)getActivity()).getMActionBar()
                .setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
    }

    private void setupListView() {
        mSwipeRefreshLayout.setRefreshing(true);
        mFavoritesList = (ArrayList<MovieTheater>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.FAVORITES_THEATERS_FILE_NAME);
        if (mFavoritesList == null)
            mFavoritesList = new ArrayList<>();
        //Adding favorites to the main dataset
        for (MovieTheater theater : mFavoritesList) {
            if (getResults().mTheaters.get(theater.mName) == null) {
                getResults().mTheaters.put(theater.mName, theater);
            }
        }

        mFavoritesListView.setAdapter(new FavoritesTheatersAdapter());

        /*mFavoritesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TheaterFragment tf = new TheaterFragment();
                Bundle b = new Bundle();
                MovieTheater theater = mFavoritesList.get(position);
                b.putSerializable("theater", theater);
                tf.setArguments(b);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, tf, theater.mName)
                        .addToBackStack(null)
                        .commit();
            }
        });*/
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        setupListView();
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            //mSelectChoiceListener.onSelectedChoice((String)v.getTag(), null, 0);
            ((FeedActivity) getActivity()).showTheaterChoiceFragment((ShowTime)v.getTag());
        }
    }

    public class FavoritesTheatersAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mFavoritesList != null)
                return  mFavoritesList.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (mFavoritesList != null)
                mFavoritesList.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            MovieTheater theater = mFavoritesList.get(position);
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.theater_item, null);
                vh = new ViewHolder();
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.theaterNameText);
                vh.mHListView = (HListView) convertView.findViewById(R.id.hListView);
                convertView.setTag(vh);
            }
            vh = (ViewHolder) convertView.getTag();

            final MovieTheater mt = mFavoritesList.get(position);
            if (mt.mDistance >= 1000 || mt.mDistance < 0) {
                final WeakReference<TextView> distanceRef = new WeakReference<TextView>(vh.mTheaterName);
                new TheaterDistanceHelper(mLocation, getResults(), distanceRef, true)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mt);
            } else {
                vh.mTheaterName.setText(mt.mName + " (" + mt.mDistance + " km)");
            }
            ShowTime stTemp = new ShowTime();
            stTemp.mTheaterId = mt.mName;
            vh.mTheaterName.setTag(stTemp);
            vh.mTheaterName.setOnClickListener(FavoritesTheatersFragment.this);
            ArrayList<ShowTime> showTimes = getResults().getNextShowTimesByTheaterId(mt.mName);
            if (showTimes != null && !showTimes.isEmpty()) {
                vh.mHListView.setAdapter(new ShowtimeAdapter(getActivity(), showTimes));
            } else {
                ApiUtils.instance().retrieveShowTimesTheaterInfos(getActivity(), theater, new OnRetrieveTheaterShowTimeInfoCompleted() {
                    @Override
                    public void onRetrieveTheaterShowTimeInfoCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
                        getResults().addNewTheaterInfos(mt, dataset);
                        FavoritesTheatersAdapter.this.notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveTheaterShowTimeInfoError(String errorMessage) {

                    }
                });
            }

            vh.mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> adapterView, View view, int i, long l) {
                    ShowTime st = getResults().getNextShowTimesByTheaterId(mt.mName).get(i);
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    b.putString("movie_id", st.mMovieId);
                    mf.setArguments(b);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf, st.mMovieId)
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

    public ShowTimesFeed getResults() {
        return ((FeedActivity) getActivity()).getResults();
    }

}
