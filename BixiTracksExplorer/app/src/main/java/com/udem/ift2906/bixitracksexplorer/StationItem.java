package com.udem.ift2906.bixitracksexplorer;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.clustering.ClusterItem;
import com.udem.ift2906.bixitracksexplorer.BixiAPI.BixiStation;

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
    private LatLng position;
    private boolean isFavorite;
    private MarkerOptions markerOptions;
    private GroundOverlayOptions groundOverlayOptions;
    private String timestamp;

    private static final BitmapDescriptor redIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_red);
    private static final BitmapDescriptor greyIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_grey);
    private static final BitmapDescriptor greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_green);
    private static final BitmapDescriptor yellowIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_yellow);

    public void setUpMarker(){
        //TODO Changes to markers are to be done here (for now)
        markerOptions = new MarkerOptions()
                .position(position)
                .title(name)
                .alpha(0)
                .anchor(0.5f,0.5f)
                .infoWindowAnchor(0.5f,0.5f);
        if (!locked)
            markerOptions.snippet(MainActivity.resources.getString(R.string.bikesAvailable) + free_bikes + "/" + (empty_slots + free_bikes));
        else
            markerOptions.snippet(MainActivity.resources.getString(R.string.stationIsLocked));
        // Since googleMap doesn't allow marker resizing we have to use ground overlay to not clog the map when we zoom out...
        groundOverlayOptions = new GroundOverlayOptions()
                .position(position, 50)
                .transparency(0.1f);
        if (locked)
            groundOverlayOptions.image(greyIcon);
        else if (free_bikes == 0)
            groundOverlayOptions.image(redIcon);
        else if (free_bikes < 3)
            groundOverlayOptions.image(yellowIcon);
        else
            groundOverlayOptions.image(greenIcon);
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
        setUpMarker();
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

    public GroundOverlayOptions getGroundOverlayOptions() {return groundOverlayOptions;}

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

    public String getDistanceStringFromLatLng(LatLng currentUserLatLng) {
        int distance = (int) getMeterFromLatLng(currentUserLatLng);
        if (distance < 1000)
            return "" + distance + " m";
        distance = distance/1000;
        return String.format("%d.3",distance) + " km";
    }
}
