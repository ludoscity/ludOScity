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
import android.util.TypedValue;

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