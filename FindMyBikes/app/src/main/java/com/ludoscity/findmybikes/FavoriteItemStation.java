package com.ludoscity.findmybikes;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by F8Full on 2017-03-12.
 * Class representing a station favorite
 */

public class FavoriteItemStation extends FavoriteItemBase {

    private static final String STATION_FAVORITE_JSON_KEY_IS_DISPLAY_NAME_DEFAULT = "is_display_name_default";

    private boolean mDisplayNameIsDefault;

    private static final String TAG = "FavoriteItemStation";

    public FavoriteItemStation(String _id, String _name, boolean _displayNameIsDefault ) {
        super(_id, _name);
        mDisplayNameIsDefault = _displayNameIsDefault;
    }

    @Override
    public boolean isDisplayNameDefault() {
        return mDisplayNameIsDefault;
    }

    @Override
    public LatLng getLocation() {
        return null;
        //return DBHelper.getStation(getId()).getLocation();
        //TODO: Rework saving algorithm. See DBHelper.getStation
    }

    @Override
    public JSONObject toJSON() {
        JSONObject toReturn = new JSONObject();

        try {
            super.toJSON(toReturn);
            toReturn.put(STATION_FAVORITE_JSON_KEY_IS_DISPLAY_NAME_DEFAULT, mDisplayNameIsDefault);
        } catch (JSONException e) {
            Log.e(TAG, "Error in JSON conversion", e );
        }

        return toReturn;
    }

    public static FavoriteItemBase fromJSON(JSONObject _from) {
        try {
            return new FavoriteItemStation(_from.getString(FAVORITE_JSON_KEY_ID),
                    _from.getString(FAVORITE_JSON_KEY_DISPLAY_NAME),
                    _from.getBoolean(STATION_FAVORITE_JSON_KEY_IS_DISPLAY_NAME_DEFAULT));
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

        dest.writeByte((byte) (mDisplayNameIsDefault ? 1 : 0));
    }

    private FavoriteItemStation(Parcel in){
        super(in.readString(), in.readString());

        mDisplayNameIsDefault = in.readByte() != 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public FavoriteItemStation createFromParcel(Parcel source) {
            return new FavoriteItemStation(source);
        }

        @Override
        public FavoriteItemStation[] newArray(int size) {
            return new FavoriteItemStation[size];
        }
    };
}
