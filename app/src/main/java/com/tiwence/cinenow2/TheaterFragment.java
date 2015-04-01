package com.tiwence.cinenow2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;
import com.tiwence.cinenow2.adapter.ShowtimeAdapter;
import com.tiwence.cinenow2.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow2.listener.OnQueryResultClickListener;
import com.tiwence.cinenow2.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow2.listener.OnRetrieveTheaterShowTimeInfoCompleted;
import com.tiwence.cinenow2.listener.OnSelectChoiceListener;
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

import it.sephiroth.android.library.widget.HListView;

/**
 * Created by temarill on 18/02/2015.
 */
public class TheaterFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private View mRootView;
    private ListView mListViewTheater;
    MovieTheater mCurrentTheater;
    private Location mLocation;
    private LinkedHashMap<String, Movie> mCachedMovies;

    private SwipeRefreshLayout mSwipeRefresh;

    private OnQueryResultClickListener mQueryResultClickListener;
    private OnSelectChoiceListener mSelectChoiceListener;
    private boolean isFullyLoaded;
    private FloatingActionButton mFavoritesButton;
    private ArrayList<MovieTheater> mFavoritesTheaters;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_theater, container, false);
        mListViewTheater = (ListView) mRootView.findViewById(R.id.listViewTheater);
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshTheater);
        mSwipeRefresh.setOnRefreshListener(this);
        mFavoritesButton = (FloatingActionButton) mRootView.findViewById(R.id.favoritesFloatingButton);
        //mListViewTheater.setAdapter(new TheaterAdapter(((FeedActivity))));

        if (getArguments().getString("theater_id") != null) {
            mCurrentTheater = getResult().mTheaters.get(getArguments().getString("theater_id"));
        } else if (getArguments().getSerializable("theater") != null){
            mCurrentTheater = (MovieTheater)getArguments().getSerializable("theater");
            getResult().mTheaters.put(mCurrentTheater.mId, mCurrentTheater);
            ApplicationUtils.saveDataInCache(getActivity(), getResult().mTheaters, ApplicationUtils.THEATERS_FILE_NAME);
        }

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);
        mFavoritesTheaters = (ArrayList<MovieTheater>) ApplicationUtils
                .getDataInCache(getActivity(), ApplicationUtils.FAVORITES_THEATERS_FILE_NAME);
        if (mFavoritesTheaters == null) mFavoritesTheaters = new ArrayList<>();

        mLocation = ((FeedActivity)getActivity()).getLocation();

        configureView();
        requestShowTimesTheaterInfos();

        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mQueryResultClickListener = (OnQueryResultClickListener) activity;
        mSelectChoiceListener = (OnSelectChoiceListener) activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFullyLoaded)
            refreshNextShowTimes();
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(false);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
        ((FeedActivity)getActivity()).getMActionBar()
                .setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
        ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public ShowTimesFeed getResult() {
        if (getActivity() != null) {
            return ((FeedActivity) getActivity()).getResults();
        }
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    /**
     *
     */
    private void configureView() {
        mListViewTheater.setAdapter(new TheaterAdapter());

        if(mFavoritesTheaters.contains(mCurrentTheater)) {
            mFavoritesButton.setIcon(R.drawable.ic_remove_favorite);
        } else {
            mFavoritesButton.setIcon(R.drawable.ic_add_favorite);
        }
        mFavoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFavoritesTheaters.contains(mCurrentTheater)) {
                    mFavoritesTheaters.remove(mCurrentTheater);
                    mFavoritesButton.setIcon(R.drawable.ic_add_favorite);
                    Toast.makeText(getActivity(), String.format(getString(R.string.remove_favorite_theater), mCurrentTheater.mName),
                            Toast.LENGTH_LONG).show();
                } else {
                    mFavoritesTheaters.add(mCurrentTheater);
                    mFavoritesButton.setIcon(R.drawable.ic_remove_favorite);
                    Toast.makeText(getActivity(), String.format(getString(R.string.add_favorite_theater), mCurrentTheater.mName),
                            Toast.LENGTH_LONG).show();
                }
                ApplicationUtils.saveDataInCache(getActivity(), mFavoritesTheaters, ApplicationUtils.FAVORITES_THEATERS_FILE_NAME);
            }
        });

        mListViewTheater.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    if (view.getTag() != null) {
                        Movie movie = ((TheaterAdapter.ViewHolder)view.getTag()).mMovie;
                        MovieFragment mf = new MovieFragment();
                        Bundle b = new Bundle();
                        b.putString("movie_id", movie.title);
                        mf.setArguments(b);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                .replace(R.id.mainContainer, mf, movie.title)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            }
        });
    }

    /**
     *
     */
    private void requestShowTimesTheaterInfos() {
        ApiUtils.instance().retrieveShowTimesTheaterInfos(getActivity(), mCurrentTheater, new OnRetrieveTheaterShowTimeInfoCompleted() {

            @Override
            public void onRetrieveTheaterShowTimeInfoCompleted(LinkedHashMap<Movie, ArrayList<ShowTime>> dataset) {
                if (getResult() != null) {
                    getResult().addNewTheaterInfos(mCurrentTheater, dataset);
                    configureView();
                    isFullyLoaded = true;
                }
            }

            @Override
            public void onRetrieveTheaterShowTimeInfoError(String errorMessage) {

            }
        });
    }

    @Override
    public void onRefresh() {
        refreshNextShowTimes();
    }

    /**
     *
     */
    public void refreshNextShowTimes() {
        mSwipeRefresh.setRefreshing(true);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getResult().filterNewNextShowTimes();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mSwipeRefresh.setRefreshing(false);
                configureView();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     *
     */
    class TheaterAdapter extends BaseAdapter implements View.OnClickListener {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;
        private static final int TYPE_NEXT_ITEM = 2;
        private static final int TYPE_NEXT_ITEM_SEPARATOR = 3;
        private static final int TYPE_ALL_ITEM_SEPARATOR = 4;
        private static final int TYPE_MAX_COUNT = TYPE_ALL_ITEM_SEPARATOR + 1;

        private LayoutInflater mInflater;
        private LinkedHashMap<Movie, ArrayList<ShowTime>> mDataset;

        public TheaterAdapter() {
            if (getActivity() != null) {
                mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mDataset = getResult().getShowTimesByMoviesForTheater(mCurrentTheater.mId);
            }
        }

        @Override
        public int getCount() {
            if (mDataset != null)
                return mDataset.size() + TYPE_ALL_ITEM_SEPARATOR;
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
            switch (position) {
                case 0 : return TYPE_HEADER;
                case 1 : return TYPE_NEXT_ITEM_SEPARATOR;
                case 2 : return TYPE_NEXT_ITEM;
                case 3 : return TYPE_ALL_ITEM_SEPARATOR;
                default: return TYPE_ITEM;
            }
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case TYPE_HEADER:
                        convertView = mInflater.inflate(R.layout.header_theater_item, null);
                        vh = new ViewHolder();
                        vh.theaterHeaderMapIcon = (ImageView) convertView.findViewById(R.id.theaterImagePlaceholder);
                        vh.theaterName = (TextView) convertView.findViewById(R.id.theaterFragmentName);
                        vh.theaterInfos = (TextView) convertView.findViewById(R.id.theaterFragmentInfo);
                        vh.theaterDistance = (TextView) convertView.findViewById(R.id.theaterFragmentDistance);
                        break;
                    case TYPE_NEXT_ITEM_SEPARATOR:
                    case TYPE_ALL_ITEM_SEPARATOR:
                        TextView header = new TextView(getActivity());
                        header.setTextColor(Color.WHITE);
                        header.setBackgroundColor(getResources().getColor(R.color.dark_gray));
                        header.setTextSize(17.0f);
                        header.setPadding(10, 10, 10, 10);
                        header.requestLayout();
                        convertView = header;
                        break;
                    case TYPE_NEXT_ITEM:
                        convertView = mInflater.inflate(R.layout.theater_item, null);
                        vh = new ViewHolder();
                        vh.theaterName = (TextView) convertView.findViewById(R.id.theaterNameText);
                        vh.nextShowTimesListView = (HListView) convertView.findViewById(R.id.hListView);
                        vh.mNoShowTimesPlaceholder = (TextView) convertView.findViewById(R.id.noshowtimesPlaceholder);
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
                    vh.theaterHeaderMapIcon.setTag(mCurrentTheater.mId);
                    vh.theaterHeaderMapIcon.setOnClickListener(this);
                    if (mCurrentTheater.mDistance >= 1000 || mCurrentTheater.mDistance < 0) {
                        final WeakReference<TextView> distanceRef = new WeakReference<TextView>(vh.theaterDistance);
                        new TheaterDistanceHelper(mLocation, getResult(), distanceRef, false)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mCurrentTheater);
                    } else {
                        vh.theaterDistance.setText(mCurrentTheater.mDistance + " km");
                    }
                    break;
                case TYPE_NEXT_ITEM_SEPARATOR:
                    ((TextView)convertView).setText(getString(R.string.header_next_showtimes));
                    break;
                case TYPE_ALL_ITEM_SEPARATOR:
                    ((TextView)convertView).setText(getString(R.string.header_all_showtimes));
                    break;
                case TYPE_NEXT_ITEM:
                    vh.theaterName.setVisibility(View.GONE);
                    final ArrayList<ShowTime> sts = getResult().getNextShowTimesByTheaterId(mCurrentTheater.mId);
                    if (sts != null && !sts.isEmpty()) {
                        vh.nextShowTimesListView.setAdapter(new ShowtimeAdapter(getActivity(), sts));
                        vh.mNoShowTimesPlaceholder.setVisibility(View.GONE);
                    } else {
                        vh.nextShowTimesListView.setVisibility(View.GONE);
                        vh.mNoShowTimesPlaceholder.setVisibility(View.VISIBLE);
                    }
                    vh.nextShowTimesListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> adapterView, View view, int i, long l) {
                            MovieFragment mf = new MovieFragment();
                            Bundle b = new Bundle();
                            b.putString("movie_id", sts.get(i).mMovieId);
                            mf.setArguments(b);
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                            android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                    .replace(R.id.mainContainer, mf, sts.get(i).mMovieId)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                    break;
                case TYPE_ITEM:
                    Movie movie = new ArrayList<Movie>(mDataset.keySet()).get(position - TYPE_ALL_ITEM_SEPARATOR);
                    vh.movieName.setText(movie.title);
                    vh.movieInfos.setText(movie.infos_g);
                    vh.mMovie = movie;

                    if (movie.poster_path != null && movie.poster_path.length() > 0) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                        Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.moviePoster);
                    } else if (mCachedMovies != null && mCachedMovies.containsKey(movie.title)
                            && mCachedMovies.get(movie.title).poster_path != null
                            && mCachedMovies.get(movie.title).poster_path.length() > 0) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(movie.title).poster_path;
                        Picasso.with(getActivity()).load(posterPath).placeholder(R.drawable.poster_placeholder).into(vh.moviePoster);
                    } else {
                        final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.moviePoster);
                        ApiUtils.instance().retrieveMovieInfo(getResult().mMovies.get(movie.title), new OnRetrieveMovieInfoCompleted() {
                            @Override
                            public void onRetrieveMovieInfoCompleted(Movie movie) {
                                if (getResult() != null && getResult().mMovies != null
                                        && movie != null && movie.title != null) {
                                    getResult().mMovies.put(movie.title, movie);
                                    String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path;
                                    if (imgViewRef != null && imgViewRef.get() != null)
                                        Picasso.with(getActivity()).load(posterPath)
                                                .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                                }
                            }

                            @Override
                            public void onRetrieveMovieError(String message) {

                            }
                        });
                    }

                    ArrayList<ShowTime> sts2 = mDataset.get(movie);
                    if (vh.showtimesLayout.getChildCount() > 0)
                        vh.showtimesLayout.removeAllViews();
                    if (sts2 != null) {
                        for (final ShowTime st : sts2) {
                            TextView stv = new TextView(getActivity());
                            stv.setTextSize(16.0f);
                            stv.setText(st.mShowTimeStr);
                            stv.setPadding(8, 5, 8, 5);
                            stv.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                        shareShowTime(st);
                                   }
                               });
                                    vh.showtimesLayout.addView(stv);
                        }
                    }
                    break;
            }

            return convertView;
        }

        private  class ViewHolder {
            TextView theaterName;
            HListView nextShowTimesListView;
            TextView theaterInfos;
            TextView theaterDistance;
            ImageView moviePoster;
            TextView movieName;
            TextView movieInfos;
            FlowLayout showtimesLayout;
            ImageView theaterHeaderMapIcon;
            Movie mMovie;
            TextView mNoShowTimesPlaceholder;
        }

        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                mSelectChoiceListener.onSelectedChoice((String)v.getTag(), null, 1);
            }
        }
    }

    /**
     *
     * @param showTime
     */
    private void shareShowTime(ShowTime showTime) {
        if (showTime != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.sharing_string),
                    showTime.mMovieId, ApplicationUtils.getTimeString(getActivity(), showTime.mTimeRemaining), showTime.mTheaterId) + " http://www.google.fr/?movies");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.sharing_string),
                    showTime.mMovieId, ApplicationUtils.getTimeString(getActivity(), showTime.mTimeRemaining), showTime.mTheaterId));
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_with)));
        }
    }

}
