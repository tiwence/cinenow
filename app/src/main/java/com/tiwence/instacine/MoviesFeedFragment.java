package com.tiwence.instacine;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.squareup.picasso.Picasso;
import com.tiwence.instacine.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.instacine.listener.OnRetrieveMovieMoreInfosCompleted;
import com.tiwence.instacine.model.Movie;
import com.tiwence.instacine.model.ShowTime;
import com.tiwence.instacine.model.ShowTimesFeed;
import com.tiwence.instacine.utils.ApiUtils;
import com.tiwence.instacine.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by temarill on 26/01/2015.
 */
public class MoviesFeedFragment extends android.support.v4.app.Fragment implements SwipeFlingAdapterView.onFlingListener {

    private View mRootView;
    SwipeFlingAdapterView mFeedContainer;

    private int i;
    private LinkedHashMap<String, Movie> mCachedMovies;
    //private ShowTimesFeed mResult;
    private ArrayList<Movie> mNextMovies;
    private ArrayList<Movie> mFavoriteMovies;
    private MoviesAdapter mFeedAdapter;
    private int mKindIndex;
    private String mIdSelected = "";
    private Bundle mySavedInstanceState;
    private FeedActivity mFeedActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_showtimes_feed, container, false);
        //mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(mFeedActivity, ApplicationUtils.MOVIES_FILE_NAME);
        mCachedMovies = ((FeedActivity)getActivity()).getCachedMovies();
        mFavoriteMovies = (ArrayList<Movie>) ApplicationUtils.getDataInCache(mFeedActivity, ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
        //mResult = (ShowTimesFeed) getArguments().getSerializable("result");

        mySavedInstanceState = getArguments();

        if (mySavedInstanceState != null
                && mySavedInstanceState.getSerializable("nextMovies") != null) {
            mNextMovies = (ArrayList<Movie>) mySavedInstanceState.getSerializable("nextMovies");
        }

        if (getResults() != null && getResults().mNextMovies != null && getResults().mNextMovies.size() > 0) {
            mRootView.findViewById(R.id.noMoreShowTimesLayout).setVisibility(View.GONE);
            updateDataList();
        } else {
            setRefreshing(false);
            mRootView.findViewById(R.id.reloadLayout).setVisibility(View.GONE);
            mRootView.findViewById(R.id.noMoreShowTimesLayout).setVisibility(View.VISIBLE);
        }

        mRootView.findViewById(R.id.reloadImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterFragment(mKindIndex);
            }
        });

        mRootView.findViewById(R.id.theatersFloatingButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeedActivity != null) {
                    mFeedActivity.displayTheatersFragment();
                }
            }
        });

        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mFeedActivity = (FeedActivity) activity;
    }

    @Override
    public void onPause() {
        super.onPause();
        mySavedInstanceState.putSerializable("nextMovies", mNextMovies);
    }

    public ShowTimesFeed getResults() {
        return  mFeedActivity.getResults();
    }

    public void updateDataList() {
        this.setRefreshing(false);

        //if ((this.getResults().mNextMovies == null && this.getResults().mNextMovies.size() == 0) {
          //  mNextMovies = this.mFirst
        //}
        if (this.getResults().mNextMovies != null && this.getResults().mNextMovies.size() > 0) {
            //ActionBar spinner adapter
            if (mNextMovies == null) {
                mNextMovies = new ArrayList<>(this.getResults().getNextMovies());
                Collections.sort(mNextMovies, Movie.MovieDistanceComparator);
            }
            mFeedAdapter = new MoviesAdapter(mFeedActivity, R.layout.feed_item,
                    mNextMovies);

            mFeedContainer = (SwipeFlingAdapterView) mRootView.findViewById(R.id.frame);

            //set the listener and the adapter
            //mFeedContainer.init(getActivity(), mFeedAdapter);
            mRootView.findViewById(R.id.reloadLayout).setVisibility(View.INVISIBLE);
            mFeedContainer.setAdapter(mFeedAdapter);
            mFeedContainer.setFlingListener(this);

            // Optionally add an OnItemClickListener
            mFeedContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
                @Override
                public void onItemClicked(int itemPosition, Object dataObject) {
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    b.putString("movie_id",((Movie)(dataObject)).title);
                    mf.setArguments(b);
                    mFeedActivity.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf, ((Movie)(dataObject)).title)
                            .addToBackStack(null)
                            .commit();
                    //Toast.makeText(getActivity(), "Clicked ! " + itemPosition, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFeedActivity != null && mFeedActivity.getMActionBar() != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mFeedActivity.getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
                    mFeedActivity.getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    mFeedActivity.getMActionBar().setDisplayHomeAsUpEnabled(false);
                    if(mFeedActivity.getMenu() != null) {
                        mFeedActivity.getMenu().findItem(R.id.action_refresh).setVisible(true);
                        mFeedActivity.getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
                        mFeedActivity.getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
                    }
                    mFeedActivity.getMActionBar().setDisplayShowTitleEnabled(false);
                    if (mKindIndex != mFeedActivity.getMActionBar().getSelectedNavigationIndex()) {
                        mKindIndex = mFeedActivity.getMActionBar().getSelectedNavigationIndex();
                        filterFragment(mKindIndex);
                    }
                }
            }, 100);
        }
    }

    /**
     *
     */
    private void resetContainer() {
        if (mFeedContainer != null) {
            mNextMovies = new ArrayList<Movie>();
            mFeedAdapter = new MoviesAdapter(mFeedActivity, R.layout.feed_item, mNextMovies);
            mRootView.findViewById(R.id.reloadLayout).setVisibility(View.INVISIBLE);
            mFeedContainer.setAdapter(mFeedAdapter);
            mFeedContainer.setFlingListener(this);
            mFeedAdapter.notifyDataSetChanged();
        }
    }

    /**
     *
     * @param kindIndex
     */
    public void filterFragment(final int kindIndex) {
        resetContainer();
        mKindIndex = kindIndex;
        mNextMovies = new ArrayList<>(getResults().getNextMovies());
        ArrayList<Movie> filteredMovies = new ArrayList<>();
        if (mKindIndex > 0) {
            for (Movie m : mNextMovies) {
                if (m.kind != null && m.kind.equals(getResults().mMovieKinds.get(mKindIndex))) {
                    filteredMovies.add(m);
                }
            }
            mNextMovies = filteredMovies;
        }

        Collections.sort(mNextMovies, Movie.MovieDistanceComparator);

        if (mySavedInstanceState != null)
            mySavedInstanceState.putSerializable("nextMovies", mNextMovies);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Reload
                if (mNextMovies != null && mFeedContainer != null) {
                    mFeedAdapter = new MoviesFeedFragment.MoviesAdapter(mFeedActivity, R.layout.feed_item, mNextMovies);
                    Log.d("MoviesFeedFragment", "UPDATEDATALIST FILTER WITH INDEX" + kindIndex);

                    mRootView.findViewById(R.id.reloadLayout).setVisibility(View.INVISIBLE);
                    //mFeedContainer.init(getActivity(), mFeedAdapter);
                    mFeedContainer.setAdapter(mFeedAdapter);
                    mFeedContainer.setFlingListener(MoviesFeedFragment.this);
                    mFeedAdapter.notifyDataSetChanged();
                }
            }
        }, 200);
    }


    public void setRefreshing(boolean isRefreshing) {
        if (isRefreshing)
            mRootView.findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
        else
            mRootView.findViewById(R.id.marker_progress).setVisibility(View.GONE);
    }

    @Override
    public void removeFirstObjectInAdapter() {
        // this is the simplest way to delete an object from the Adapter (/AdapterView)
        mNextMovies.remove(0);
        mFeedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLeftCardExit(Object dataObject) {
        if (mFavoriteMovies == null) mFavoriteMovies = new ArrayList<>();
        if (mFavoriteMovies.contains((Movie)dataObject)) {
            mFavoriteMovies.remove((Movie) dataObject);
            ApplicationUtils.saveDataInCache(mFeedActivity, mFavoriteMovies, ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
        }
    }

    @Override
    public void onRightCardExit(Object dataObject) {
        if (mFavoriteMovies == null) mFavoriteMovies = new ArrayList<>();
        if (!mFavoriteMovies.contains((Movie)dataObject)) {
            mFavoriteMovies.add((Movie)dataObject);
            ApplicationUtils.saveDataInCache(mFeedActivity, mFavoriteMovies, ApplicationUtils.FAVORITES_MOVIES_FILE_NAME);
        }

    }

    @Override
    public void onAdapterAboutToEmpty(int itemsInAdapter) {
        // Ask for more data here
        mFeedAdapter.notifyDataSetChanged();
        if (mNextMovies == null || mNextMovies.size() == 0)
            mRootView.findViewById(R.id.reloadLayout).setVisibility(View.VISIBLE);
        i++;
    }

    @Override
    public void onScroll(float scrollProgressPercent) {
        View view = mFeedContainer.getSelectedView();
        if (view != null && view.findViewById(R.id.item_swipe_left_indicator) != null)
            view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
        if (view != null && view.findViewById(R.id.item_swipe_right_indicator) != null)
            view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
    }

    /**
     *
     */
    public class MoviesAdapter extends ArrayAdapter<Movie> implements View.OnClickListener {

        private Context mContext;
        private MoviesFeedFragment mFeedFragment;

        public MoviesAdapter(Context context, int resource, List<Movie> objects) {
            super(context, resource, objects);
            this.mContext = context;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.feed_item, parent, false);
                vh = new ViewHolder();
                vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.showtimeTimeRemainingTextView);
                vh.mPoster = (ImageView) convertView.findViewById(R.id.showtimePoster);
                vh.mMovieTitle = (TextView) convertView.findViewById(R.id.showtimeTitleTextView);
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.showtimeTheaterTextView);
                vh.mOtherShowTimesLayout = (LinearLayout) convertView.findViewById(R.id.movieShowTimesLayout);
                vh.mMovieOverView = (TextView) convertView.findViewById(R.id.feedMovieOverviewTextView);
                vh.showOtherShowTimesButton = (ImageButton) convertView.findViewById(R.id.buttonMoreShowTimes);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            final Movie movie = mNextMovies.get(position);
            final ArrayList<ShowTime> sts = getResults().getNextShowtimesByMovieId(movie.title);

            if (sts == null)
                return  convertView;

            ShowTime bst = movie.mBestNextShowtime;
            if (bst == null) {
                bst = sts.get(0);
            }
            if (bst != null) {
                vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(bst.mTimeRemaining));
                vh.mMovieTitle.setText(getResults().mMovies.get(bst.mMovieId).title);
                vh.mTheaterName.setText(getResults().mTheaters.get(bst.mTheaterId).mName);
                vh.mTheaterName.setTag(bst);
                vh.mMovieTitle.setTag(bst);
                vh.mMovieOverView.setText("PROUT" + movie.overview);
                vh.mTheaterName.setOnClickListener(this);
                vh.mMovieTitle.setOnClickListener(this);

                for (int i = 0; i < sts.size(); i++) {
                    ShowTime s = sts.get(i);
                    if (!s.mId.equals(bst.mId)) {
                        TextView tv = new TextView(mFeedActivity);
                        tv.setTextColor(Color.WHITE);
                        tv.setTextSize(14.0f);
                        tv.setPadding(5, 5, 5, 5);
                        tv.setText(ApplicationUtils.getTimeString(s.mTimeRemaining) + " " + getResults().mTheaters.get(s.mTheaterId).mName);
                        tv.setTag(s);
                        tv.setClickable(true);
                        tv.setOnClickListener(this);
                        vh.mOtherShowTimesLayout.addView(tv);
                    }
                }
                vh.mOtherShowTimesLayout.requestLayout();
            }

            final WeakReference<TextView> ref = new WeakReference<TextView>(vh.mMovieOverView);
            vh.showOtherShowTimesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ref.get() != null) {
                        if (ref.get().getAlpha() == 1.0f) {
                            ApplicationUtils.fadeView(ref.get(), false);
                        } else {
                            if (movie.overview != null && !movie.overview.equals("")) {
                                if(ref.get() != null) {
                                    ref.get().setText(movie.overview);
                                    ApplicationUtils.fadeView(ref.get(), true);
                                }
                            } else {
                                ApiUtils.instance().retrieveMoreMovieInfos(movie, new OnRetrieveMovieMoreInfosCompleted() {
                                    @Override
                                    public void onRetrieveMovieMoreInfosCompleted(Movie _movie) {
                                        movie.overview = _movie.overview;
                                        getResults().mMovies.put(movie.title, movie);
                                        ApplicationUtils.saveDataInCache(mFeedActivity, getResults().mMovies, ApplicationUtils.MOVIES_FILE_NAME);
                                        ref.get().setText(movie.overview);
                                        ((TextView)mFeedContainer.getSelectedView().findViewById(R.id.feedMovieOverviewTextView)).setText(movie.overview);
                                        ((TextView)mFeedContainer.getSelectedView().findViewById(R.id.feedMovieOverviewTextView)).invalidate();
                                        ApplicationUtils.fadeView(ref.get(), true);
                                    }
                                    @Override
                                    public void onRetrieveMovieMoreInfosError(String errorMessage) {

                                    }
                                });
                            }
                        }
                    }
                }
            });
            //Get poster
            if (getResults().mMovies.get(bst.mMovieId).poster_path != null &&
                    !getResults().mMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + getResults().mMovies.get(bst.mMovieId).poster_path;
                Picasso.with(mFeedActivity).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(bst.mMovieId)
                    && mCachedMovies.get(bst.mMovieId).poster_path != null
                    && !mCachedMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(bst.mMovieId).poster_path;
                Picasso.with(mFeedActivity).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(getResults().mMovies.get(bst.mMovieId), new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        getResults().mMovies.put(movie.title, movie);
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        if (imgViewRef != null && imgViewRef.get() != null)
                            Picasso.with(mFeedActivity).load(posterPath)
                                    .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                    }

                    @Override
                    public void onRetrieveMovieError(String message) {

                    }
                });
            }

            return convertView;
        }

        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                mFeedActivity.showTheaterChoiceFragment((ShowTime) v.getTag());
            }
        }

        public class ViewHolder {
            ImageView mPoster;
            TextView mMovieTitle;
            TextView mTimeRemaining;
            TextView mTheaterName;
            LinearLayout mOtherShowTimesLayout;
            ImageButton showOtherShowTimesButton;
            TextView mMovieOverView;
        }
    }
}
