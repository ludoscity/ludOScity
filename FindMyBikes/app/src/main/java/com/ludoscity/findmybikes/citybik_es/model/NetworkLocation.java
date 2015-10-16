package com.ludoscity.findmybikes.citybik_es.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
public class NetworkLocation {
    public Double latitude;
    public Double longitude;
    public String city;

    public LatLng getAsLatLng(){ return new LatLng(latitude, longitude); }
}
