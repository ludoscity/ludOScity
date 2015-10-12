package com.ludoscity.ift2906.bikeactivityexplorer.backend.Responses;

import com.ludoscity.ift2906.bikeactivityexplorer.backend.Data.Track;

/**
 * Created by F8Full on 2015-03-16.
 * Specific response class for getTrackFromTimeUTCKeyString endpoint method.
 */
@SuppressWarnings("unused") //getTrack() required for serialization
public class GetTrackFromTimeUTCKeyStringResponse extends BaseResponse {

    public GetTrackFromTimeUTCKeyStringResponse(Track responseTrack) {
        mResponseTrack = responseTrack;
    }

    private Track mResponseTrack;

    public Track getTrack() {
        return mResponseTrack;
    }

}
