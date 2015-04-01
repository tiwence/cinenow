package com.tiwence.cinenow2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.text.Html;
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
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow2.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow2.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow2.model.ShowTime;
import com.tiwence.cinenow2.utils.ApplicationUtils;
import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.MovieTheater;
import com.tiwence.cinenow2.model.ShowTimesFeed;
import com.tiwence.cinenow2.utils.ApiUtils;

import org.apmem.tools.layouts.FlowLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by temarill on 10/03/2015.
 */
public class FavoritesMoviesFragment extends android.support.v4.app.Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private View mRootView;
    private ExpandableListView mFavoritesListView;
    private ArrayList<Movie> mFavoritesList;
    private LinkedHashMap<Movie, LinkedHashMap<MovieTheater, ArrayList<ShowTime>>> mData = new LinkedHashMap<>();
    private LinkedHashMap<Movie, ArrayList<ShowTime>> mNextShowTimesFavorites = new LinkedHashMap<Movie, ArrayList<ShowTime>>();;
    private boolean mIsOnModalChoice;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        mFavoritesListView = (ExpandableListView) mRootView.findViewById(R.id.favoritesListView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshFavorites);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mFavoritesList = (ArrayList<Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
        if (mFavoritesList == null) {
            mFavoritesList = new ArrayList<>();
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListView();
        loadData();
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(false);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(true);
        ((FeedActivity) getActivity()).getMActionBar().setTitle(R.string.action_favorites_movies);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
        ((FeedActivity)getActivity()).getMActionBar()
                .setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
    }

    public void displayNoDataTextView(boolean isVisible) {
        if (isVisible) {
            mRootView.findViewById(R.id.noFavoriteMoviesText).setVisibility(View.VISIBLE);
            mFavoritesListView.setVisibility(View.GONE);
        } else {
            mRootView.findViewById(R.id.noFavoriteMoviesText).setVisibility(View.GONE);
            mFavoritesListView.setVisibility(View.VISIBLE);
        }
    }

    /**
     *
     */
    private void setupListView() {
        mFavoritesListView.setItemsCanFocus(false);
        mFavoritesListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mFavoritesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mFavoritesListView.setItemChecked(position, true);
                return true;
            }
        });

        mFavoritesListView.setMultiChoiceModeListener(multipleChoiceCallback);

        mFavoritesListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (mIsOnModalChoice) {
                    SparseBooleanArray checkedArray = mFavoritesListView.getCheckedItemPositions();
                    if(checkedArray.get(groupPosition) == true) {
                        mFavoritesListView.setItemChecked(groupPosition, false);
                    } else {
                        mFavoritesListView.setItemChecked(groupPosition, true);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    /**
     *
     */
    private void loadData() {
        mSwipeRefreshLayout.setRefreshing(true);
        displayNoDataTextView(false);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mData = new LinkedHashMap<Movie, LinkedHashMap<MovieTheater, ArrayList<ShowTime>>>();
                mNextShowTimesFavorites = new LinkedHashMap<Movie, ArrayList<ShowTime>>();
                for (Movie movie : mFavoritesList) {
                    mData.put(movie, getResults().getShowTimesByTheatersForMovie(movie.title));
                    mNextShowTimesFavorites.put(movie, getResults().getNextShowtimesByMovieId(movie.title));
                }
                if (mData.isEmpty()) {
                    //TODO 
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mSwipeRefreshLayout.setRefreshing(false);
                mFavoritesListView.setAdapter(new FavoritesMoviesListAdapter());
                if (mFavoritesList.size() > 0)
                    displayNoDataTextView(false);
                else
                    displayNoDataTextView(true);

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public ShowTimesFeed getResults() {
        return ((FeedActivity) getActivity()).getResults();
    }

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
                    mode.setSubtitle(getString(R.string.one_movie_selected));
                    break;
                default:
                    mode.setSubtitle(String.format(getString(R.string.movies_selected), checkedCount));
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
                            loadData();
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
        ApplicationUtils.saveDataInCache(getActivity(), mFavoritesList, ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
    }

    @Override
    public void onRefresh() {
        loadData();
    }

    /**
     *
     */
    public class FavoritesMoviesListAdapter extends BaseExpandableListAdapter {

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
            Movie movie = (Movie) getGroup(groupPosition);
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.favorites_group_item, null);
            }

            ImageView movieImageView = (ImageView) convertView
                    .findViewById(R.id.favoriteMoviePoster);
            TextView movieTitleView = (TextView) convertView
                    .findViewById(R.id.favoriteMovieName);
            TextView movieInfosView = (TextView) convertView
                    .findViewById(R.id.favoriteMovieInfos);
            LinearLayout nextShowTimesLayout = (LinearLayout) convertView
                    .findViewById(R.id.favoritesMovieShowTimesLayout);

            if (movie.poster_path != null && !movie.poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(movieImageView);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(movieImageView);
                ApiUtils.instance().retrieveMovieInfo(movie, new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        getResults().mMovies.put(movie.title, movie);
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

            movieTitleView.setText(movie.title);
            movieInfosView.setText(movie.infos_g);

            nextShowTimesLayout.removeAllViews();
            if(mNextShowTimesFavorites.get(movie) != null) {
                for (ShowTime st : mNextShowTimesFavorites.get(movie)) {
                    TextView tv = new TextView(getActivity());
                    tv.setTextSize(15.0f);
                    tv.setPadding(5, 5, 5, 5);
                    tv.setText(Html.fromHtml(ApplicationUtils.getTimeString(getActivity(), st.mTimeRemaining)
                            + " <strong>" + getResults().mTheaters.get(st.mTheaterId).mName + "</strong>"));
                    tv.setTag(st);
                    nextShowTimesLayout.addView(tv);
                    tv.setOnClickListener(FavoritesMoviesFragment.this);
                }
            } else {
                TextView tv = new TextView(getActivity());
                tv.setTextSize(15.0f);
                tv.setPadding(5, 5, 5, 5);
                tv.setText(getString(R.string.no_more_next_showtimes));
                nextShowTimesLayout.addView(tv);
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            Movie movie = (Movie) getGroup(groupPosition);
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.theater_for_movie_item, null);
            }

            TextView mTheaterNameTextView = (TextView) convertView.findViewById(R.id.theaterNameForMovieText);
            LinkedHashMap<MovieTheater, ArrayList<ShowTime>> mTheatersShowTimes = mData.get(movie);
            MovieTheater theater = (MovieTheater) mTheatersShowTimes.keySet().toArray()[childPosition];
            if (theater.mDistance >= 1000) {
                final WeakReference<TextView> distanceRef = new WeakReference<TextView>(mTheaterNameTextView);
                new TheaterDistanceHelper(((FeedActivity)getActivity()).getLocation(), getResults(), distanceRef, true)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, theater);
            } else {
                mTheaterNameTextView.setText(theater.mName + " (" + theater.mDistance + " km)");
            }

            mTheaterNameTextView.setOnClickListener(FavoritesMoviesFragment.this);
            ShowTime tempSt = new ShowTime();
            tempSt.mTheaterId = theater.mId;
            mTheaterNameTextView.setTag(tempSt);

            FlowLayout flowLayout = (FlowLayout) convertView.findViewById(R.id.showtimesForMovieLayout);
            flowLayout.removeAllViews();
            for (ShowTime st : mTheatersShowTimes.get(theater)) {
                TextView stv = new TextView(getActivity());
                stv.setTextSize(16.0f);
                stv.setText(st.mShowTimeStr);
                stv.setPadding(8, 5, 8, 5);
                stv.setTag(st);
                stv.setOnClickListener(FavoritesMoviesFragment.this);
                flowLayout.addView(stv);
            }

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            ((FeedActivity) getActivity()).showTheaterChoiceFragment((ShowTime)v.getTag());
        }
    }
}
