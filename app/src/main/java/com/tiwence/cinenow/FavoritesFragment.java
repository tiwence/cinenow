package com.tiwence.cinenow;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by temarill on 10/03/2015.
 */
public class FavoritesFragment extends android.support.v4.app.Fragment {

    private View mRootView;
    private ExpandableListView mFavoritesListView;
    private ArrayList<Movie> mFavoritesList;
    private LinkedHashMap<Movie, ArrayList<ShowTime>> mData;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        mFavoritesListView = (ExpandableListView) mRootView.findViewById(R.id.favoritesListView);

        loadData();

        return mRootView;
    }

    private void loadData() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                ShowTimesFeed stf  = ((FeedActivity) getActivity()).getResults();
                mData = new LinkedHashMap<Movie, ArrayList<ShowTime>>();
                for (Movie movie : mFavoritesList) {
                    mData.put(movie, stf.getNextShowtimesByMovieId(movie.title));
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public class FavoritesListAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return mData.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mData.get(mFavoritesList.get(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mFavoritesList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mData.get(mFavoritesList.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            return null;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            return null;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
