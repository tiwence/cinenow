package com.tiwence.cinenow;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tiwence.cinenow.adapter.SearchResultAdapter;
import com.tiwence.cinenow.listener.OnRetrieveQueryCompleted;
import com.tiwence.cinenow.listener.OnRetrieveTheaterShowTimeInfoCompleted;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by temarill on 18/02/2015.
 */
public class TheaterFragment extends Fragment implements OnRetrieveQueryCompleted {

    private View mRootView;
    private ListView mListViewTheater;
    MovieTheater mCurrentTheater;
    private Location mLocation;
    private LinkedHashMap<String, Movie> mCachedMovies;

    private MenuItem mSearchItem;
    private AutoCompleteTextView mEditSearch;

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
                requestShowTimesTheaterInfos();
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
                }
            }

            @Override
            public void onRetrieveTheaterShowTimeInfoError(String errorMessage) {

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
                    } else if (mCachedMovies != null && mCachedMovies.containsKey(movie.title)
                            && mCachedMovies.get(movie.title).poster_path != null
                            && mCachedMovies.get(movie.title).poster_path.length() > 0) {
                        String posterPath = ApiUtils.MOVIE_DB_POSTER_ROOT_URL + mCachedMovies.get(movie.title).poster_path;
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
        menu.findItem(R.id.action_theaters).setVisible(false);
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
                    if (position < dataset.size()) {
                        if (dataset.get(position) != null) {
                            MenuItemCompat.collapseActionView(mSearchItem);
                            if (dataset.get(position) instanceof Movie) {
                                Movie movie = (Movie) dataset.get(position);
                                mEditSearch.setText("");
                                MovieFragment mf = new MovieFragment();
                                Bundle b = new Bundle();
                                if (!getResult().mMovies.containsKey(movie.title)) {
                                    getResult().mMovies.put(movie.title, movie);
                                }
                                b.putString("movie_id", movie.title);
                                mf.setArguments(b);
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                        .replace(R.id.mainContainer, mf)
                                        .addToBackStack(null)
                                        .commit();
                            } else if (dataset.get(position) instanceof MovieTheater) {
                                MovieTheater theater = (MovieTheater) dataset.get(position);
                                mEditSearch.setText("");
                                TheaterFragment tf = new TheaterFragment();
                                Bundle b = new Bundle();
                                if (!getResult().mTheaters.containsKey(theater.mId)) {
                                    getResult().mTheaters.put(theater.mId, theater);
                                }
                                b.putString("theater_id", theater.mId);
                                tf.setArguments(b);
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                        .replace(R.id.mainContainer, tf)
                                        .addToBackStack(null)
                                        .commit();
                            }

                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRetrieveQueryError(String errorMessage) {

    }
}
