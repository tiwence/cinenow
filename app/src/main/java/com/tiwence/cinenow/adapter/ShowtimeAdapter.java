package com.tiwence.cinenow.adapter;

/**
 * Created by temarill on 10/02/2015.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.MovieFragment;
import com.tiwence.cinenow.R;
import com.tiwence.cinenow.TheatersFragment;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 *
 */
public class ShowtimeAdapter extends BaseAdapter {

    private ArrayList<ShowTime> mShowTimes;
    private LayoutInflater mInflater;
    private TheatersFragment mTheatersFragment;

    public ShowtimeAdapter(TheatersFragment tf, ArrayList<ShowTime> showTimes) {
        this.mShowTimes = showTimes;
        this.mInflater = LayoutInflater.from(tf.getActivity());
        this.mTheatersFragment = tf;
    }

    @Override
    public int getCount() {
        if (mShowTimes != null)
            return mShowTimes.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mShowTimes != null)
            return mShowTimes.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return this.getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.movie_item, parent, false);
            vh = new ViewHolder();
            vh.mTimeRemaining = (TextView) convertView.findViewById(R.id.movieTimeRemainingText);
            vh.mPoster = (ImageView) convertView.findViewById(R.id.moviePosterView);
            vh.mMovieTitle = (TextView) convertView.findViewById(R.id.movieTitleText);
            convertView.setTag(vh);
        }

        vh = (ViewHolder) convertView.getTag();
        vh.mTimeRemaining.setText(ApplicationUtils.getTimeString(mShowTimes.get(position).mTimeRemaining));
        vh.mMovieTitle.setText(mTheatersFragment.getResults().mMovies.get(mShowTimes.get(position).mMovieId).title);

        //Get poster
        if (mTheatersFragment.getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path != null &&
                !mTheatersFragment.getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mTheatersFragment.getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(mTheatersFragment.getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else if (mTheatersFragment.getCachedMovies() != null && mTheatersFragment.getCachedMovies().containsKey(mShowTimes.get(position).mMovieId)
                && mTheatersFragment.getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path != null
                && !mTheatersFragment.getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mTheatersFragment.getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(mTheatersFragment.getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else {
            final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
            ApiUtils.instance().retrieveMovieInfo(mTheatersFragment.getResults().mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
                @Override
                public void onRetrieveMovieInfoCompleted(Movie movie) {
                    String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                    if (imgViewRef != null && imgViewRef.get() != null)
                        Picasso.with(mTheatersFragment.getActivity()).load(posterPath)
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
    }
}
