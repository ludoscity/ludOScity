package com.udem.ift2906.bixitracksexplorer;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each stations to be shown in listViewAdapter
 */
public class StationItem {
    private int uid;
    private String name;
    private Boolean locked;
    private int empty_slots;
    private int free_bikes;
    private LatLng position;
    private double meterFromUserLocation;

    StationItem(BixiStation _station) {
        uid = _station.extra.uid;
        name = _station.extra.name;
        locked = _station.extra.locked;
        empty_slots = _station.empty_slots;
        free_bikes = _station.free_bikes;
        position = new LatLng(_station.latitude, _station.longitude);
    }


    public int getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public Boolean isLocked() {
        return locked;
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

    public LatLng getPosition() {
        return position;
    }
}
