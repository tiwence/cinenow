package com.tiwence.cinenow;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveMoviesInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveShowTimesCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import org.jsoup.Connection;
import org.w3c.dom.Text;

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
    private ShowTimesFeed mResult;
    private ArrayList<Movie> mNextMovies;
    private MoviesAdapter mFeedAdapter;
    private int mKindIndex;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_showtimes_feed, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        mResult = (ShowTimesFeed) getArguments().getSerializable("result");

        if (mResult != null && mResult.mNextMovies != null && mResult.mNextMovies.size() > 0) {
            updateDataList(mResult);
        }

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void updateDataList(ShowTimesFeed result) {
        //Adding feed view
        mResult = result;
        if (this.mResult.mNextMovies != null && this.mResult.mNextMovies.size() > 0) {
            //ActionBar spinner adapter
            mNextMovies = new ArrayList<>(this.mResult.mNextMovies);
            Collections.sort(mNextMovies, Movie.MovieDistanceComparator);
            mFeedAdapter = new MoviesAdapter(getActivity(), R.layout.feed_item,
                    mNextMovies);
            mFeedContainer = (SwipeFlingAdapterView) mRootView.findViewById(R.id.frame);

            //set the listener and the adapter
            //mFeedContainer.init(getActivity(), mFeedAdapter);
            mFeedContainer.setAdapter(mFeedAdapter);
            mFeedContainer.setFlingListener(this);

            // Optionally add an OnItemClickListener
            mFeedContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
                @Override
                public void onItemClicked(int itemPosition, Object dataObject) {
                    Toast.makeText(getActivity(), "Clicked ! " + itemPosition, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("MFF", "ON RESUME");
        if (getActivity() != null && ((FeedActivity) getActivity()).getMActionBar() != null) {
            mKindIndex = ((FeedActivity) getActivity()).getMActionBar().getSelectedNavigationIndex();
            filterFragment(mKindIndex);
        }
    }

    private void resetContainer() {
        mNextMovies = new ArrayList<Movie>();
        mFeedAdapter = new MoviesAdapter(getActivity(), R.layout.feed_item, mNextMovies);
        mFeedContainer.setAdapter(mFeedAdapter);
        mFeedContainer.setFlingListener(this);
        mFeedAdapter.notifyDataSetChanged();
    }

    public void filterFragment(int kindIndex) {
        resetContainer();
        mKindIndex = kindIndex;
        mNextMovies = new ArrayList<>(mResult.mNextMovies);
        ArrayList<Movie> filteredMovies = new ArrayList<>();
        if (mKindIndex > 0) {
            for (Movie m : mNextMovies) {
                if (m.kind != null && m.kind.equals(mResult.mMovieKinds.get(mKindIndex))) {
                    filteredMovies.add(m);
                }
            }
            mNextMovies = filteredMovies;
        }
        Collections.sort(mNextMovies, Movie.MovieDistanceComparator);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Reload
                mFeedAdapter = new MoviesFeedFragment.MoviesAdapter(getActivity(), R.layout.feed_item, mNextMovies);
                //mFeedContainer.init(getActivity(), mFeedAdapter);
                mFeedContainer.setAdapter(mFeedAdapter);
                mFeedContainer.setFlingListener(MoviesFeedFragment.this);
                mFeedAdapter.notifyDataSetChanged();
            }
        }, 200);
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


    }

    @Override
    public void onAdapterAboutToEmpty(int itemsInAdapter) {
        // Ask for more data here
        mFeedAdapter.notifyDataSetChanged();
        //Log.d("LIST", "notified");
        i++;
    }

    @Override
    public void onScroll(float scrollProgressPercent) {
        View view = mFeedContainer.getSelectedView();
        if (view != null && view.findViewById(R.id.item_swipe_right_indicator) != null)
            view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
        if (view != null && view.findViewById(R.id.item_swipe_left_indicator) != null)
            view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
    }


    public class MoviesAdapter extends ArrayAdapter<Movie> {

        private Context mContext;

        public MoviesAdapter(Context context, int resource, List<Movie> objects) {
            super(context, resource, objects);
            this.mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
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
                vh.mOtherShowTimesLayout = (LinearLayout) convertView.findViewById(R.id.otherShowTimesLayout);
                vh.showOtherShowTimesButton = (ImageButton) convertView.findViewById(R.id.buttonMoreShowTimes);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            Movie movie = getItem(position);
            ArrayList<ShowTime> sts = mResult.getNextShowtimesByMovieId(movie.id_g);

            ShowTime bst = sts.get(0);
            if (movie.mBestNextShowtime != null)
                bst = movie.mBestNextShowtime;

            vh.mOtherShowTimesLayout.setVisibility(View.GONE);

            for (int i = 1; i < sts.size(); i++) {
                ShowTime s = sts.get(i);
                TextView tv = new TextView(getActivity());
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14.0f);
                tv.setPadding(5, 5, 5, 5);
                tv.setText("" + ApplicationUtils.getTimeString(s.mTimeRemaining) + " " + mResult.mTheaters.get(s.mTheaterId).mName);
                vh.mOtherShowTimesLayout.addView(tv);
                vh.mOtherShowTimesLayout.requestLayout();
            }

            vh.mTimeRemaining.setText("" + ApplicationUtils.getTimeString(bst.mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(bst.mMovieId).title);
            vh.mTheaterName.setText(mResult.mTheaters.get(bst.mTheaterId).mName);

            final WeakReference<View> convertViewRef = new WeakReference<View>(convertView);

            vh.showOtherShowTimesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (convertViewRef != null && convertViewRef.get() != null) {
                        if (((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.getVisibility() == View.VISIBLE) {
                            ((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.setVisibility(View.GONE);
                        } else {
                            ((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.setVisibility(View.VISIBLE);
                        }
                        //((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.getParent().requestLayout();
                        ((ViewGroup)((ViewHolder)convertViewRef.get().getTag()).mOtherShowTimesLayout.getParent()).invalidate();
                    }

                }
            });

            //Get poster
            if (mResult.mMovies.get(bst.mMovieId).poster_path != null &&
                    !mResult.mMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(bst.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(bst.mMovieId)
                    && mCachedMovies.get(bst.mMovieId).poster_path != null
                    && !mCachedMovies.get(bst.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(bst.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(bst.mMovieId), new OnRetrieveMovieInfoCompleted() {
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
            TextView mTheaterName;
            LinearLayout mOtherShowTimesLayout;
            ImageButton showOtherShowTimesButton;
        }
    }


}
