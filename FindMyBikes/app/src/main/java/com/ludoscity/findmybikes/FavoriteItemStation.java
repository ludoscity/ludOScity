package com.ludoscity.findmybikes;

import android.util.Log;

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
}
