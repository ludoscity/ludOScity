package com.ludoscity.bikeactivityexplorer;

import android.os.Parcel;
import android.os.Parcelable;

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
    private LatLng position;
    private boolean isFavorite;
    private String timestamp;

    private boolean isSelected;

    private MarkerOptions markerOptions;
    private GroundOverlayOptions groundOverlayOptions;

    private Marker marker;
    private GroundOverlay groundOverlay;

    private static final BitmapDescriptor redIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_red);
    private static final BitmapDescriptor greyIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_grey);
    private static final BitmapDescriptor greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_green);
    private static final BitmapDescriptor yellowIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_yellow);

    public StationItem(long uid, String name, LatLng position, int free_bikes, int empty_slots, String timestamp, boolean locked, boolean isFavorite) {
        this.uid = uid;
        this.name = name;
        this.locked = locked;
        this.empty_slots = empty_slots;
        this.free_bikes = free_bikes;
        this.position = position;
        this.isFavorite = isFavorite;
        this.timestamp = timestamp;
        setUpMarker();
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

    public void setUpMarker(){
        //TODO isLookingForBike?
        markerOptions = new MarkerOptions()
                .position(position)
                .title(name)
                .alpha(0)
                .anchor(0.5f,0.5f)
                .infoWindowAnchor(0.5f,0.5f);
        if (!locked) {
            String parkings;
            String bikes;
            if(empty_slots < 2) parkings = MainActivity.resources.getString(R.string.parking) +": ";
            else parkings = MainActivity.resources.getString(R.string.parkings)+": ";
            if (free_bikes < 2) bikes = MainActivity.resources.getString(R.string.bike)+": ";
            else bikes = MainActivity.resources.getString(R.string.bikes)+": ";
            markerOptions.snippet(bikes + free_bikes + "   " + parkings + empty_slots);
        }
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
        return SphericalUtil.computeDistanceBetween(userLocation, position);}

    public double getBearingFromLatLng(LatLng userLocation){
        return SphericalUtil.computeHeading(userLocation,position);}

    public LatLng getPosition() {return position;}

    public Marker getMarker() {return marker;}

    public GroundOverlay getGroundOverlay() {
        return groundOverlay;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean isSelected(){return isSelected;}

    public void setSelected(boolean b) {isSelected = b;}

    public void setFavorite(Boolean b){
        isFavorite = b;
        DBHelper.updateFavorite(b,uid);
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

    public String getDistanceStringFromLatLng(LatLng currentUserLatLng) {
        int distance = (int) getMeterFromLatLng(currentUserLatLng);
        if (distance < 1000)
            return "" + distance + " m";
        distance = distance/1000;
        return String.format("%d.3",distance) + " km";
    }

    public void addMarkerToMap(GoogleMap map){
        marker = map.addMarker(markerOptions);
        groundOverlay = map.addGroundOverlay(groundOverlayOptions);
    }

    public void updateMarker(boolean isLookingForBikes) {
        if (isLookingForBikes){
            if (free_bikes == 0)
                groundOverlay.setImage(redIcon);
            else if (free_bikes < 3)
                groundOverlay.setImage(yellowIcon);
            // check if the overlay is not already green
            else if (empty_slots < 3)
                // overlay isn't green yet
                groundOverlay.setImage(greenIcon);
        } else {
            if (empty_slots == 0)
                groundOverlay.setImage(redIcon);
            else if (empty_slots < 3)
                groundOverlay.setImage(yellowIcon);
            // check if the overlay is not already green
            else if (free_bikes < 3)
                // overlay isn't green yet
                groundOverlay.setImage(greenIcon);
        }
    }
}
