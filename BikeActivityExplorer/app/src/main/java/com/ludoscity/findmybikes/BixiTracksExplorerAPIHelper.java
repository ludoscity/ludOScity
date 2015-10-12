package com.ludoscity.findmybikes;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.BixiTracksExplorerAPI;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.GetTrackFromTimeUTCKeyStringResponse;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.ListTracksResponse;

import java.io.IOException;

/**
 * Created by F8Full on 2015-04-11.
 * Class used to maintain an instance of BixiTracksExplorerAPI
 */
public class BixiTracksExplorerAPIHelper {

    private static BixiTracksExplorerAPI mAPIService = null;

    private BixiTracksExplorerAPIHelper(){}

    public static void init(){
        BixiTracksExplorerAPI.Builder builder =new BixiTracksExplorerAPI.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null)
                .setRootUrl("https://udem-ift2905-backend.appspot.com/_ah/api/")
                .setServicePath("bixiTracksExplorerAPI/v3/")
                //.setRootUrl("http://192.168.1.XX:8080/_ah/api/") // 10.0.2.2 is localhost's IP address in Android emulator
                //TO BE ABLE TO TARGET ON LAN, RUN THE LOCAL APPENGINE SERVER ENVIRONMENT ON 0.0.0.0 IP ADDRESS (NOT localhost)
                /*.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
                    @Override
                    public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
                        abstractGoogleClientRequest.setDisableGZipContent(true);
                    }
                })*/;


        mAPIService = builder.build();

    }

    public static ListTracksResponse listTrack(){
        ListTracksResponse toReturn = new ListTracksResponse();

        try {
            toReturn = mAPIService.listTracks().execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public static GetTrackFromTimeUTCKeyStringResponse retrieveFullTrack(String _key_timeUTC){

        GetTrackFromTimeUTCKeyStringResponse toReturn = new GetTrackFromTimeUTCKeyStringResponse();

        if (!_key_timeUTC.equalsIgnoreCase("null")) {
            try {
                toReturn = mAPIService.getTrackFromTimeUTCKeyString(_key_timeUTC).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return toReturn;
    }




}
