package com.tiwence.instacine.listener;

import com.tiwence.instacine.model.ShowTime;

/**
 * Created by temarill on 23/02/2015.
 */
public interface OnSelectChoiceListener {
    public void onSelectedChoice(String movieTheaterName, ShowTime st, int position);
}
