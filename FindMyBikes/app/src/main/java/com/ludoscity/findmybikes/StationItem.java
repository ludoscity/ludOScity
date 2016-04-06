package com.ludoscity.findmybikes;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterItem;
import com.ludoscity.findmybikes.citybik_es.model.Station;
import com.ludoscity.findmybikes.helpers.DBHelper;

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
                this.name = new String(_station.extra.name.getBytes("UTF-8"), "UTF-8");
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


        //Laurier / Brebeuf
        /*if (_station.id.equalsIgnoreCase("f132843c3c740cce6760167985bc4d17")){
            this.empty_slots = 35;
            this.free_bikes = 0;

            //Lanaudiere / Laurier
        }else if (_station.id.equalsIgnoreCase("92d97d6adec177649b366c36f3e8e2ff")){
            this.empty_slots = 17;
            this.free_bikes = 2;

        }else if (_station.id.equalsIgnoreCase("d20fea946f06e7e64e6da7d95b3c3a89")){
            this.empty_slots = 1;
            this.free_bikes = 19;
        }else if (_station.id.equalsIgnoreCase("3500704c9971a0c13924e696f5804bbd")){
            this.empty_slots = 0;
            this.free_bikes = 31;
        } else {*/
        this.empty_slots = _station.empty_slots;
        this.free_bikes = _station.free_bikes;
        //}
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

    //public double getBearingFromLatLng(LatLng userLocation){
    //    return SphericalUtil.computeHeading(userLocation, new LatLng(latitude,longitude) );}

    public LatLng getPosition() {return new LatLng(latitude,longitude);}

    public boolean isFavorite(Context ctx) {
        return DBHelper.isFavorite(id, ctx);
    }

    public void setFavorite(Boolean b, Context ctx){
        DBHelper.updateFavorite(b, id, name, ctx);
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

    public String getProximityStringFromLatLng(LatLng _targetLatLng, boolean _asDistance, float _speedKmh, Context _ctx) {

        String toReturn;

        int distance = (int) getMeterFromLatLng(_targetLatLng);

        if (_asDistance) {
            if (distance < 1000)
                toReturn = "" + distance + " m";
            else {
                distance = distance / 1000;
                toReturn = String.format("%d.3", distance) + " km";
            }
        }
        else {
            //I want a result in milliseconds
            float speedMetersPerH = _speedKmh * 1000f;
            float speedMetersPerS = speedMetersPerH / 3600f;

            float timeInS = distance / speedMetersPerS;

            long timeInMs = (long) (timeInS * 1000);

            if (timeInMs < 60000)
                toReturn = "< 1" + _ctx.getString(R.string.min);
            else if (timeInMs < 3600000 )
                toReturn = "~" + timeInMs / 1000 / 60 + _ctx.getString(R.string.min);
            else
                toReturn = "> 1" + _ctx.getString(R.string.hour_symbol);
        }

        return toReturn;
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
