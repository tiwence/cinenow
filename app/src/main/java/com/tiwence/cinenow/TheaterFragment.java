package com.tiwence.cinenow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.adapter.SearchResultAdapter;
import com.tiwence.cinenow.adapter.ShowtimeAdapter;
import com.tiwence.cinenow.adapter.TheaterDistanceHelper;
import com.tiwence.cinenow.listener.OnQueryResultClickListener;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.listener.OnRetrieveTheaterShowTimeInfoCompleted;
import com.tiwence.cinenow.listener.OnSelectChoiceListener;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.model.ShowTimesFeed;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.ApplicationUtils;

import org.apmem.tools.layouts.FlowLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import it.sephiroth.android.library.widget.HListView;

/**
 * Created by temarill on 18/02/2015.
 */
public class TheaterFragment extends Fragment implements OnRetrieveQueryCompleted, SwipeRefreshLayout.OnRefreshListener {

    private View mRootView;
    private ListView mListViewTheater;
    MovieTheater mCurrentTheater;
    private Location mLocation;
    private LinkedHashMap<String, Movie> mCachedMovies;

    private MenuItem mSearchItem;
    private AutoCompleteTextView mEditSearch;

    private SwipeRefreshLayout mSwipeRefresh;

    private OnQueryResultClickListener mQueryResultClickListener;
    private OnSelectChoiceListener mSelectChoiceListener;
    private boolean isFullyLoaded;


    /**
     * Text watcher used to search movies or theaters
     */
    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            String queryName = mEditSearch.getText().toString()
                    .toLowerCase(Locale.getDefault()).trim();

            if (queryName.length() > 2) {
                if (mLocation != null) {
                    ApiUtils.instance().retrieveQueryInfo(mLocation, queryName, TheaterFragment.this);
                } else {
                    //TODO ?
                }
            }
        }
        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                  int arg3) {
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_theater, container, false);
        mListViewTheater = (ListView) mRootView.findViewById(R.id.listViewTheater);
        mSwipeRefresh = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeRefreshTheater);
        mSwipeRefresh.setOnRefreshListener(this);
        //mListViewTheater.setAdapter(new TheaterAdapter(((FeedActivity))));

        if (getArguments().getString("theater_id") != null) {
            mCurrentTheater = getResult().mTheaters.get(getArguments().getString("theater_id"));
        } else if (getArguments().getSerializable("theater") != null){
            mCurrentTheater = (MovieTheater)getArguments().getSerializable("theater");
        }

        mCachedMovies = (LinkedHashMap<String, Movie>) ApplicationUtils.getDataInCache(getActivity(), ApplicationUtils.MOVIES_FILE_NAME);

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
        ((FeedActivity)getActivity()).getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
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
        setHasOptionsMenu(true);
    }

    /**
     *
     */
    private void configureView() {
        mListViewTheater.setAdapter(new TheaterAdapter());
        mListViewTheater.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Movie movie = ((TheaterAdapter.ViewHolder)view.getTag()).mMovie;
                    MovieFragment mf = new MovieFragment();
                    Bundle b = new Bundle();
                    b.putString("movie_id", movie.title);
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
            mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDataset = getResult().getShowTimesByMoviesForTheater(mCurrentTheater.mName);
            Log.d("Size : ", "" + mDataset.size());
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
                    vh.theaterHeaderMapIcon.setTag(mCurrentTheater.mName);
                    vh.theaterHeaderMapIcon.setOnClickListener(this);
                    if (mCurrentTheater.mDistance >= 1000) {
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
                    final ArrayList<ShowTime> sts = getResult().getNextShowTimesByTheaterId(mCurrentTheater.mName);
                    vh.nextShowTimesListView.setAdapter(new ShowtimeAdapter(getActivity(),sts));
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
                                    .replace(R.id.mainContainer, mf)
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
                                getResult().mMovies.put(movie.title, movie);
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
                    showTime.mMovieId, ApplicationUtils.getTimeString(showTime.mTimeRemaining), showTime.mTheaterId) + " http://www.google.fr/?movies");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(getString(R.string.sharing_string),
                    showTime.mMovieId, ApplicationUtils.getTimeString(showTime.mTimeRemaining), showTime.mTheaterId));
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_with)));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_movie, menu);

        mSearchItem = menu.findItem(R.id.action_movie_search);
        mEditSearch = (AutoCompleteTextView) MenuItemCompat.getActionView(mSearchItem);
        mEditSearch.addTextChangedListener(textWatcher);
        mEditSearch.setHint(getString(R.string.search_placeholder));

        MenuItemCompat.setOnActionExpandListener(mSearchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mEditSearch.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mEditSearch.setText("");
                mEditSearch.clearFocus();
                return true;
            }
        });

        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_refresh).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                getActivity().getSupportFragmentManager().popBackStack();
                break;
        }

        return true;
    }

    @Override
    public void onRetrieveQueryDataset(ShowTimesFeed stf) {

    }

    @Override
    public void onRetrieveQueryCompleted(final List<Object> dataset) {
        if (dataset != null && dataset.size() > 0) {

            SearchResultAdapter searchedAppsAdapter = new SearchResultAdapter(
                    getActivity(), R.layout.spinner_search_item, dataset);
            mEditSearch.setAdapter(searchedAppsAdapter);
            mEditSearch.showDropDown();
            mEditSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mQueryResultClickListener.onQueryResultClicked(dataset, position, mSearchItem, mEditSearch);
                }
            });
        }
    }

    @Override
    public void onRetrieveQueryError(String errorMessage) {

    }

}
