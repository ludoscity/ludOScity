package com.ludoscity.bikeactivityexplorer;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ExpandableListView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.BixiTracksExplorerAPI;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.ListTracksResponse;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.TrackCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by F8Full on 2015-02-19.
 * This ASyncTask sets up the backend service object in PreExecute and as a simple DoInBackground
 * code consisting in calling the listTracks endpoint method
 */
public class RetrieveTrackListTask extends AsyncTask<ExpandableListView, Void, Void> {

    private static BixiTracksExplorerAPI mBixiTracksExplorerService =null;

    Context mContext;
    ExpandableListView mExpListView;

    List<String> mListDataHeader;
    HashMap<String, List<String>> mListDataChild;

    public RetrieveTrackListTask(Context context)
    {
        mContext = context;
    }





//    @Override
//    protected Pair<List<String>, HashMap<String, List<String>>> doInBackground(Void... voids) {
//
//        Pair<List<String>, HashMap<String, List<String>>> toReturn;
//        toReturn = new Pair<List<String>, HashMap<String, List<String>>>(new ArrayList<String>(), new HashMap<String, List<String>>() );
//
//
//        mListDataHeader = new ArrayList<String>();
//        mListDataChild = new HashMap<String, List<String>>();
//
//        // Adding child data
//
//        toReturn.first.add("Top 250");
//        toReturn.first.add("Now Showing");
//        toReturn.first.add("Coming Soon..");
//
//        //mListDataHeader.add("Top 250");
//        //mListDataHeader.add("Now Showing");
//        //mListDataHeader.add("Coming Soon..");
//
//        // Adding child data
//        List<String> top250 = new ArrayList<String>();
//        top250.add("The Shawshank Redemption");
//        top250.add("The Godfather");
//        top250.add("The Godfather: Part II");
//        top250.add("Pulp Fiction");
//        top250.add("The Good, the Bad and the Ugly");
//        top250.add("The Dark Knight");
//        top250.add("12 Angry Men");
//
//        List<String> nowShowing = new ArrayList<String>();
//        nowShowing.add("The Conjuring");
//        nowShowing.add("Despicable Me 2");
//        nowShowing.add("Turbo");
//        nowShowing.add("Grown Ups 2");
//        nowShowing.add("Red 2");
//        nowShowing.add("The Wolverine");
//
//        List<String> comingSoon = new ArrayList<String>();
//        comingSoon.add("2 Guns");
//        comingSoon.add("The Smurfs 2");
//        comingSoon.add("The Spectacular Now");
//        comingSoon.add("The Canyons");
//        comingSoon.add("Europa Report");
//
//
//        toReturn.second.put(toReturn.first.get(0), top250); // Header, Child data
//        toReturn.second.put(toReturn.first.get(1), nowShowing);
//        toReturn.second.put(toReturn.first.get(2), comingSoon);
//        //mListDataChild.put(mListDataHeader.get(0), top250); // Header, Child data
//        //mListDataChild.put(mListDataHeader.get(1), nowShowing);
//        //mListDataChild.put(mListDataHeader.get(2), comingSoon);
//        return toReturn;
//    }

    @Override
    protected Void doInBackground(ExpandableListView... expandableListViews) {

        mListDataHeader = new ArrayList<>();
        mListDataChild = new HashMap<>();
        mExpListView = expandableListViews[0];

        TrackCollection collection = new TrackCollection();
        collection.setItems(new ArrayList<Track>());
        ListTracksResponse listTracksResponse;

        try {
            listTracksResponse = mBixiTracksExplorerService.listTracks().execute();

            //test of good reception of meta data
            //JSONObject responseMeta = new JSONObject(listTracksResponse.getMeta());
            //String license = responseMeta.getString("license");

            if (listTracksResponse.getTrackList() != null) {
                collection.setItems(listTracksResponse.getTrackList());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } /*catch (JSONException e) {
            e.printStackTrace();
        }*/

        int trackIdx = 0;

        for (Track t : collection.getItems())
        {
            String header = trackIdx + ". " + t.getKeyTimeUTC();
            mListDataHeader.add(header);

            List<String> trackDetails = new ArrayList<>();
            trackDetails.add(t.getName());
            trackDetails.add("Start reason : " + t.getStartReason());
            trackDetails.add("End reason : " + t.getEndReason());
            trackDetails.add("From : " + t.getStartStationName());
            trackDetails.add("To : " + t.getEndStationName());
            trackDetails.add("Helmet : " + String.valueOf(t.getHelmet()));
            trackDetails.add("Rating : " + String.valueOf(t.getRating()));

            mListDataChild.put(header, trackDetails);

            ++trackIdx;
        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

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


        mBixiTracksExplorerService = builder.build();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        ExpandableListAdapter expAdapter = new ExpandableListAdapter(mContext, mListDataHeader, mListDataChild);

        // setting list adapter
        mExpListView.setAdapter(expAdapter);
    }
}
