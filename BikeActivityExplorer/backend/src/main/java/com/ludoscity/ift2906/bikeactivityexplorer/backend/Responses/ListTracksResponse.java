package com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses;

import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.Track;

import java.util.List;

/**
 * Created by F8Full on 2015-03-14.
 * Specific response class for listTracks endpoint method.
 */
@SuppressWarnings("unused") //getTrackList() required for serialization
public class ListTracksResponse extends BaseResponse {

    public ListTracksResponse(List<Track> _responseTrackList) {
        mResponseTrackList = _responseTrackList;
    }

    private List<Track> mResponseTrackList;

    public List<Track> getTrackList() {
        return mResponseTrackList;
    }
}