package com.tiwence.cinenow2.listener;

import com.tiwence.cinenow2.model.ShowTime;

/**
 * Created by temarill on 23/02/2015.
 */
public interface OnSelectChoiceListener {
    public void onSelectedChoice(String movieTheaterName, ShowTime st, int position);
}
