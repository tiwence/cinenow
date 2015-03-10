package com.tiwence.cinenow;

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
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_showtimes_feed, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        //mResult = (ShowTimesFeed) getArguments().getSerializable("result");

        mySavedInstanceState = getArguments();

        if (mySavedInstanceState != null
                && mySavedInstanceState.getSerializable("nextMovies") != null) {
            mNextMovies = (ArrayList<Movie>) mySavedInstanceState.getSerializable("nextMovies");
        }

        if (getResults() != null && getResults().mNextMovies != null && getResults().mNextMovies.size() > 0) {
            updateDataList();
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
                if (getActivity() != null) {
                    ((FeedActivity)getActivity()).displayTheatersFragment();
                }
            }
        });

        mRootView.findViewById(R.id.favoritesFloatingButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    mf.setArguments(b);
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mySavedInstanceState.putSerializable("nextMovies", mNextMovies);
    }

    public ShowTimesFeed getResults() {
        return  ((FeedActivity)this.getActivity()).getResults();
    }

    public void updateDataList() {
        //Adding feed view
        if (this.getResults().mNextMovies != null && this.getResults().mNextMovies.size() > 0) {
            //ActionBar spinner adapter
            if (mNextMovies == null) {
                mNextMovies = new ArrayList<>(this.getResults().getNextMovies());
                Collections.sort(mNextMovies, Movie.MovieDistanceComparator);
            }
            mFeedAdapter = new MoviesAdapter(getActivity(), R.layout.feed_item,
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
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                    android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                            .replace(R.id.mainContainer, mf)
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
        if (getActivity() != null && ((FeedActivity) getActivity()).getMActionBar() != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ((FeedActivity)getActivity()).getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
                    ((FeedActivity)getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(false);
                    if (mKindIndex != ((FeedActivity) getActivity()).getMActionBar().getSelectedNavigationIndex()) {
                        mKindIndex = ((FeedActivity) getActivity()).getMActionBar().getSelectedNavigationIndex();
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
            mFeedAdapter = new MoviesAdapter(getActivity(), R.layout.feed_item, mNextMovies);
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

        logMovies("Next movies");

        mySavedInstanceState.putSerializable("nextMovies", mNextMovies);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Reload
                if (mNextMovies != null && mFeedContainer != null) {
                    mFeedAdapter = new MoviesFeedFragment.MoviesAdapter(getActivity(), R.layout.feed_item, mNextMovies);
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


    public void logMovies(String log) {
        for (Movie m : mNextMovies) {
            Log.d(log, m.title + ", " + m.mBestDistance + ", " + m.mFirstTimeRemaining);
        }
    }

    @Override
    public void removeFirstObjectInAdapter() {
        // this is the simplest way to delete an object from the Adapter (/AdapterView)
        mNextMovies.remove(0);
        mFeedAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLeftCardExit(Object dataObject) {
    }

    @Override
    public void onRightCardExit(Object dataObject) {
        if (mFavoriteMovies == null) mFavoriteMovies = new ArrayList<>();
        mFavoriteMovies.add((Movie)dataObject);
    }

    @Override
    public void onAdapterAboutToEmpty(int itemsInAdapter) {
        // Ask for more data here
        mFeedAdapter.notifyDataSetChanged();
        if (mNextMovies == null || mNextMovies.size() == 0)
            mRootView.findViewById(R.id.reloadLayout).setVisibility(View.VISIBLE);
        //Log.d("LIST", "notified");
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
                vh.showOtherShowTimesButton = (ImageButton) convertView.findViewById(R.id.buttonMoreShowTimes);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            final Movie movie = mNextMovies.get(position);
            final ArrayList<ShowTime> sts = getResults().getNextShowtimesByMovieId(movie.title);

            if (sts == null)
                return  convertView;

            ShowTime bst = movie.mBestNextShowtime;
            if (bst != null) {
                vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(bst.mTimeRemaining));
                vh.mMovieTitle.setText(getResults().mMovies.get(bst.mMovieId).title);
                vh.mTheaterName.setText(getResults().mTheaters.get(bst.mTheaterId).mName);
                vh.mTheaterName.setTag(bst);
                vh.mMovieTitle.setTag(bst);
                vh.mTheaterName.setOnClickListener(this);
                vh.mMovieTitle.setOnClickListener(this);

                for (int i = 0; i < sts.size(); i++) {
                    ShowTime s = sts.get(i);
                    if (!s.mId.equals(bst.mId)) {
                        TextView tv = new TextView(getActivity());
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

            /*vh.showOtherShowTimesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.d("Clicked : ", "" + ((ViewHolder) mFeedContainer.getSelectedView().getTag()).mMovieTitle.getText());
                    if (mFeedContainer.getSelectedView().findViewById(R.id.movieShowTimesLayout).getVisibility() == View.GONE) {
                        mFeedContainer.getSelectedView().findViewById(R.id.movieShowTimesLayout).setVisibility(View.VISIBLE);
                    } else {
                        mFeedContainer.getSelectedView().findViewById(R.id.movieShowTimesLayout).setVisibility(View.GONE);
                    }
                    Log.d("Visibility", "" + mFeedContainer.getSelectedView().findViewById(R.id.movieShowTimesLayout).getVisibility());
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mFeedContainer.getSelectedView().invalidate();
                            Log.d("Visibility", "" + ((TextView)mFeedContainer.getSelectedView().findViewById(R.id.showtimeTitleTextView)).getText());
                            mFeedContainer.getSelectedView().requestLayout();
                        }
                    }, 150);

                }
            });*/
            //Get poster
            if (getResults().mMovies.get(bst.mMovieId).poster_path != null &&
                    !getResults().mMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + getResults().mMovies.get(bst.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(bst.mMovieId)
                    && mCachedMovies.get(bst.mMovieId).poster_path != null
                    && !mCachedMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(bst.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(getResults().mMovies.get(bst.mMovieId), new OnRetrieveMovieInfoCompleted() {
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

            return convertView;
        }

        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                ((FeedActivity) getActivity()).showTheaterChoiceFragment((ShowTime)v.getTag());
            }
        }

        public class ViewHolder {
            ImageView mPoster;
            TextView mMovieTitle;
            TextView mTimeRemaining;
            TextView mTheaterName;
            LinearLayout mOtherShowTimesLayout;
            ImageButton showOtherShowTimesButton;
        }
    }
}
