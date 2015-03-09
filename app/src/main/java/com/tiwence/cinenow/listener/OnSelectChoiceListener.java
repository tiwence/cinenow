package com.tiwence.cinenow.listener;

import com.tiwence.cinenow.model.ShowTime;

/**
 * Created by temarill on 23/02/2015.
 */
public interface OnSelectChoiceListener {
    public void onSelectedChoice(String movieTheaterName, ShowTime st, int position);
}
