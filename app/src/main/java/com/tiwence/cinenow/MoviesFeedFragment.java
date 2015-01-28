package com.tiwence.cinenow;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by temarill on 26/01/2015.
 */
public class MoviesFeedFragment extends android.support.v4.app.Fragment {

    private LinkedHashMap<String, Movie> mCachedMovies;
    private ShowTimesFeed mResult;
    private View mRootView;
    SwipeFlingAdapterView mFeedContainer;
    ShowTimesAdapter mFeedAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_showtimes_feed, container, false);
        mCachedMovies = ApplicationUtils.getMoviesInCache(getActivity());

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if (((MainActivity)getActivity()).getLocation() != null) {
          //  this.requestData(((MainActivity)getActivity()).getLocation());
        //}
    }


    private void updateDataList() {
        //add the view via xml or programmatically

        //choose your favorite adapter
        //arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item, R.id.helloText, al);

        mFeedContainer = (SwipeFlingAdapterView) mRootView.findViewById(R.id.frame);

        mFeedAdapter = new ShowTimesAdapter(getActivity(), R.layout.feed_item, mResult.mNextShowTimes);
        mFeedContainer.setAdapter(mFeedAdapter);

        //set the listener and the adapter
        mFeedContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                Log.d("LIST", "removed object!");
                mResult.mShowTimes.remove(0);
                mFeedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                Toast.makeText(getActivity(), "Left!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                Toast.makeText(getActivity(), "Right!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {

            }

            @Override
            public void onScroll(float scrollProgressPercent) {
                View view = mFeedContainer.getSelectedView();
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
            }
        });

        // Optionally add an OnItemClickListener
        mFeedContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                Toast.makeText(getActivity(), "Clicked ! " + itemPosition, Toast.LENGTH_SHORT).show();
            }
        });

        Toast.makeText(getActivity(), "DISPLAYING CARDS", Toast.LENGTH_LONG).show();

        mFeedContainer.invalidate();
    }

    /**
     *
     * @param location
     */
    public void requestData(Location location) {
        ApiUtils.instance().retrieveMovieShowTimeTheaters(getActivity(), location, new OnRetrieveShowTimesCompleted() {
            @Override
            public void onRetrieveShowTimesCompleted(ShowTimesFeed result) {
                mResult = result;
                updateDataList();
                getActivity().setProgressBarIndeterminateVisibility(false);
                ApiUtils.instance().retrieveMoviesInfo(getActivity(), result.mMovies, new OnRetrieveMoviesInfoCompleted() {
                    @Override
                    public void onProgressMovieInfoCompleted(Movie movie) {
                        //mResult.mMovies.put(movie.id_g, movie);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesInfoCompleted(LinkedHashMap<String, Movie> movies) {
                        Log.d("MOVIE SEARCH", "All movies get");
                        mResult.mMovies = movies;
                        ApplicationUtils.saveMoviesInCache(getActivity(), mResult.mMovies);
                        //((TheaterAdapter)mListView.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void onRetrieveMoviesError(String message) {
                        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onRetrieveShowTimesError(String errorMessage) {
                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    public class ShowTimesAdapter extends ArrayAdapter<ShowTime> {

        private Context mContext;

        public ShowTimesAdapter(Context context, int resource, ArrayList<ShowTime> showTimes) {
            super(context, resource, showTimes);
            mContext = context;
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView  == null) {
                convertView  = mInflater.inflate(R.layout.feed_item, parent, false);
                vh = new ViewHolder();
                vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.showtimeTimeRemainingTextView);
                //vh.mPoster = (ImageView) convertView.findViewById(R.id.showtimePoster);
                vh.mMovieTitle = (TextView) convertView.findViewById(R.id.showtimeTitleTextView);
                vh.mTheaterName = (TextView) convertView.findViewById(R.id.showtimeTheaterTextView);
                convertView.setTag(vh);
            }

            vh = (ViewHolder) convertView.getTag();

            ShowTime st = getItem(position);

            vh.mTimeRemaining.setText(ApplicationUtils.getTimeString(st.mTimeRemaining));
            vh.mMovieTitle.setText(mResult.mMovies.get(st.mMovieId).title);
            vh.mTheaterName.setText(mResult.mTheaters.get(st.mTheaterId).mName);

            //Get poster
            if (mResult.mMovies.get(st.mMovieId).poster_path != null &&
                    !mResult.mMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(st.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else if (mCachedMovies != null && mCachedMovies.containsKey(st.mMovieId)
                    && mCachedMovies.get(st.mMovieId).poster_path != null
                    && !mCachedMovies.get(st.mMovieId).poster_path.equals("")) {
                String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(st.mMovieId).poster_path;
                Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
                ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(st.mMovieId), new OnRetrieveMovieInfoCompleted() {
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
        }
    }
}
