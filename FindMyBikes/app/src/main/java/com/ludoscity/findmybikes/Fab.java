package com.ludoscity.findmybikes;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import com.gordonwong.materialsheetfab.AnimatedFab;

/**
 * Created by F8Full on 2016-03-26.
 * from https://github.com/gowong/material-sheet-fab/blob/master/sample/src/main/java/com/gordonwong/materialsheetfab/sample/Fab.java
 */
public class Fab extends FloatingActionButton implements AnimatedFab {
    //private static final int FAB_ANIM_DURATION = 200;

    private boolean mIsShowAnimationRunning = false;

    public Fab(Context context) {
        super(context);
    }

    public Fab(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Fab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isShowRunning(){ return mIsShowAnimationRunning; }

    @Override
    public void show(float translationX, float translationY) {

        mIsShowAnimationRunning = true;

        show(new OnVisibilityChangedListener(){
            @Override
            public void onShown(FloatingActionButton fab) {
                mIsShowAnimationRunning = false;
                super.onShown(fab);
            }
        });
        // Set FAB's translation
        /*setTranslation(translationX, translationY);

        // Only use scale animation if FAB is hidden
        if (getVisibility() != View.VISIBLE) {
            // Pivots indicate where the animation begins from
            float pivotX = getPivotX() + translationX;
            float pivotY = getPivotY() + translationY;

            ScaleAnimation anim;
            // If pivots are 0, that means the FAB hasn't been drawn yet so just use the
            // center of the FAB
            if (pivotX == 0 || pivotY == 0) {
                anim = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
            } else {
                anim = new ScaleAnimation(0, 1, 0, 1, pivotX, pivotY);
            }

            // Animate FAB expanding
            anim.setDuration(FAB_ANIM_DURATION);
            anim.setInterpolator(getInterpolator());
            startAnimation(anim);
        }
        setVisibility(View.VISIBLE);*/

    }

    /*private void setTranslation(float translationX, float translationY) {
        animate().setInterpolator(getInterpolator()).setDuration(FAB_ANIM_DURATION)
                .translationX(translationX).translationY(translationY);
    }

    private Interpolator getInterpolator() {
        return AnimationUtils.loadInterpolator(getContext(), R.interpolator.msf_interpolator);
    }*/
}
