package com.ludoscity.findmybikes;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterItem;
import com.ludoscity.findmybikes.Citybik_esAPI.model.Station;
import com.ludoscity.findmybikes.Helpers.DBHelper;

import java.io.UnsupportedEncodingException;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Simple item holding the data necessary for each stations to be shown in listViewAdapter
 */
public class StationItem implements Parcelable, ClusterItem {
    private String id;
    private String name;
    private boolean locked;
    private int empty_slots;
    private int free_bikes;
    private double latitude;
    private double longitude;
   // private LatLng position;
    private String timestamp;

    public StationItem(String id, String name, LatLng position, int free_bikes, int empty_slots, String timestamp, boolean locked) {
        this.id = id;
        this.name = name;
        this.locked = locked;
        this.empty_slots = empty_slots;
        this.free_bikes = free_bikes;
        this.latitude = position.latitude;
        this.longitude = position.longitude;
        //this.position = position;
        this.timestamp = timestamp;
    }

    public StationItem(Station _station, Context ctx) {

        this.id = _station.id;

        if (null != _station.extra.name) {
            try {
                //Ugly hack. Character encoding for the bixi system is weird.
                if (DBHelper.isBixiNetwork(ctx))
                    this.name = new String(_station.extra.name.getBytes("ISO-8859-1"), "UTF-8");
                else
                    this.name = new String(_station.extra.name.getBytes("UTF-8"), "UTF-8");
                //
            } catch (UnsupportedEncodingException e) {
                Log.d("StationItem constructor", "String trouble",e );
            }
        }
        else {
            try {
                this.name = new String(_station.name.getBytes("UTF-8"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.d("StationItem constructor", "String trouble", e);
            }
        }

        if (null != _station.extra.locked)
            this.locked = _station.extra.locked;

        this.empty_slots = _station.empty_slots;
        this.free_bikes = _station.free_bikes;
        this.latitude = _station.latitude;
        this.longitude = _station.longitude;
        //this.position = new LatLng(_station.latitude, _station.longitude);
        this.timestamp = _station.timestamp;
    }

    public StationItem(Parcel in){
        id = in.readString();
        name = in.readString();
        locked = in.readByte() != 0;
        empty_slots = in.readInt();
        free_bikes = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        //position = in.readParcelable(LatLng.class.getClassLoader());
        timestamp = in.readString();
    }


    public String getId() {
        return id;
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

    public boolean isFavorite(Context ctx) {
        return DBHelper.isFavorite(id, ctx);
    }

    public void setFavorite(Boolean b, Context ctx){
        DBHelper.updateFavorite(b, id, ctx);
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
        dest.writeString(id);
        dest.writeString(name);
        dest.writeByte((byte) (locked ? 1 : 0));
        dest.writeInt(empty_slots);
        dest.writeInt(free_bikes);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        //dest.writeParcelable(position, flags);
        dest.writeString(timestamp);
    }

    public String getDistanceStringFromLatLng(LatLng currentUserLatLng) {
        int distance = (int) getMeterFromLatLng(currentUserLatLng);
        if (distance < 1000)
            return "" + distance + " m";
        distance = distance/1000;
        return String.format("%d.3",distance) + " km";
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

        @Override
        public StationItem createFromParcel(Parcel source) {
            return new StationItem(source);
        }

        @Override
        public StationItem[] newArray(int size) {
            return new StationItem[size];
        }
    };


}
