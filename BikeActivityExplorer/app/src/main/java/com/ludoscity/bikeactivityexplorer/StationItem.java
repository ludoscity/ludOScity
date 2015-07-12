package com.ludoscity.bikeactivityexplorer;

import android.os.Parcel;
import android.os.Parcelable;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterItem;
import com.ludoscity.bikeactivityexplorer.BixiAPI.BixiStation;
import com.ludoscity.bikeactivityexplorer.DBHelper.DBHelper;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each stations to be shown in listViewAdapter
 */
public class StationItem implements Parcelable, ClusterItem {
    private long uid;
    private String name;
    private boolean locked;
    private int empty_slots;
    private int free_bikes;
    private double latitude;
    private double longitude;
   // private LatLng position;
    private boolean isFavorite;
    private String timestamp;

    public StationItem(long uid, String name, LatLng position, int free_bikes, int empty_slots, String timestamp, boolean locked, boolean isFavorite) {
        this.uid = uid;
        this.name = name;
        this.locked = locked;
        this.empty_slots = empty_slots;
        this.free_bikes = free_bikes;
        this.latitude = position.latitude;
        this.longitude = position.longitude;
        //this.position = position;
        this.isFavorite = isFavorite;
        this.timestamp = timestamp;
    }

    // Constructor to be used ONLY when parsing the json file since it trims the name
    public StationItem(BixiStation _station, boolean isFavorite, String date) {
        this.uid = _station.extra.uid;
        // 'Hacky' remove first 7 characters which are a station code
        // ex: "6156 - Marie-Anne / de la Roche"
        this.name = _station.name.substring(7);
        this.locked = _station.extra.locked;
        this.empty_slots = _station.empty_slots;
        this.free_bikes = _station.free_bikes;
        this.latitude = _station.latitude;
        this.longitude = _station.longitude;
        //this.position = new LatLng(_station.latitude, _station.longitude);
        this.isFavorite = isFavorite;
        this.timestamp = date;
    }

    public StationItem(Parcel in){
        uid = in.readLong();
        name = in.readString();
        locked = in.readByte() != 0;
        empty_slots = in.readInt();
        free_bikes = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        //position = in.readParcelable(LatLng.class.getClassLoader());
        isFavorite = in.readByte() != 0;
        timestamp = in.readString();
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

    public int getFree_bikes() { return free_bikes; }

    public double getMeterFromLatLng(LatLng userLocation) {
        return SphericalUtil.computeDistanceBetween(userLocation, new LatLng(latitude,longitude));}

    public double getBearingFromLatLng(LatLng userLocation){
        return SphericalUtil.computeHeading(userLocation, new LatLng(latitude,longitude) );}

    public LatLng getPosition() {return new LatLng(latitude,longitude);}

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean b){
        isFavorite = b;
        try {
            DBHelper.updateFavorite(b, uid);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public boolean isLocked() {
        return locked;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeString(name);
        dest.writeByte((byte) (locked ? 1 : 0));
        dest.writeInt(empty_slots);
        dest.writeInt(free_bikes);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        //dest.writeParcelable(position, flags);
        dest.writeByte((byte) (isFavorite? 1:0));
        dest.writeString(timestamp);
    }

    public String getDistanceStringFromLatLng(LatLng currentUserLatLng) {
        int distance = (int) getMeterFromLatLng(currentUserLatLng);
        if (distance < 1000)
            return "" + distance + " m";
        distance = distance/1000;
        return String.format("%d.3",distance) + " km";
    }


}
