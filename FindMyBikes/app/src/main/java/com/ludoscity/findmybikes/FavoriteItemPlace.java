package com.ludoscity.findmybikes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by F8Full on 2017-03-12.
 * Class representing a place favorite
 */

public class FavoriteItemPlace extends FavoriteItemBase {

    private static final String PLACE_FAVORITE_JSON_KEY_LATITUDE = "latitude";
    private static final String PLACE_FAVORITE_JSON_KEY_LONGITUDE = "longitude";
    private static final String PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS = "attributions";
    //To avoid collisions with citybik.es API ids
    public static final String PLACE_ID_PREFIX = "PLACE_";

    private LatLng mLocation;
    private CharSequence mAttributions;

    private static final String TAG = "FavoriteItemPlace";

    public FavoriteItemPlace(String _id, String _name, LatLng _location, CharSequence _attributions) {
        super(PLACE_ID_PREFIX + _id, _name);
        mLocation = _location;
        mAttributions = _attributions;
    }

    public FavoriteItemPlace(FavoriteItemBase _from, String _newName){
        super(_from.getId(), _newName);

        mLocation = _from.getLocation();
        mAttributions = _from.getAttributions();
    }

    //A place name should always be considered as non default (displayed in bold)
    @Override
    public boolean isDisplayNameDefault() {
        return false;
    }

    @Override
    public LatLng getLocation() {
        return mLocation;
    }

    @Override
    public CharSequence getAttributions(){ return mAttributions;}

    @Override
    public JSONObject toJSON() {

        JSONObject toReturn = new JSONObject();

        try {
            super.toJSON(toReturn);
            toReturn.put(PLACE_FAVORITE_JSON_KEY_LATITUDE, mLocation.latitude);
            toReturn.put(PLACE_FAVORITE_JSON_KEY_LONGITUDE, mLocation.longitude);
            if (mAttributions != null)
                toReturn.put(PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS, mAttributions);
            else
                toReturn.put(PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS, "");
        } catch (JSONException e) {
            Log.e(TAG, "Error in converting to JSON", e );
        }

        return toReturn;
    }

    private FavoriteItemPlace(String _id, String _name){
        super(_id, _name);
    }


    public static FavoriteItemBase fromJSON(JSONObject _from) {
        try {
            FavoriteItemPlace toReturn = new FavoriteItemPlace(_from.getString(FAVORITE_JSON_KEY_ID),
                    _from.getString(FAVORITE_JSON_KEY_DISPLAY_NAME));

            toReturn.mLocation = new LatLng(_from.getDouble(PLACE_FAVORITE_JSON_KEY_LATITUDE),
                    _from.getDouble(PLACE_FAVORITE_JSON_KEY_LONGITUDE));

            toReturn.mAttributions = _from.getString(PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS);

            return toReturn;
        }
        catch (JSONException e){
            Log.e(TAG, "Error while converting from JSON", e);
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(getId());
        dest.writeString(getDisplayName());

        dest.writeParcelable(mLocation, i);
        if (mAttributions != null)
            dest.writeString(mAttributions.toString());
        else
            dest.writeString("");
    }

    private FavoriteItemPlace(Parcel in){
        super(in.readString(), in.readString());

        mLocation = in.readParcelable(LatLng.class.getClassLoader());
        mAttributions = in.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public FavoriteItemPlace createFromParcel(Parcel source) {
            return new FavoriteItemPlace(source);
        }

        @Override
        public FavoriteItemPlace[] newArray(int size) {
            return new FavoriteItemPlace[size];
        }
    };
}
