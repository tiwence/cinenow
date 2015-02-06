package com.tiwence.cinenow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.tiwence.cinenow.listener.OnRetrieveMovieInfoCompleted;
import com.tiwence.cinenow.model.Movie;
import com.tiwence.cinenow.model.MovieTheater;
import com.tiwence.cinenow.utils.ApiUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by temarill on 06/02/2015.
 */
public class SearchResultAdapter extends ArrayAdapter<Object> {

    private Context mContext;

    public SearchResultAdapter(Context context, int resource, List<Object> objects) {
        super(context, resource, objects);
        mContext = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.spinner_search_item, null);
            vh = new ViewHolder();
            vh.searchImage = (ImageView) convertView.findViewById(R.id.spinnerSearchImage);
            vh.searchName = (TextView) convertView.findViewById(R.id.spinnerSearchName);
            vh.searchInfos = (TextView) convertView.findViewById(R.id.spinnerSearchInfos);
            convertView.setTag(vh);
        }

        vh = (ViewHolder) convertView.getTag();

        //
        Object data = getItem(position);
        if (data instanceof Movie) {
            vh.searchName.setText(((Movie)data).title);
            vh.searchInfos.setText(((Movie)data).infos_g);
            if (((Movie)data).poster_path != null) {
                Picasso.with(mContext).load(((Movie)data).poster_path).placeholder(R.drawable.poster_placeholder).into(vh.searchImage);
            } else {
                final WeakReference<ImageView> imgViewRef = new WeakReference<ImageView>(vh.searchImage);
                final Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        Log.d("IMAGE LOADED", from.name());
                        if (imgViewRef != null &&imgViewRef.get() != null) {
                            imgViewRef.get().setImageBitmap(bitmap);
                            imgViewRef.get().invalidate();
                        }
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        Log.d("IMAGE FAILED", "LOUL");
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) { }
                };
                ApiUtils.instance().retrieveMovieInfo((Movie)data, new OnRetrieveMovieInfoCompleted() {
                    @Override
                    public void onRetrieveMovieInfoCompleted(Movie movie) {
                        if (imgViewRef != null && imgViewRef.get() != null
                                && movie != null && movie.poster_path != null) {
                            Picasso.with(mContext).load(ApiUtils.MOVIE_DB_POSTER_ROOT_URL + movie.poster_path)
                                    .placeholder(R.drawable.poster_placeholder).into(imgViewRef.get());
                        }
                    }

                    @Override
                    public void onRetrieveMovieError(String message) {

                    }
                });
            }
        } else if (data instanceof MovieTheater) {
            vh.searchName.setText(((MovieTheater)data).mName);
            vh.searchInfos.setText(((MovieTheater)data).mAddress);
            //Picasso.with(mContext).load()
        }

        return convertView;
    }

    class ViewHolder {
        ImageView searchImage;
        TextView searchName;
        TextView searchInfos;
    }
}
