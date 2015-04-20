package com.udem.ift2906.bixitracksexplorer;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each stations to be shown in listViewAdapter
 */
public class StationItem implements Parcelable {
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
                .snippet("Bikes available: " + free_bikes + "/" + (empty_slots + free_bikes));
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

    public StationItem(Parcel in){
        uid = in.readLong();
        name = in.readString();
        locked = in.readByte() != 0;
        empty_slots = in.readInt();
        free_bikes = in.readInt();
        position = in.readParcelable(LatLng.class.getClassLoader());
        isFavorite = in.readByte() != 0;
        timestamp = in.readString();
        setUpMarker();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uid);
        dest.writeString(name);
        dest.writeByte((byte) (locked? 1:0));
        dest.writeInt(empty_slots);
        dest.writeInt(free_bikes);
        dest.writeParcelable(position, flags);
        dest.writeByte((byte) (isFavorite? 1:0));
        dest.writeString(timestamp);
    }
}
