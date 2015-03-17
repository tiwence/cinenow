package com.tiwence.cinenow;

/**
 * Created by temarill on 19/01/2015.
 */

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.tiwence.cinenow.adapter.TheaterAdapter;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

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
        View rootView = inflater.inflate(R.layout.fragment_theaters, container, false);

        mSwipeRefresh = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(this);

        mListView = (ListView) rootView.findViewById(R.id.listView);

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        //mResult = (ShowTimesFeed) getArguments().getSerializable("result");
        mKindIndex =  getArguments().getInt("kindIndex");

        if (getResults().mTheaters != null && getResults().mTheaters.size() > 0)
            updateDataList(mKindIndex);

        return rootView;
    }

    public ShowTimesFeed getResults() {
        if (getActivity() != null)
            return ((FeedActivity)getActivity()).getResults();
        else
            return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && ((FeedActivity) getActivity()).getMActionBar() != null) {
            ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(true);
            ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
            ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
            ((FeedActivity)getActivity()).getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
            ((FeedActivity)getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(false);
            ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
            mKindIndex = ((FeedActivity) getActivity()).getMActionBar().getSelectedNavigationIndex();
            filterFragment(mKindIndex);
        }
    }

    public void updateDataList(int kindIndex) {
        mSwipeRefresh.setRefreshing(false);
        mKindIndex = kindIndex;
        filterFragment(mKindIndex);
    }

    @Override
    public void onRefresh() {
        mSwipeRefresh.setRefreshing(true);
        ((FeedActivity)getActivity()).refresh();
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
            mTheaterAdapter = new TheaterAdapter(TheatersFragment.this, mTheaters);
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
                    if (this.getResults() != null && theater.mName != null
                            && this.getResults().getNextShowTimesByTheaterId(theater.mName) != null) {
                        for (ShowTime st : this.getResults().getNextShowTimesByTheaterId(theater.mName)) {
                            if (getResults().mMovies.get(st.mMovieId).kind != null &&
                                    getResults().mMovies.get(st.mMovieId).kind.equals(getResults().mMovieKinds.get(mKindIndex))) {
                                totalNbST ++;
                            }
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

    public  LinkedHashMap<String, Movie> getCachedMovies() {
        return mCachedMovies;
    }

    /**
     *
     * @param nextShowTimesByTheaterId
     * @return
     */
    public ArrayList<ShowTime> filteredShowTime(ArrayList<ShowTime> nextShowTimesByTheaterId) {
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

}
