package com.tiwence.cinenow.adapter;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tiwence.cinenow.FeedActivity;
import com.tiwence.cinenow.MovieFragment;
import com.tiwence.cinenow.R;
import com.tiwence.cinenow.TheatersFragment;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;
import com.tiwence.cinenow.utils.ApiUtils;
import com.tiwence.cinenow.utils.TheatersUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import it.sephiroth.android.library.widget.AdapterView;
import it.sephiroth.android.library.widget.HListView;

/**
 * Created by temarill on 10/02/2015.
 */
public class TheaterAdapter extends BaseAdapter implements View.OnClickListener {

    ArrayList<MovieTheater> mTheaters;
    LayoutInflater mInflater;
    TheatersFragment mTheatersFragment;

    public TheaterAdapter(TheatersFragment tf, ArrayList<MovieTheater> theaters) {
        this.mTheaters = theaters;
        this.mInflater = LayoutInflater.from(tf.getActivity());
        this.mTheatersFragment = tf;
    }

    @Override
    public int getCount() {
        if (mTheaters != null)
            return mTheaters.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mTheaters != null)
            return mTheaters.get(position);
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
            convertView = mInflater.inflate(R.layout.theater_item, parent, false);
            vh = new ViewHolder();
            vh.mTheaterName = (TextView) convertView.findViewById(R.id.theaterNameText);
            vh.mHListView = (HListView) convertView.findViewById(R.id.hListView);
            convertView.setTag(vh);
        }
        vh = (ViewHolder) convertView.getTag();
        final MovieTheater mt = mTheaters.get(position);
        if (mt.mDistance >= 1000) {
            final WeakReference<TextView> distanceRef = new WeakReference<TextView>(vh.mTheaterName);
            new TheaterDistanceHelper(((FeedActivity)mTheatersFragment.getActivity()).getLocation(), mTheatersFragment.getResults(), distanceRef, true)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mt);
        } else {
            vh.mTheaterName.setText(mt.mName + " (" + mt.mDistance + " km)");
        }
        ShowTime showTimeTemp = new ShowTime();
        showTimeTemp.mTheaterId = mt.mName;
        vh.mTheaterName.setTag(showTimeTemp);
        vh.mTheaterName.setOnClickListener(TheaterAdapter.this);

        vh.mHListView.setAdapter(new ShowtimeAdapter(mTheatersFragment.getActivity(),
                mTheatersFragment.filteredShowTime(mTheatersFragment.getResults().getNextShowTimesByTheaterId(mt.mName))));
        vh.mHListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ShowTime st = mTheatersFragment.filteredShowTime(mTheatersFragment.getResults().getNextShowTimesByTheaterId(mt.mName)).get(i);
                MovieFragment mf = new MovieFragment();
                Bundle b = new Bundle();
                b.putString("movie_id", st.mMovieId);
                mf.setArguments(b);
                mTheatersFragment.getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, mf, st.mMovieId)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return convertView;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            ((FeedActivity)mTheatersFragment.getActivity()).showTheaterChoiceFragment((ShowTime)v.getTag());
        }
     }

    public class ViewHolder {
        TextView mTheaterName;
        HListView mHListView;
    }
}
