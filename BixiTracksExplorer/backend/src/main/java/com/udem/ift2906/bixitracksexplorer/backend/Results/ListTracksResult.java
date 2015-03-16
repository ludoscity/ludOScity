package com.udem.ift2906.bixitracksexplorer.backend.Results;

import com.udem.ift2906.bixitracksexplorer.backend.Data.Track;

import java.util.List;

/**
 * Created by F8Full on 2015-03-14.
 * Specific answer class for listTracks endpoint methods.
 */
@SuppressWarnings("unused") //getTrackList() required for serialization
public class ListTracksResult extends BaseResult{

    public ListTracksResult(List<Track> resultList) {
        mTrackList = resultList;
    }

    private List<Track> mTrackList;

    public List<Track> getTrackList() {
        return mTrackList;
    }
}