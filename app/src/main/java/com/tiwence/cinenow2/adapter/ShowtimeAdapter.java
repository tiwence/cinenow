package com.tiwence.cinenow2.adapter;

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
import com.tiwence.cinenow2.FeedActivity;
import com.tiwence.cinenow2.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow2.model.ShowTime;
import com.tiwence.cinenow2.utils.ApplicationUtils;
import com.tiwence.cinenow2.R;
import com.tiwence.cinenow2.model.Movie;
import com.tiwence.cinenow2.model.ShowTimesFeed;
import com.tiwence.cinenow2.utils.ApiUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 */
public class ShowtimeAdapter extends BaseAdapter {

    private ArrayList<ShowTime> mShowTimes;
    private LayoutInflater mInflater;
    private Context mContext;
    //private TheatersFragment mTheatersFragment;

    public ShowtimeAdapter(Context context, ArrayList<ShowTime> showTimes) {
        this.mShowTimes = showTimes;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        //this.mTheatersFragment = tf;
    }

    @Override
    public int getCount() {
        if (mShowTimes != null)
            return mShowTimes.size();
        return 0;
    }

    public ShowTimesFeed getResults() {
        return ((FeedActivity)mContext).getResults();
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
        vh.mTimeRemaining.setText(ApplicationUtils.getTimeString(mContext, mShowTimes.get(position).mTimeRemaining));
        vh.mMovieTitle.setText(getResults().mMovies.get(mShowTimes.get(position).mMovieId).title);

        //Get poster
        if (getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path != null &&
                !getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + getResults().mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(mContext).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else if (getCachedMovies() != null && getCachedMovies().containsKey(mShowTimes.get(position).mMovieId)
                && getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path != null
                && !getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + getCachedMovies().get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(mContext).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else {
            final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
            ApiUtils.instance().retrieveMovieInfo(getResults().mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
                @Override
                public void onRetrieveMovieInfoCompleted(Movie movie) {
                    String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                    if (imgViewRef != null && imgViewRef.get() != null)
                        Picasso.with(mContext).load(posterPath)
                                .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                }

                @Override
                public void onRetrieveMovieError(String message) {

                }
            });
        }

        convertView.invalidate();
        convertView.requestLayout();

        return convertView;
    }

    public LinkedHashMap<String, Movie> getCachedMovies() {
        return ((FeedActivity)mContext).getCachedMovies();
    }

    public class ViewHolder {
        ImageView mPoster;
        TextView mMovieTitle;
        TextView mTimeRemaining;
    }
}
