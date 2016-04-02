package com.ludoscity.findmybikes;

/**
 * Created by F8Full on 2016-03-31.
 * Simple item holding data necessary to diplay a favorite
 */
public class FavoriteItem {

    private String mId;
    private String mDisplayName;

    public FavoriteItem(String _id, String _name){
        mId = _id;
        mDisplayName = _name;
    }

    public String getDisplayName() {
        return mDisplayName;
    }
}
