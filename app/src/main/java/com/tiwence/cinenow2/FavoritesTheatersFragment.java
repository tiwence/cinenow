package com.tiwence.cinenow2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.tiwence.cinenow2.adapter.ShowtimeAdapter;
import com.tiwence.cinenow2.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow2.listener.OnRetrieveTheaterShowTimeInfoCompleted;
import com.tiwence.cinenow2.listener.OnSelectChoiceListener;
import com.tiwence.cinenow2.model.ShowTime;
import com.tiwence.cinenow2.model.ShowTimesFeed;
import com.tiwence.cinenow2.utils.ApplicationUtils;
import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.MovieTheater;
import com.tiwence.cinenow2.utils.ApiUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
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

        mFavoritesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mFavoritesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mFavoritesListView.setItemChecked(position, true);
                return true;
            }
        });
        mFavoritesListView.setMultiChoiceModeListener(multipleChoiceCallback);

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

    public void displayNoDataTextView(boolean isVisible) {
        if (isVisible) {
            mRootView.findViewById(R.id.noFavoriteTheatersText).setVisibility(View.VISIBLE);
            mFavoritesListView.setVisibility(View.GONE);
        } else {
            mRootView.findViewById(R.id.noFavoriteTheatersText).setVisibility(View.GONE);
            mFavoritesListView.setVisibility(View.VISIBLE);
        }
    }

    private void setupListView() {
        mSwipeRefreshLayout.setRefreshing(true);
        mFavoritesList = (ArrayList<MovieTheater>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.FAVORITES_THEATERS_FILE_NAME);
        if (mFavoritesList == null)
            mFavoritesList = new ArrayList<>();
        //Adding favorites to the main dataset
        for (MovieTheater theater : mFavoritesList) {
            if (getResults().mTheaters.get(theater.mId) == null) {
                getResults().mTheaters.put(theater.mId, theater);
            }
        }

        if (mFavoritesList.isEmpty()) {
            displayNoDataTextView(true);
        } else {
            displayNoDataTextView(false);
        }

        Collections.sort(mFavoritesList, MovieTheater.MovieTheatersDistanceComparator);

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
                vh.mNoShowTimesPlaceholder = (TextView) convertView.findViewById(R.id.noshowtimesPlaceholder);
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
            stTemp.mTheaterId = mt.mId;
            vh.mTheaterName.setTag(stTemp);
            vh.mTheaterName.setOnClickListener(FavoritesTheatersFragment.this);
            ArrayList<ShowTime> showTimes = getResults().getNextShowTimesByTheaterId(mt.mId);
            if (showTimes != null && !showTimes.isEmpty()) {
                vh.mHListView.setAdapter(new ShowtimeAdapter(getActivity(), showTimes));
                vh.mNoShowTimesPlaceholder.setVisibility(View.GONE);
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
                    ShowTime st = getResults().getNextShowTimesByTheaterId(mt.mId).get(i);
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
            TextView mNoShowTimesPlaceholder;
        }
    }

    public ShowTimesFeed getResults() {
        return ((FeedActivity) getActivity()).getResults();
    }

    private boolean mIsOnModalChoice;
    /**
     *
     */
    private AbsListView.MultiChoiceModeListener multipleChoiceCallback = new AbsListView.MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = mFavoritesListView.getCheckedItemCount();
            Log.d("COUNT", "" + checkedCount);
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle(getString(R.string.one_theater_selected));
                    break;
                default:
                    mode.setSubtitle(String.format(getString(R.string.theaters_selected), checkedCount));
                    break;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mIsOnModalChoice = true;
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.menu_favorites, menu);
            mode.setTitle(getString(R.string.favorites_contextual_selection_header));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        boolean mIsAllChecked = false;
        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_select_all:
                    for (int i = 0; i < mFavoritesList.size(); i++) {
                        mFavoritesListView.setItemChecked(i, !mIsAllChecked);
                    }
                    mIsAllChecked = !mIsAllChecked;
                    break;
                /*case R.id.action_share:
                    Toast.makeText(getActivity(), "Shared " + mFavoritesListView.getCheckedItemCount() +
                            " items", Toast.LENGTH_SHORT).show();
                    mode.finish();
                    //TODO
                    break;*/
                case R.id.action_delete:
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(String.format(getString(R.string.delete_verification_header),
                            mFavoritesListView.getCheckedItemCount()));
                    builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeFavorites();
                            dialog.dismiss();
                            setupListView();
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mode.finish();
                        }
                    });
                    builder.create().show();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mIsOnModalChoice = false;
        }
    };

    /**
     *
     */
    private void removeFavorites() {
        SparseBooleanArray checkedArray = mFavoritesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedArray.size(); i++) {
            int index = checkedArray.keyAt(0);
            mFavoritesList.remove(index);
        }
        ApplicationUtils.saveDataInCache(getActivity(), mFavoritesList, ApplicationUtils.FAVORITES_THEATERS_FILE_NAME);
    }

}
