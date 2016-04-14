package com.ludoscity.findmybikes;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

/**
 * Created by F8Full on 2015-07-12.
 * This class is intended to retain the nescessary components to create and display a marker on a Google map
 * It's been created when StationItem was rendered jsonable
 */
public class StationMapGfx {

    private MarkerOptions markerOptions;
    private GroundOverlayOptions groundOverlayOptions;

    private final StationItem mItem; //corresponding data
    private Marker marker;
    private GroundOverlay groundOverlay;

    private static final BitmapDescriptor redIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_red);
    private static final BitmapDescriptor greyIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_grey);
    private static final BitmapDescriptor greenIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_green);
    private static final BitmapDescriptor yellowIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_yellow);

    //For linear mappig of zoom level to oberlay size. Empirically determined.
    private static float maxZoomOut = 13.75f;
    private static float maxZoomIn = 21;
    private static float minOverlaySize = 1;
    private static float maxOverlaySize = 50;

    public StationMapGfx(StationItem item, boolean lookingForBike, Context _ctx){

        mItem = item;

        //Marker setup
        markerOptions = new MarkerOptions()
                .position(item.getPosition())
                .title(item.getId())
                .alpha(0)
                .anchor(0.5f,0.5f)
                .infoWindowAnchor(0.5f,0.5f);

        // Since googleMap doesn't allow marker resizing we have to use ground overlay to not clog the map when we zoom out...
        groundOverlayOptions = new GroundOverlayOptions()
                .position(item.getPosition(), maxOverlaySize)
                .transparency(0.1f)
                .visible(false);
        if (item.isLocked())
            groundOverlayOptions.image(greyIcon);
        else{
            if (lookingForBike){
                if (item.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                    groundOverlayOptions.image(redIcon);
                else if (item.getFree_bikes() <= DBHelper.getBadAvailabilityMax(_ctx))
                    groundOverlayOptions.image(yellowIcon);
                else
                    groundOverlayOptions.image(greenIcon);
            }
            else{
                if (item.getEmpty_slots() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                    groundOverlayOptions.image(redIcon);
                else if (item.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(_ctx))
                    groundOverlayOptions.image(yellowIcon);
                else
                    groundOverlayOptions.image(greenIcon);
            }
        }
    }

    public void addMarkerToMap(GoogleMap map){
        marker = map.addMarker(markerOptions);
        groundOverlay = map.addGroundOverlay(groundOverlayOptions);
    }

    public String getMarkerTitle(){ return marker.getTitle(); }

    public LatLng getMarkerLatLng() { return marker.getPosition(); }

    public void updateMarker(boolean isLookingForBikes, Context _ctx) {
        if (isLookingForBikes){
            if (!mItem.isLocked()) {
                if (mItem.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                    groundOverlay.setImage(redIcon);
                else if (mItem.getFree_bikes() <= DBHelper.getBadAvailabilityMax(_ctx))
                    groundOverlay.setImage(yellowIcon);
                    // check if the overlay is not already green
                else if (mItem.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(_ctx))
                    // overlay isn't green yet
                    groundOverlay.setImage(greenIcon);
            } else
                groundOverlay.setImage(greyIcon);
        } else {
            if (!mItem.isLocked()) {
                if (mItem.getEmpty_slots() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                    groundOverlay.setImage(redIcon);
                else if (mItem.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(_ctx))
                    groundOverlay.setImage(yellowIcon);
                    // check if the overlay is not already green
                else if (mItem.getFree_bikes() <= DBHelper.getBadAvailabilityMax(_ctx))
                    // overlay isn't green yet
                    groundOverlay.setImage(greenIcon);
            } else
                groundOverlay.setImage(greyIcon);
        }
    }

    public void hide() {
        if (groundOverlay != null)
            groundOverlay.setVisible(false);
    }

    public void show(float _zoom) {

        groundOverlay.setDimensions(Utils.map(_zoom, maxZoomOut, maxZoomIn, maxOverlaySize, minOverlaySize));
        groundOverlay.setVisible(true);
    }
}
