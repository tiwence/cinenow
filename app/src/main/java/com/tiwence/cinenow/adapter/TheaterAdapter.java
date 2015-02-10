package com.tiwence.cinenow.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tiwence.cinenow.MovieFragment;
import com.tiwence.cinenow.R;
import com.tiwence.cinenow.TheatersFragment;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.model.ShowTime;

import java.util.ArrayList;

import it.sephiroth.android.library.widget.AdapterView;
import it.sephiroth.android.library.widget.HListView;

/**
 * Created by temarill on 10/02/2015.
 */
public class TheaterAdapter extends BaseAdapter {

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
        vh.mTheaterName.setText(mt.mName);

        vh.mHListView.setAdapter(new ShowtimeAdapter(mTheatersFragment,
                mTheatersFragment.filteredShowTime(mTheatersFragment.getResults().getNextShowTimesByTheaterId(mt.mId))));
        vh.mHListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ShowTime st = mTheatersFragment.filteredShowTime(mTheatersFragment.getResults().getNextShowTimesByTheaterId(mt.mId)).get(i);
                MovieFragment mf = new MovieFragment();
                Bundle b = new Bundle();
                b.putString("movie_id", st.mMovieId);
                mf.setArguments(b);
                mTheatersFragment.getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.mainContainer, mf)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return convertView;
    }

    public class ViewHolder {
        TextView mTheaterName;
        HListView mHListView;
    }
}
