package com.tiwence.instacine;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.tiwence.instacine.listener.OnSelectChoiceListener;
import com.tiwence.instacine.model.ShowTime;

/**
 * Created by temarill on 23/02/2015.
 */
public class TheaterFragmentDialog extends DialogFragment {

    private String movieTheaterName;
    private OnSelectChoiceListener mListener;
    private ShowTime mShowTime;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.theater_dialog_choice);
        int layoutId;
        if (mShowTime != null) {
            layoutId = R.array.theater_choices_array;
        } else {
            layoutId = R.array.theater_choices_array_2;
        }
        builder.setItems(layoutId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onSelectedChoice(movieTheaterName, mShowTime, which);
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnSelectChoiceListener) activity;
    }

    public void setMovieTheaterName(String movieTheaterName) {
        this.movieTheaterName = movieTheaterName;
    }

    public void setShowtime(ShowTime showtime) {
        this.mShowTime = showtime;
    }
}
