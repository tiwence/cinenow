package com.tiwence.cinenow;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveTheaterInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import org.apmem.tools.layouts.FlowLayout;
import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by temarill on 18/02/2015.
 */
public class TheaterFragment extends Fragment {

    private View mRootView;
    private ListView mListViewTheater;
    MovieTheater mCurrentTheater;
    private Location mLocation;
    private LinkedHashMap<String, Movie> mCachedMovies;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_theater, container, false);
        mListViewTheater = (ListView) mRootView.findViewById(R.id.listViewTheater);
        //mListViewTheater.setAdapter(new TheaterAdapter(((FeedActivity))));

        if (getArguments().getString("theater_id") != null) {
            mCurrentTheater = getResult().mTheaters.get(getArguments().getString("theater_id"));
        } else if (getArguments().getSerializable("theater") != null){
            mCurrentTheater = (MovieTheater)getArguments().getSerializable("theater");
        }

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);

        mLocation = ((FeedActivity)getActivity()).getLocation();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                configureView();
                requestTheaterInfos();
            }


        }, 100);

        return mRootView;
    }

    public ShowTimesFeed getResult() {
        if (getActivity() != null) {
            return ((FeedActivity) getActivity()).getResults();
        }
        return null;
    }


    private void configureView() {
        mListViewTheater.setAdapter(new TheaterAdapter());
    }

    private void requestTheaterInfos() {
        ApiUtils.instance().retrieveTheaterInfos(getActivity(), mCurrentTheater, new OnRetrieveTheaterInfoCompleted() {
            @Override
            public void onRetrieveTheaterCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
                getResult().addNewTheaterInfos(mCurrentTheater, dataset);
                configureView();
            }

            @Override
            public void onRetrieveTheaterError(String errorMessage) {

            }
        });
    }

    /**
     *
     */
    class TheaterAdapter extends BaseAdapter {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private static final int TYPE_MAX_COUNT = TYPE_ITEM + 1;

        private LayoutInflater mInflater;
        private LinkedHashMap<Movie, ArrayList<ShowTime>> mDataset;

        public TheaterAdapter() {
            mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDataset = getResult().getShowTimesByMoviesForTheater(mCurrentTheater.mId);

        }

        @Override
        public int getCount() {
            if (mDataset != null)
                return mDataset.size() + 1;
            return 0;
        }

        @Override
        public Object getItem(int position) {
         return null;
        }

        @Override
        public long getItemId(int position) {
         return 0;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_HEADER;
            else
                return TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case TYPE_HEADER:
                        convertView = mInflater.inflate(R.layout.header_theater_item, null);
                        vh = new ViewHolder();
                        vh.theaterName = (TextView) convertView.findViewById(R.id.theaterFragmentName);
                        vh.theaterInfos = (TextView) convertView.findViewById(R.id.theaterFragmentInfo);
                        vh.theaterDistance = (TextView) convertView.findViewById(R.id.theaterFragmentDistance);
                        break;
                    case TYPE_ITEM:
                        convertView = mInflater.inflate(R.layout.movie_theater_item, null);
                        vh = new ViewHolder();
                        vh.movieName = (TextView)convertView.findViewById(R.id.movieNameForTheater);
                        vh.movieInfos = (TextView)convertView.findViewById(R.id.movieInfosForTheater);
                        vh.movieInfos = (TextView)convertView.findViewById(R.id.movieInfosForTheater);
                        vh.moviePoster = (ImageView) convertView.findViewById(R.id.moviePosterForTheater);
                        vh.showtimesLayout = (FlowLayout) convertView.findViewById(R.id.movieShowTimesForTheaterLayout);
                        break;
                    default:
                        break;
                }
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            switch (type) {
                case TYPE_HEADER:
                    vh.theaterName.setText(mCurrentTheater.mName);
                    vh.theaterInfos.setText(mCurrentTheater.mAddress);
                    vh.theaterDistance.setText(mCurrentTheater.mDistance + " km");
                    break;
                case TYPE_ITEM:
                    Movie movie = new ArrayList<Movie>(mDataset.keySet()).get(position - 1);
                    vh.movieName.setText(movie.title);
                    vh.movieInfos.setText(movie.duration_time);

                    if (movie.poster_path != null && movie.poster_path.length() > 0) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.moviePoster);
                    } else if (mCachedMovies != null && mCachedMovies.containsKey(movie.id_g)
                            && mCachedMovies.get(movie.id_g).poster_path != null
                            && mCachedMovies.get(movie.id_g).poster_path.length() > 0) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(movie.id_g).poster_path;
                        Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.moviePoster);
                    }

                    ArrayList<ShowTime> sts = mDataset.get(movie);
                    if (vh.showtimesLayout.getChildCount() > 0)
                        vh.showtimesLayout.removeAllViews();
                    if (sts != null) {
                        for (ShowTime st : sts) {
                            TextView stv = new TextView(getActivity());
                            stv.setTextSize(16.0f);
                            stv.setText(st.mShowTimeStr);
                            stv.setPadding(8, 5, 8, 5);
                            vh.showtimesLayout.addView(stv);
                        }
                    }
                    break;
            }

            return convertView;
        }



        private  class ViewHolder {
            TextView theaterName;
            TextView theaterInfos;
            TextView theaterDistance;
            ImageView moviePoster;
            TextView movieName;
            TextView movieInfos;
            FlowLayout showtimesLayout;
        }
    }
}
