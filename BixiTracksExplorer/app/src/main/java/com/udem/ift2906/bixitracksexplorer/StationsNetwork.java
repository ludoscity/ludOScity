package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;

import java.util.ArrayList;

/**
 * Created by Looney on 08-04-15.
 */
public class StationsNetwork {
    public ArrayList<StationItem> stations;
    private ArrayList<MarkerOptions> listOfMarkersOptions;

    public StationsNetwork() {
        stations = new ArrayList<>();
        listOfMarkersOptions = new ArrayList<>();
    }

    public ArrayList<MarkerOptions> getListOfMarkersOptions() {
        return listOfMarkersOptions;
    }

    public void setUpMarkers(){
        for (StationItem item: stations){
            item.setUpMarker();
            listOfMarkersOptions.add(item.getMarkerOptions());
        }
    }

    public void addMarkersToMap(GoogleMap nearbyMap) {
        for (MarkerOptions item: listOfMarkersOptions){
            nearbyMap.addMarker(item);
        }
    }
}
