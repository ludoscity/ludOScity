package com.ludoscity.findmybikes.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationMapGfx;
import com.ludoscity.findmybikes.utils.Utils;

import java.util.ArrayList;

public class StationMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraChangeListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapClickListener {

    //Used to buffer markers update requests (avoids glitchy anim)
    private class CustomCancellableCallback implements GoogleMap.CancelableCallback {

        Boolean mLookingForBikeWhenFinished = null;
        @Override
        public void onFinish() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mLookingForBikeWhenFinished);

            mAnimCallback = null;

        }

        @Override
        public void onCancel() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mLookingForBikeWhenFinished);

            mAnimCallback = null;

        }
    }

    public static final String INFOWINDOW_CLICK_PATH = "infowindow_click";
    public static final String MARKER_CLICK_PATH = "marker_click";
    public static final String MAP_READY_PATH = "map_ready";
    public static final String MAP_CLICK_PATH = "map_click";

    public static final String INFOWINDOW_CLICK_MARKER_POS_LAT_PARAM = "infowindow_click_marker_lat";
    public static final String INFOWINDOW_CLICK_MARKER_POS_LNG_PARAM = "infowindow_click_marker_lng";
    public static final String MARKER_CLICK_TITLE_PARAM = "marker_click_title";


    private boolean mInitialCameraSetupDone;
    private boolean mEnforceMaxZoom = false;
    private GoogleMap mGoogleMap = null;
    private CustomCancellableCallback mAnimCallback = null;

    private float mMaxZoom = 16f;

    private ArrayList<StationMapGfx> mMapMarkersGfxData = new ArrayList<>();

    private OnStationMapFragmentInteractionListener mListener;

    private Marker mMarkerStationA;
    private Marker mMarkerStationB;
    private BitmapDescriptor mIconABitmapDescriptor;
    private BitmapDescriptor mIconBBitmapDescriptor;

    private final LatLng MONTREAL_LATLNG = new LatLng(45.5087, -73.554);

    //Pin markers can only be restored after mGoogleMap is ready
    private Bundle mBufferedBundle = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflatedView = inflater.inflate(R.layout.fragment_station_map, container, false);

        if (mGoogleMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.mapNearby)).getMapAsync(this);

        mBufferedBundle = savedInstanceState;

        mIconABitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_a_map);
        mIconBBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_b_map);

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
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("pin_A_visibility", mMarkerStationA != null && mMarkerStationA.isVisible());
        outState.putBoolean("pin_B_visibility", mMarkerStationB != null && mMarkerStationB.isVisible());
        outState.putParcelable("pin_A_latlng", mMarkerStationA != null ? mMarkerStationA.getPosition() : MONTREAL_LATLNG);
        outState.putParcelable("pin_B_latlng", mMarkerStationB != null ? mMarkerStationB.getPosition() : MONTREAL_LATLNG);
        super.onSaveInstanceState(outState);
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
        if (mEnforceMaxZoom && cameraPosition.zoom > mMaxZoom) {
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

        if (mListener != null) {
            mListener.onStationMapFragmentInteraction(builder.build());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mInitialCameraSetupDone = false;
        mGoogleMap = googleMap;
        enableMyLocationCheckingPermission();
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 13));
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnInfoWindowClickListener(this);
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

        if ( (mMarkerStationA.isVisible() &&
                mMarkerStationA.getPosition().latitude == marker.getPosition().latitude &&
                mMarkerStationA.getPosition().longitude == marker.getPosition().longitude) ||
                (mMarkerStationB.isVisible() &&
                        mMarkerStationB.getPosition().latitude == marker.getPosition().latitude &&
                        mMarkerStationB.getPosition().longitude == marker.getPosition().longitude) )
            return true;

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(MARKER_CLICK_PATH);

        builder.appendQueryParameter(MARKER_CLICK_TITLE_PARAM, marker.getTitle());

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }

        //So that info window will not be showed
        return true;
    }

    public void onUserLocationChange(Location location) {
        if (location != null) {
            //Log.d("onMyLocationChange", "new location " + location.toString());
            if (!mInitialCameraSetupDone && mGoogleMap != null) {
                doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15), true);
            }
        }
    }

    public void enableMyLocationCheckingPermission(){
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return;

        }
        else{
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    public void doInitialCameraSetup(CameraUpdate cameraUpdate, boolean animate){
        if (animate)
            mGoogleMap.animateCamera(cameraUpdate);
        else
            mGoogleMap.moveCamera(cameraUpdate);

        mInitialCameraSetupDone = true;
    }

    public LatLng getMarkerALatLng(){
        LatLng toReturn = null;

        if (mMarkerStationA != null)
            toReturn = mMarkerStationA.getPosition();
        else if (mBufferedBundle != null)
            toReturn = mBufferedBundle.getParcelable("pin_A_latlng");

        return toReturn;
    }

    public LatLng getMarkerBVisibleLatLng() {
        LatLng toReturn = null;
        if ( mMarkerStationB != null  ) {
            if (mMarkerStationB.isVisible())
                toReturn = mMarkerStationB.getPosition();
        }
        else if (mBufferedBundle != null && mBufferedBundle.getBoolean("pin_B_visibility") )
                toReturn = mBufferedBundle.getParcelable("pin_B_latlng");

        return toReturn;
    }

    public boolean isRestoring() { return mBufferedBundle != null; }

    public void clearMarkerB() {
        if (mMarkerStationB != null)
            mMarkerStationB.setVisible(false);
    }

    public boolean isMapReady(){ return mGoogleMap != null; }

    public void addMarkerForStationItem(StationItem item, boolean lookingForBike) {
        mMapMarkersGfxData.add(new StationMapGfx(item, lookingForBike));
    }

    public void redrawMarkers() {

        boolean pinAVisible;
        boolean pinBVisible;
        LatLng pinALatLng;
        LatLng pinBLatLng;

        if (mBufferedBundle != null){

            pinAVisible = mBufferedBundle.getBoolean("pin_A_visibility");
            pinBVisible = mBufferedBundle.getBoolean("pin_B_visibility");

            pinALatLng = mBufferedBundle.getParcelable("pin_A_latlng");
            pinBLatLng = mBufferedBundle.getParcelable("pin_B_latlng");

            mBufferedBundle = null;
        } else {

            pinAVisible = mMarkerStationA != null && mMarkerStationA.isVisible();
            pinBVisible = mMarkerStationB != null && mMarkerStationB.isVisible();
            pinALatLng = mMarkerStationA != null ? mMarkerStationA.getPosition() : MONTREAL_LATLNG;
            pinBLatLng = mMarkerStationB != null ? mMarkerStationB.getPosition() : MONTREAL_LATLNG;
        }

        mGoogleMap.clear();

        //There is a bug in the map library with vector drawable, I use a workaround I found in the bug report
        //https://code.google.com/p/gmaps-api-issues/issues/detail?id=9011
        BitmapDescriptor iconA = mIconABitmapDescriptor;//BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_a_map);
        BitmapDescriptor iconB = mIconBBitmapDescriptor;//BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_b_map);

        mMarkerStationA = mGoogleMap.addMarker(new MarkerOptions().position(pinALatLng)
                .icon(iconA)
                .visible(pinAVisible));
        mMarkerStationB = mGoogleMap.addMarker(new MarkerOptions().position(pinBLatLng)
                .icon(iconB)
                .visible(pinBVisible));

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
        mAnimCallback = new CustomCancellableCallback();

        mGoogleMap.animateCamera(cameraUpdate, 850, mAnimCallback);
    }

    public void lookingForBikes(boolean lookingForBike) {

        if (mAnimCallback != null){
            mAnimCallback.mLookingForBikeWhenFinished = lookingForBike;
        }
        else
            updateMarkerAll(lookingForBike);
    }

    private void updateMarkerAll(boolean lookingForBike){

        for (StationMapGfx markerData : mMapMarkersGfxData){
            markerData.updateMarker(lookingForBike);
        }
    }

    //TODO: if clients have a stationname, maybe they have the station LatLng on hand
    public void setPinOnStation(boolean _lookingForBike, String _stationName){

        for (StationMapGfx markerData : mMapMarkersGfxData){
            if (markerData.getMarkerTitle().equalsIgnoreCase(_stationName)) {
                if (_lookingForBike){
                    mMarkerStationA.setPosition(markerData.getMarkerLatLng());
                    mMarkerStationA.setVisible(true);
                } else {
                    mMarkerStationB.setPosition(markerData.getMarkerLatLng());
                    mMarkerStationB.setVisible(true);
                }

                break;
            }
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
