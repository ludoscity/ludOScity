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
    private static final BitmapDescriptor pinkIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_station_64px_outdated);

    //For linear mappig of zoom level to oberlay size. Empirically determined.
    @SuppressWarnings("FieldCanBeLocal")
    private static float maxZoomOut = 13.75f;
    @SuppressWarnings("FieldCanBeLocal")
    private static float maxZoomIn = 21;
    @SuppressWarnings("FieldCanBeLocal")
    private static float minOverlaySize = 1;
    private static float maxOverlaySize = 50;

    public StationMapGfx(boolean _outdated, StationItem _item, boolean _lookingForBike, Context _ctx){

        mItem = _item;

        //Marker setup
        markerOptions = new MarkerOptions()
                .position(_item.getLocation())
                .title(_item.getId())
                .alpha(0)
                .zIndex(1.f)//so that invisible clicking marker is in front of Favorite pin
                .anchor(0.5f,0.5f)
                .infoWindowAnchor(0.5f,0.5f);

        // Since googleMap doesn't allow marker resizing we have to use ground overlay to not clog the map when we zoom out...
        groundOverlayOptions = new GroundOverlayOptions()
                .position(_item.getLocation(), maxOverlaySize)
                .transparency(0.1f)
                .visible(false);

        if (!_outdated) {
            if (_item.isLocked())
                groundOverlayOptions.image(greyIcon);
            else {
                if (_lookingForBike) {
                    if (_item.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                        groundOverlayOptions.image(redIcon);
                    else if (_item.getFree_bikes() <= DBHelper.getBadAvailabilityMax(_ctx))
                        groundOverlayOptions.image(yellowIcon);
                    else
                        groundOverlayOptions.image(greenIcon);
                } else {
                    if (_item.getEmpty_slots() != -1 && _item.getEmpty_slots() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                        groundOverlayOptions.image(redIcon);
                    else if (_item.getEmpty_slots() != -1 && _item.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(_ctx))
                        groundOverlayOptions.image(yellowIcon);
                    else
                        groundOverlayOptions.image(greenIcon);
                }
            }
        }
        else {
            groundOverlayOptions.image(pinkIcon);
        }
    }

    public void addMarkerToMap(GoogleMap map){
        marker = map.addMarker(markerOptions);
        groundOverlay = map.addGroundOverlay(groundOverlayOptions);
    }

    public String getMarkerTitle(){ return marker.getTitle(); }

    public LatLng getMarkerLatLng() { return marker.getPosition(); }

    public void updateMarker(boolean _outdated, boolean _isLookingForBikes, Context _ctx) {

        //happens on screen orientation change with slow devices
        if (groundOverlay == null)
            return;

        if (!_outdated) {
            if (_isLookingForBikes) {
                if (!mItem.isLocked()) {
                    if (mItem.getFree_bikes() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                        groundOverlay.setImage(redIcon);
                    else if (mItem.getFree_bikes() <= DBHelper.getBadAvailabilityMax(_ctx))
                        groundOverlay.setImage(yellowIcon);
                    else
                        groundOverlay.setImage(greenIcon);
                } else
                    groundOverlay.setImage(greyIcon);
            } else {
                if (!mItem.isLocked()) {
                    if (mItem.getEmpty_slots() != -1 && mItem.getEmpty_slots() <= DBHelper.getCriticalAvailabilityMax(_ctx))
                        groundOverlay.setImage(redIcon);
                    else if (mItem.getEmpty_slots() != -1 && mItem.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(_ctx))
                        groundOverlay.setImage(yellowIcon);
                    else
                        groundOverlay.setImage(greenIcon);
                } else
                    groundOverlay.setImage(greyIcon);
            }
        }
        else{
            groundOverlay.setImage(pinkIcon);
        }
    }

    public void hide() {
        if (groundOverlay != null)
            groundOverlay.setVisible(false);
    }

    public void show(float _zoom) {

        if (groundOverlay != null) {
            groundOverlay.setDimensions(Utils.map(_zoom, maxZoomOut, maxZoomIn, maxOverlaySize, minOverlaySize));
            groundOverlay.setVisible(true);
        }
    }
}
