package com.tiwence.instacine.listener;

import android.view.MenuItem;
import android.widget.AutoCompleteTextView;

import java.util.List;

/**
 * Created by temarill on 23/02/2015.
 */
public interface OnQueryResultClickListener {
    public void onQueryResultClicked(List<Object> dataset, int position, MenuItem mSearchItem, AutoCompleteTextView mEditSearch);
}
