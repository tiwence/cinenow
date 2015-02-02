package com.tiwence.cinenow;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;


/**
 * Created by temarill on 02/02/2015.
 */
public class MovieFragment extends  android.support.v4.app.Fragment {

    private View mRootView;
    private ImageView mBackdropView;
    private ImageView mPosterView;
    private TextView mMovieTitleView;
    private Movie mCurrentMovie;
    private ShowTimesFeed mResult;
    private LinkedHashMap<String, Movie> mCachedMovies;
    private TextView mMovieOverview;
    private TextView mMovieAverageView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie, container, false);
        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        mCurrentMovie = (Movie) getArguments().getSerializable("movie");
        configureView();

        return mRootView;
    }

    private void configureView() {
        mBackdropView = (ImageView) mRootView.findViewById(R.id.backdropImageView);
        mMovieTitleView = (TextView) mRootView.findViewById(R.id.movieTitleTextView);
        mMovieOverview = (TextView) mRootView.findViewById(R.id.overviewText);
        mMovieAverageView = (TextView) mRootView.findViewById(R.id.voteAverageTextView);
        mPosterView = (ImageView) mRootView.findViewById(R.id.moviePosterImageView);

        mMovieTitleView.setText(mCurrentMovie.title);
        mMovieAverageView.setText("" + mCurrentMovie.vote_average + "/10");
        mMovieOverview.setText(mCurrentMovie.overview);

        if (mCurrentMovie.poster_path != null && mCurrentMovie.poster_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mPosterView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.id_g)
                && mCachedMovies.get(mCurrentMovie.id_g).poster_path != null
                && mCachedMovies.get(mCurrentMovie.id_g).poster_path.length() >0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.id_g).poster_path;
        }

        if (mCurrentMovie.backdrop_path != null && mCurrentMovie.backdrop_path.length() > 0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCurrentMovie.backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mCurrentMovie.id_g)
                && mCachedMovies.get(mCurrentMovie.id_g).backdrop_path != null
                && mCachedMovies.get(mCurrentMovie.id_g).backdrop_path.length() >0) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mCurrentMovie.id_g).backdrop_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(mBackdropView);
        }

        //Get poster
        /*if (mResult.mMovies.get(mCurrentMovie.id_g).poster_path != null &&
                !mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mResult.mMovies.get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else if (mCachedMovies != null && mCachedMovies.containsKey(mShowTimes.get(position).mMovieId)
                && mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path != null
                && !mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path.equals("")) {
            String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(mShowTimes.get(position).mMovieId).poster_path;
            Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.mPoster);
        } else {
            final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.mPoster);
            ApiUtils.instance().retrieveMovieInfo(mResult.mMovies.get(mShowTimes.get(position).mMovieId), new OnRetrieveMovieInfoCompleted() {
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
        }*/
    }
}
