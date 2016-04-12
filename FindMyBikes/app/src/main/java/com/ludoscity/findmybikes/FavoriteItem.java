package com.ludoscity.findmybikes;

/**
 * Created by F8Full on 2016-03-31.
 * Simple item holding data necessary to diplay a favorite
 */
public class FavoriteItem {

    private String mStationId;
    private String mDisplayName;
    private boolean mDisplayNameIsDefault;

    public FavoriteItem(String _stationId, String _name, boolean _displayNameIsDefault){
        mStationId = _stationId;
        mDisplayName = _name;
        mDisplayNameIsDefault = _displayNameIsDefault;
    }

    public String getDisplayName() { return mDisplayName; }

    public String getStationId() { return mStationId; }

    public boolean isDisplayNameDefault() { return  mDisplayNameIsDefault; }
}
