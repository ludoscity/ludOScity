package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;

/**
 * Created by Looney on 08-04-15.
 */
public class StationsNetwork {
    public ArrayList<StationItem> stations;

    public StationsNetwork() {
        stations = new ArrayList<>();
    }

    public void addMarkersToMap(GoogleMap map) {
        for (StationItem item: stations)
            item.addMarkerToMap(map);
    }
}
