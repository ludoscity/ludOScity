package com.ludoscity.findmybikes.citybik_es.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

/**
 * Created by F8Full on 2015-10-10.
 * Data model class for citybik.es API
 */
public class NetworkDesc {

    public String href;
    public String id;
    public String name;
    public NetworkLocation location;

    public double getMeterFromLatLng(LatLng userLocation) {
        return SphericalUtil.computeDistanceBetween(userLocation, location.getAsLatLng());}

}
