package com.udem.ift2906.bixitracksexplorer.BixiAPI;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class BixiStation{
    public int empty_slots;
    public Extra extra;
    public int free_bikes;
    public String id;
    public Double latitude;
    public Double longitude;
    public String name;
    public String timestamp;

    public MarkerOptions markerOptions;

    public void setUpMarker(){
        //TODO Changes to markers are to be done here (for now)
        markerOptions = new MarkerOptions()
                .position(new LatLng(latitude,longitude))
                .title(name);
    }
}
