package com.ludoscity.findmybikes.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by F8Full on 2015-04-30.
 *
 * Class with static utilities
 */
public class Utils {

    public static int dpToPx(float toConvert, Context ctx){
        /// Converts 66 dip into its equivalent px
        Resources r = ctx.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toConvert, r.getDisplayMetrics());
    }

    //Snackbar related utils
    public static class Snackbar{


        //A modified version of make that allows background color manipulation
        //as it is not currently supported through styles/theming
        //Should be done something like this
        /*<style name="MyCustomSnackbar" parent="Theme.AppCompat.Light">
        <item name="colorAccent">@color/theme_accent</item>
        <item name="android:textColor">@color/theme_textcolor_primary</item>
        <item name="android:background">@color/theme_primary_dark</item>
        </style>*/
        //Right now, textColor and action color are controlled through theming,
        //but not background color.
        public static android.support.design.widget.Snackbar makeStyled(@NonNull View _view, @StringRes int _textStringResId, @android.support.design.widget.Snackbar.Duration int _duration,
                                                                                  @ColorInt int _backgroundColor/*, @ColorInt int _textColor, @ColorInt int _actionTextColor*/ ){

            android.support.design.widget.Snackbar toReturn = android.support.design.widget.Snackbar.make(_view, _textStringResId, _duration );

            View snackbarView = toReturn.getView();

            /*//change snackbar action text color
            toReturn.setActionTextColor(_actionTextColor);

            // change snackbar text color
            int snackbarTextId = android.support.design.R.id.snackbar_text;
            TextView textView = (TextView)snackbarView.findViewById(snackbarTextId);
            textView.setTextColor(_textColor);*/

            // change snackbar background
            snackbarView.setBackgroundColor(_backgroundColor);

            return toReturn;
        }

    }



    /**
     * Created by F8Full on 2015-03-15.
     * Used to manipulate request result metadata and avoid repetitive code
     */
    public static class Connectivity extends BroadcastReceiver{

        private static boolean mConnected = false;

        public static boolean isConnected(Context context){

            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            mConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();

            if (!mConnected){
                //start listening to connectivity change
                ComponentName receiver = new ComponentName(context, Connectivity.class);

                PackageManager pm = context.getPackageManager();

                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            }

            return mConnected;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            mConnected = extras.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);

            if (mConnected){
                //stop listening to connectivity change
                ComponentName receiver = new ComponentName(context, Connectivity.class);

                PackageManager pm = context.getPackageManager();

                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }
}