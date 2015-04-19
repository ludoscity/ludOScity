package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each stations to be shown in listViewAdapter
 */
public class StationItem {
    private long uid;
    private String name;
    private boolean locked;
    private int empty_slots;
    private int free_bikes;
    private LatLng position;
    private boolean isFavorite;
    private MarkerOptions markerOptions;
    private String timestamp;


    public void setUpMarker(){
        //TODO Changes to markers are to be done here (for now)
        markerOptions = new MarkerOptions()
                .position(position)
                .title(name)
                .snippet("Bikes available: " + free_bikes + "/" + (empty_slots+free_bikes));
    }

    public StationItem(BixiStation _station, boolean isFavorite, String date) {
        this.uid = _station.extra.uid;
        this.name = _station.extra.name;
        this.locked = _station.extra.locked;
        this.empty_slots = _station.empty_slots;
        this.free_bikes = _station.free_bikes;
        this.position = new LatLng(_station.latitude, _station.longitude);
        this.isFavorite = isFavorite;
        this.timestamp = date;

    }


    public long getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public int getEmpty_slots() {
        return empty_slots;
    }

    public int getFree_bikes() {
        return free_bikes;
    }

    public double getMeterFromLatLng(LatLng userLocation) {
        return SphericalUtil.computeDistanceBetween(userLocation, position);
    }

    public double getBearingFromLatLng(LatLng userLocation){
        return SphericalUtil.computeHeading(userLocation,position);
    }

    public LatLng getPosition() {
        return position;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public MarkerOptions getMarkerOptions() {
        return markerOptions;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
