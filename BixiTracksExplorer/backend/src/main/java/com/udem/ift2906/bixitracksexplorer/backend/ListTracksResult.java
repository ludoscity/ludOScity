package com.udem.ift2906.bixitracksexplorer.backend;

import java.util.List;

/**
 * Created by F8Full on 2015-03-14.
 * Specific answer class for listTracks endpoint methods.
 */
public class ListTracksResult extends BaseResult{

    ListTracksResult(List<Track> resultList) {
        mTrackList = resultList;
    }

    private List<Track> mTrackList;

    public List<Track> getTrackList() {
        return mTrackList;
    }
}