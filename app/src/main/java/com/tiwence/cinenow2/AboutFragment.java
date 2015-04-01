package com.tiwence.cinenow2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tiwence.cinenow2.utils.ApplicationUtils;

/**
 * Created by temarill on 19/03/2015.
 */
public class AboutFragment extends Fragment implements View.OnClickListener {

    private View mAboutView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mAboutView = inflater.inflate(R.layout.fragment_about, container, false);

        TextView versionNumberTextView = (TextView) mAboutView.findViewById(R.id.textViewVersionNumber);

        try {
            String aboutText = this.getString(R.string.about_text, getString(R.string.app_name),
                    getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            versionNumberTextView.setText(aboutText);
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        mAboutView.findViewById(R.id.buttonShareApp).setOnClickListener(this);
        mAboutView.findViewById(R.id.buttonContact).setOnClickListener(this);
        mAboutView.findViewById(R.id.buttonReview).setOnClickListener(this);
        mAboutView.findViewById(R.id.aboutTextViewAPI).setOnClickListener(this);



        return mAboutView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_refresh).setVisible(false);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_movies).setVisible(true);
        ((FeedActivity)getActivity()).getMenu().findItem(R.id.action_favorites_theaters).setVisible(true);
        ((FeedActivity)getActivity()).getMActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_gray));
        ((FeedActivity) getActivity()).getMActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayHomeAsUpEnabled(true);
        ((FeedActivity) getActivity()).getMActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonShareApp:
                shareApp();
                break;
            case R.id.buttonContact:
                contact();
                break;
            case R.id.buttonReview:
                addReview();
                break;
            case R.id.aboutTextViewAPI:
                showApiDoc();
            default:
                break;
        }
    }

    private void showApiDoc() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(ApplicationUtils.MOVIE_DB_API_DOC));
        startActivity(webIntent);
    }

    private void addReview() {
        Intent webIntent = new Intent(Intent.ACTION_VIEW);
        webIntent.setData(Uri.parse(ApplicationUtils.PLAYSTORE_URL));
        startActivity(webIntent);
    }

    private void contact() {
        Intent contactIntent = new Intent(Intent.ACTION_SEND);
        contactIntent.setType("text/html");
        contactIntent.putExtra(android.content.Intent.EXTRA_EMAIL, "tiwence.inc@gmail.com");
        contactIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.contact_subject));
        startActivity(Intent.createChooser(contactIntent, this.getResources().getString(R.string.contact_via)));
    }

    private void shareApp() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        //sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.share_subject));
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, ApplicationUtils.PLAYSTORE_URL);
        startActivity(Intent.createChooser(sharingIntent, this.getResources().getString(R.string.share_via)));
    }
}
