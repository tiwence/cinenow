package com.tiwence.cinenow2.utils;

import android.content.Context;
import android.util.AttributeSet;

import com.nirhart.parallaxscroll.views.ParallaxScrollView;

/**
 * Created by temarill on 10/03/2015.
 */
public class MyParallaxScrollview extends ParallaxScrollView {

    public MyParallaxScrollview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MyParallaxScrollview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyParallaxScrollview(Context context) {
        super(context);
    }

    /*private OnScrollViewListener mOnScrollViewListener;

    public void setOnScrollViewListener(OnScrollViewListener l) {
        this.mOnScrollViewListener = l;
    }

    public interface OnScrollViewListener {
        void onScrollChanged(MyParallaxScrollview v, int l, int t, int oldl, int oldt );
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        //mOnScrollViewListener.onScrollChanged(this, l, t, oldl, oldt);
        super.onScrollChanged(l, t, oldl, oldt);
    }*/
}
