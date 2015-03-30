package com.udem.ift2906.bixitracksexplorer.BixiAPI;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class Network{
    public ArrayList<BixiStation> stations;
    private ArrayList<MarkerOptions> listOfMarkersOptions;

    public ArrayList<MarkerOptions> getListOfMarkersOptions() {
        return listOfMarkersOptions;
    }

    public void setUpMarkers(){
        listOfMarkersOptions = new ArrayList<>();
        for (BixiStation item: stations){
            item.setUpMarker();
            listOfMarkersOptions.add(item.markerOptions);
        }
    }

    public void addMarkersToMap(GoogleMap nearbyMap) {
        for (MarkerOptions item: listOfMarkersOptions){
            nearbyMap.addMarker(item);
        }
    }
}
