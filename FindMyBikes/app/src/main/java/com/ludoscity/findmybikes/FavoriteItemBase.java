package com.ludoscity.findmybikes;

import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by F8Full on 2016-03-31.
 * Simple item holding data necessary to diplay a favorite
 */
public abstract class FavoriteItemBase implements Parcelable {

    public static final String FAVORITE_JSON_KEY_ID = "favorite_id";
    public static final String FAVORITE_JSON_KEY_DISPLAY_NAME = "display_name";

    private String mId; //can be either stationID or placeID
    private String mDisplayName;


    protected FavoriteItemBase(String _id, String _name){
        mId = _id;
        mDisplayName = _name;
    }

    public String getDisplayName() { return mDisplayName; }

    public String getId() { return mId; }

    public CharSequence getAttributions(){ return null; }

    public abstract boolean isDisplayNameDefault();
    public abstract LatLng getLocation();

    public abstract JSONObject toJSON();

    protected JSONObject toJSON(JSONObject _jsonObject) throws JSONException {
        _jsonObject.put(FAVORITE_JSON_KEY_ID, mId);
        _jsonObject.put(FAVORITE_JSON_KEY_DISPLAY_NAME, mDisplayName);

        return _jsonObject;
    }
}
