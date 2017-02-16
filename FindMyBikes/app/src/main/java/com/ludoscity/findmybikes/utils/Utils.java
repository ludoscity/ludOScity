package com.ludoscity.findmybikes.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.ludoscity.findmybikes.StationRecyclerViewAdapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by F8Full on 2015-04-30.
 *
 * Class with static utilities
 */
public class Utils {

    public static String extractClosestAvailableStationIdFromProcessedString(String _processedString){

        //int debug0 = _processedString.indexOf(StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX);
        //int debug1 = StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length();
        //int debug2 = _processedString.length();


        //Either a station id followed by _AVAILABILITY_AOK
        //or
        //a station id followed by _AVAILABILITY_BAD
        //or
        //a station id followed by _AVAILABILITY_LCK

        //extract only first id
        String firstId = extractOrderedStationIdsFromProcessedString(_processedString).get(0);

        return firstId.length()>=32 ? firstId.substring(0, 32) : "";






        //everything went AOK
        /*if (_processedString.indexOf(StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX) != -1 &&
                _processedString.indexOf(StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX) + StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() ==
                        _processedString.length()){

            return _processedString.substring(0, _processedString.length() - StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() );

        }
        else {
            int debug3 = _processedString.lastIndexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE);

            //some availability troubles, let's just trim the end
            return _processedString.substring(0, _processedString.lastIndexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE));
        }*/
    }

    //citybik.es Ids, ordered by distance
    //get(0) is the id of the selected station with BAD or AOK availability
    public static List<String> extractOrderedStationIdsFromProcessedString(String _processedString){

        if (_processedString.isEmpty()){
            List<String> toReturn = new ArrayList<>();
            toReturn.add(_processedString);

            return toReturn;
        }

        //int startSequenceIdx = _processedString.lastIndexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE);

        /*int subStringStarIdxDebug = _processedString.lastIndexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE)
                + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length();*/

        /*String subStringDebug = _processedString.substring(_processedString.lastIndexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE)
                + StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length());*/


        //TODO: something is fishy here, couldn't figure out how to get the same result without intermediary debug labelled variable
        String debugSplit = _processedString.substring(_processedString.indexOf(StationRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE)
                        + StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length());

        //String[] debugSplitResult = debugSplit.split(String.format("(?<=\\G.{%d})", StationRecyclerViewAdapter.CRITICAL_AVAILABILITY_POSTFIX.length() + 32));

        List<String> toReturn = new ArrayList<>();
        toReturn.add(_processedString.substring(0,32 + StationRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() ));

        toReturn.addAll(splitEqually(debugSplit, StationRecyclerViewAdapter.CRITICAL_AVAILABILITY_POSTFIX.length() + 32));

        return toReturn;
    }

    private static List<String> splitEqually(String text, int size) {
        // Give the list the right capacity to start with. You could use an array
        // instead if you wanted.
        List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    //workaround from https://code.google.com/p/gmaps-api-issues/issues/detail?id=9011
    public static BitmapDescriptor getBitmapDescriptor(Context ctx, int id) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(ctx.getResources(), id, null);
        Bitmap bm = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bm);
    }

    public static float map(float x, float in_min, float in_max, float out_min, float out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static int dpToPx(float toConvert, Context ctx){
        /// Converts 66 dip into its equivalent px
        Resources r = ctx.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toConvert, r.getDisplayMetrics());
    }

    /**
     * Round to certain number of decimals
     *
     * @param d
     * @param decimalPlace
     * @return
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    /**
     * Returns a percentage value as a float from an XML resource file. The value can be optionally
     * rounded.
     *
     * @param _ctx
     * @param _resId
     * @param _rounded
     * @return float
     */
    public static float getPercentResource(Context _ctx, int _resId, boolean _rounded){
        TypedValue valueContainer = new TypedValue();
        _ctx.getResources().getValue(_resId, valueContainer, true);
        float toReturn = valueContainer.getFraction(1, 1);//http://stackoverflow.com/questions/11734470/how-does-one-use-resources-getfraction

        if (_rounded)
            toReturn = Utils.round(toReturn,2);

        return toReturn;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
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

            //didn't use to work but maybe newer SnackBar versions will support it ?
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