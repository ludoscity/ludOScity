package com.ludoscity.findmybikes;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by F8Full on 2015-07-12.
 * This class is intended to retain the nescessary components to create and display a marker on a Google map
 * It's been created when StationItem was rendered jesonable
 */
public class StationMapGfx {

    private MarkerOptions markerOptions;
    private GroundOverlayOptions groundOverlayOptions;

    private final StationItem mItem; //corresponding data
    private Marker marker;
    private GroundOverlay groundOverlay;

    private static final BitmapDescriptor redIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_red);
    private static final BitmapDescriptor greyIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_grey);
    private static final BitmapDescriptor greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_green);
    private static final BitmapDescriptor yellowIcon = BitmapDescriptorFactory.fromResource(R.drawable.station_icon_yellow);

    private static final float OVERLAY_SIZE_BASE = 50;

    public StationMapGfx(StationItem item, boolean lookingForBike, Context context){

        mItem = item;

        //Marker setup
        markerOptions = new MarkerOptions()
                .position(item.getPosition())
                .title(item.getName())
                .alpha(0)
                .anchor(0.5f,0.5f)
                .infoWindowAnchor(0.5f,0.5f);
        if (!item.isLocked()) {
            String parkings;
            String bikes;
            if(item.getEmpty_slots() < 2) parkings = context.getString(R.string.parking) +": ";
            else parkings = context.getString(R.string.parkings)+": ";
            if (item.getFree_bikes() < 2) bikes = context.getString(R.string.bike)+": ";
            else bikes = context.getString(R.string.bikes)+": ";
            markerOptions.snippet(bikes + item.getFree_bikes() + "   " + parkings + item.getEmpty_slots());
        }
        else
            markerOptions.snippet(context.getString(R.string.stationIsLocked));
        // Since googleMap doesn't allow marker resizing we have to use ground overlay to not clog the map when we zoom out...
        groundOverlayOptions = new GroundOverlayOptions()
                .position(item.getPosition(), OVERLAY_SIZE_BASE)
                .transparency(0.1f);
        if (item.isLocked())
            groundOverlayOptions.image(greyIcon);
        else{
            if (lookingForBike){
                if (item.getFree_bikes() == 0)
                    groundOverlayOptions.image(redIcon);
                else if (item.getFree_bikes() < 3)
                    groundOverlayOptions.image(yellowIcon);
                else
                    groundOverlayOptions.image(greenIcon);
            }
            else{
                if (item.getEmpty_slots() == 0)
                    groundOverlayOptions.image(redIcon);
                else if (item.getEmpty_slots() < 3)
                    groundOverlayOptions.image(yellowIcon);
                else
                    groundOverlayOptions.image(greenIcon);
            }
        }
    }

    public void setInfoWindowVisible(boolean toSet){
        if (toSet)
            marker.showInfoWindow();
        else
            marker.hideInfoWindow();
    }

    public void setBigOverlay(boolean toSet){
        if (toSet)
            groundOverlay.setDimensions(2.0f*OVERLAY_SIZE_BASE);
        else
            groundOverlay.setDimensions(OVERLAY_SIZE_BASE);
    }

    public void setGroundOverlayVisible(boolean toSet){
        groundOverlay.setVisible(toSet);
    }

    public void addMarkerToMap(GoogleMap map){
        marker = map.addMarker(markerOptions);
        groundOverlay = map.addGroundOverlay(groundOverlayOptions);
    }

    public String getMarkerTitle(){ return marker.getTitle(); }

    public String getStationId(){
        return mItem.getId();
    }

    public void invalidateMarker(){
        groundOverlay.setImage(greyIcon);
    }

    public void updateMarker(boolean isLookingForBikes) {
        if (isLookingForBikes){
            if (mItem.getFree_bikes() == 0)
                groundOverlay.setImage(redIcon);
            else if (mItem.getFree_bikes() < 3)
                groundOverlay.setImage(yellowIcon);
                // check if the overlay is not already green
            else if (mItem.getEmpty_slots() < 3)
                // overlay isn't green yet
                groundOverlay.setImage(greenIcon);
        } else {
            if (mItem.getEmpty_slots() == 0)
                groundOverlay.setImage(redIcon);
            else if (mItem.getEmpty_slots() < 3)
                groundOverlay.setImage(yellowIcon);
                // check if the overlay is not already green
            else if (mItem.getFree_bikes() < 3)
                // overlay isn't green yet
                groundOverlay.setImage(greenIcon);
        }
    }
}
