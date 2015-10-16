package com.ludoscity.findmybikes.fragments;

import android.app.Activity;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationMapGfx;

import java.util.ArrayList;

public class StationMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationChangeListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapClickListener {

    public static final String INFOWINDOW_CLICK_PATH = "infowindow_click";
    public static final String MARKER_CLICK_PATH = "marker_click";
    public static final String LOCATION_CHANGED_PATH = "location_changed";
    public static final String MAP_READY_PATH = "map_ready";
    public static final String MAP_CLICK_PATH = "map_click";

    public static final String LOCATION_CHANGED_LATITUDE_PARAM = "location_changed_lat";
    public static final String LOCATION_CHANGED_LONGITUDE_PARAM = "location_changed_lng";
    public static final String INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM = "infowindow_click_marker_lat";
    public static final String INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM = "infowindow_click_marker_lng";
    public static final String MARKER_CLICK_TITLE_PARAM = "marker_click_title";


    private boolean mInitialCameraSetupDone;
    private boolean mEnforceMaxZoom = false;
    private GoogleMap mGoogleMap = null;

    private float mMaxZoom = 16f;

    private ArrayList<StationMapGfx> mMapMarkersGfxData = new ArrayList<>();

    private OnStationMapFragmentInteractionListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView = inflater.inflate(R.layout.fragment_station_map, container, false);

        if(mGoogleMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapNearby)).getMapAsync(this);

        return inflatedView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(R.id.mapNearby);
        if (f != null) {
            getActivity().getFragmentManager().beginTransaction().remove(f).commit();
            mGoogleMap = null;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnStationMapFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        //Log.d("CameraZoomLevel", Float.toString(cameraPosition.zoom));
        if (mEnforceMaxZoom && cameraPosition.zoom > mMaxZoom){
            mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(mMaxZoom));
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(INFOWINDOW_CLICK_PATH);

        marker.hideInfoWindow();

        builder.appendQueryParameter(INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM, String.valueOf(marker.getPosition().latitude));
        builder.appendQueryParameter(INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM, String.valueOf(marker.getPosition().longitude));

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mInitialCameraSetupDone = false;
        mGoogleMap = googleMap;
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 13));
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnMyLocationChangeListener(this);
        mGoogleMap.setOnCameraChangeListener(this);
        mGoogleMap.setOnMapClickListener(this);

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(MAP_READY_PATH);

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(MARKER_CLICK_PATH);

        builder.appendQueryParameter(MARKER_CLICK_TITLE_PARAM, marker.getTitle());

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }

        //So that info window will not be showed
        return true;
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            //Log.d("onMyLocationChange", "new location " + location.toString());
            if (!mInitialCameraSetupDone && mGoogleMap != null) {
                doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15), true);
            }

            Uri.Builder builder = new Uri.Builder();
            builder.appendPath(LOCATION_CHANGED_PATH);

            builder.appendQueryParameter(LOCATION_CHANGED_LATITUDE_PARAM, String.valueOf(location.getLatitude()));
            builder.appendQueryParameter(LOCATION_CHANGED_LONGITUDE_PARAM, String.valueOf(location.getLongitude()));

            if (mListener != null){
                mListener.onStationMapFragmentInteraction(builder.build());
            }
        }
    }

    public void doInitialCameraSetup(CameraUpdate cameraUpdate, boolean animate){
        if (animate)
            mGoogleMap.animateCamera(cameraUpdate);
        else
            mGoogleMap.moveCamera(cameraUpdate);

        mInitialCameraSetupDone = true;
    }

    public boolean isMapReady(){return !(mGoogleMap==null);}

    public void addMarkerForStationItem(StationItem item, boolean lookingForBike) {
        mMapMarkersGfxData.add(new StationMapGfx(item, lookingForBike));
    }

    public void redrawMarkers() {

        mGoogleMap.clear();

        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.addMarkerToMap(mGoogleMap);
        }
    }

    public void clearMarkerGfxData() {
        mMapMarkersGfxData.clear();
    }

    public CameraPosition getCameraPosition() {
        CameraPosition toReturn = null;
        if (isMapReady())
            toReturn = mGoogleMap.getCameraPosition();

        return toReturn;
    }

    public void animateCamera(CameraUpdate cameraUpdate) {
        mGoogleMap.animateCamera(cameraUpdate);
    }

    public void lookingForBikes(boolean lookingForBike) {

        for (StationMapGfx markerData : mMapMarkersGfxData){
                markerData.updateMarker(lookingForBike);
            }
    }

    public void resetMarkerSizeAll(){
        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.setBigOverlay(false);
        }
    }

    public void oversizeMarkerUniqueForStationName(String stationName){

        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.setBigOverlay(markerData.getMarkerTitle().equalsIgnoreCase(stationName));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(MAP_CLICK_PATH);

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnStationMapFragmentInteractionListener {
        // TODO: Update argument type and name
        void onStationMapFragmentInteraction(Uri uri);
    }

}
