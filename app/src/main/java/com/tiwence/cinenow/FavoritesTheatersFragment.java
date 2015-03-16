package com.tiwence.cinenow;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tiwence.cinenow.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow.listener.OnSelectChoiceListener;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

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
        mRootView = inflater.inflate(R.layout.fragment_theater, container, false);
        mRootView.findViewById(R.id.favoritesFloatingButton).setVisibility(View.GONE);
        mFavoritesListView = (ListView) mRootView.findViewById(R.id.listViewTheater);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshTheater);
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
        mFavoritesListView.setAdapter(new FavoritesTheatersAdapter());

        mFavoritesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TheaterFragment tf = new TheaterFragment();
                Bundle b = new Bundle();
                MovieTheater theater = mFavoritesList.get(position);
                b.putString("theater_id", theater.mName);
                tf.setArguments(b);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, tf, theater.mName)
                        .addToBackStack(null)
                        .commit();
            }
        });
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
            mSelectChoiceListener.onSelectedChoice((String)v.getTag(), null, 1);
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
                convertView = getActivity().getLayoutInflater().inflate(R.layout.header_theater_item, null);
                vh = new ViewHolder();
                vh.theaterHeaderMapIcon = (ImageView) convertView.findViewById(R.id.theaterImagePlaceholder);
                vh.theaterName = (TextView) convertView.findViewById(R.id.theaterFragmentName);
                vh.theaterInfos = (TextView) convertView.findViewById(R.id.theaterFragmentInfo);
                vh.theaterDistance = (TextView) convertView.findViewById(R.id.theaterFragmentDistance);
                convertView.setTag(vh);
            }
            vh = (ViewHolder) convertView.getTag();
            vh.theaterName.setText(theater.mName);
            vh.theaterInfos.setText(theater.mAddress);
            vh.theaterHeaderMapIcon.setTag(theater.mName);
            vh.theaterHeaderMapIcon.setOnClickListener(FavoritesTheatersFragment.this);
            if (theater.mDistance >= 1000 || theater.mDistance < 0) {
                final WeakReference<TextView> distanceRef = new WeakReference<TextView>(vh.theaterDistance);
                new TheaterDistanceHelper(mLocation, getResults(), distanceRef, false)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, theater);
            } else {
                vh.theaterDistance.setText(theater.mDistance + " km");
            }
            return convertView;
        }

        class ViewHolder {
            ImageView theaterHeaderMapIcon;
            TextView theaterName;
            TextView theaterInfos;
            TextView theaterDistance;
        }

    }

    public ShowTimesFeed getResults() {
        return ((FeedActivity) getActivity()).getResults();
    }

}
