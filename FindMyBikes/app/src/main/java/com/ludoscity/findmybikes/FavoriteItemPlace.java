package com.ludoscity.findmybikes;

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
    private String mAttributions;

    private static final String TAG = "FavoriteItemPlace";

    public FavoriteItemPlace(String _id, String _name, LatLng _location, String _attributions) {
        super(PLACE_ID_PREFIX + _id, _name);
        mLocation = _location;
        mAttributions = _attributions;
    }

    //A place name should always be considered as non default (displayed in bold)
    @Override
    public boolean isDisplayNameDefault() {
        return false;
    }

    @Override
    public JSONObject toJSON() {

        JSONObject toReturn = new JSONObject();

        try {
            super.toJSON(toReturn);
            toReturn.put(PLACE_FAVORITE_JSON_KEY_LATITUDE, mLocation.latitude);
            toReturn.put(PLACE_FAVORITE_JSON_KEY_LONGITUDE, mLocation.longitude);
            toReturn.put(PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS, mAttributions);
        } catch (JSONException e) {
            Log.e(TAG, "Error in converting to JSON", e );
        }

        return toReturn;
    }


    public static FavoriteItemBase fromJSON(JSONObject _from) {
        try {
            return new FavoriteItemPlace(_from.getString(FAVORITE_JSON_KEY_ID),
                    _from.getString(FAVORITE_JSON_KEY_DISPLAY_NAME),
                    new LatLng(_from.getDouble(PLACE_FAVORITE_JSON_KEY_LATITUDE),
                            _from.getDouble(PLACE_FAVORITE_JSON_KEY_LONGITUDE)),
                    _from.getString(PLACE_FAVORITE_JSON_KEY_ATTRIBUTIONS));
        }
        catch (JSONException e){
            Log.e(TAG, "Error while converting from JSON", e);
            return null;
        }
    }
}
