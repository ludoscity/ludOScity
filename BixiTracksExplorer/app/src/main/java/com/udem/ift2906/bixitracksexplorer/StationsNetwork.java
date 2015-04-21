package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

/**
 * Created by Looney on 08-04-15.
 */
public class StationsNetwork {
    public ArrayList<StationItem> stations;
    public ArrayList<MarkerContainer> markerContainers;

    public class MarkerContainer{
        Marker marker;
        GroundOverlay groundOverlay;
        MarkerContainer(Marker marker, GroundOverlay groundOverlay){
            this.marker = marker;
            this.groundOverlay = groundOverlay;
        }
    }
    public StationsNetwork() {
        stations = new ArrayList<>();
        markerContainers = new ArrayList<>();
    }

    public void addMarkersToMap(GoogleMap map) {
        for (StationItem item: stations){
            markerContainers.add(new MarkerContainer(
                    map.addMarker(item.getMarkerOptions()),
                    map.addGroundOverlay(item.getGroundOverlayOptions()))
            );
        }
    }
}
