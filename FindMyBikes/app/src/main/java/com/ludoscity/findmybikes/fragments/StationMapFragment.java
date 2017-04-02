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
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ludoscity.findmybikes.R;
import com.ludoscity.findmybikes.StationItem;
import com.ludoscity.findmybikes.StationMapGfx;
import com.ludoscity.findmybikes.utils.Utils;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

public class StationMapFragment extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        /*GoogleMap.OnCameraChangeListener,*/
        GoogleMap.OnInfoWindowClickListener/*,
        GoogleMap.OnMapClickListener*/ {

    //Used to buffer markers update requests (avoids glitchy anim)
    private class CustomCancellableCallback implements GoogleMap.CancelableCallback {

        Boolean mLookingForBikeWhenFinished = null;
        Boolean mOutdatedWhenFinished = null;
        @Override
        public void onFinish() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mOutdatedWhenFinished, mLookingForBikeWhenFinished);

            showAllStations();

            mAnimCallback = null;

        }

        @Override
        public void onCancel() {

            if (mLookingForBikeWhenFinished != null)
                updateMarkerAll(mOutdatedWhenFinished, mLookingForBikeWhenFinished);


            showAllStations();

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

    private static int CURRENT_MAP_PADDING_LEFT = 0;
    private static int CURRENT_MAP_PADDING_RIGHT = 0;

    private boolean mInitialCameraSetupDone;

    private GoogleMap mGoogleMap = null;
    private CustomCancellableCallback mAnimCallback = null;

    private ArrayList<StationMapGfx> mMapMarkersGfxData = new ArrayList<>();

    private OnStationMapFragmentInteractionListener mListener;

    private Marker mMarkerStationA;
    private BitmapDescriptor mPinAIconBitmapDescriptor;
    private Marker mMarkerStationB;
    private BitmapDescriptor mPinBIconBitmapDescriptor;
    private Marker mMarkerPickedPlace;
    private BitmapDescriptor mPinSearchIconBitmapDescriptor;
    private Marker mMarkerPickedFavorite;
    private BitmapDescriptor mPinFavoriteIconBitmapDescriptor;
    private BitmapDescriptor mNoPinFavoriteIconBitmapDescriptor;

    private TextView mAttributionsText;

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

        mPinAIconBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_a_36dp_black);
        mPinBIconBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_b_36dp_black);
        mPinSearchIconBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_search_24dp_black);
        mPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_pin_favorite_24dp_black);
        mNoPinFavoriteIconBitmapDescriptor = Utils.getBitmapDescriptor(getContext(), R.drawable.ic_nopin_favorite_24dp_white);

        mAttributionsText = (TextView) inflatedView.findViewById(R.id.attributions_text);

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
        outState.putString("pin_a_station_id", mMarkerStationA != null ? mMarkerStationA.getTitle() : "");

        outState.putBoolean("pin_picked_place_visibility", mMarkerPickedPlace != null && mMarkerPickedPlace.isVisible());
        outState.putParcelable("pin_picked_place_latlng", mMarkerPickedPlace != null ? mMarkerPickedPlace.getPosition() : MONTREAL_LATLNG);
        outState.putString("picked_place_name", mMarkerPickedPlace != null ? mMarkerPickedPlace.getTitle() : "");

        outState.putBoolean("pin_picked_favorite_visibility", mMarkerPickedFavorite != null && mMarkerPickedFavorite.isVisible());
        outState.putParcelable("pin_picked_favorite_latlng", mMarkerPickedFavorite != null ? mMarkerPickedFavorite.getPosition() : MONTREAL_LATLNG);
        outState.putString("picked_favorite_name", mMarkerPickedFavorite != null ? mMarkerPickedFavorite.getTitle() : "");
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

    /*@Override
    public void onCameraChange(CameraPosition cameraPosition) {
        //Log.d("CameraZoomLevel", Float.toString(cameraPosition.zoom));

    }*/

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
        //mGoogleMap.setOnCameraChangeListener(this);
        //mGoogleMap.setOnMapClickListener(this);
        //héhéhé, feel the power of design !!
        mGoogleMap.getUiSettings().setZoomGesturesEnabled(false);
        mGoogleMap.getUiSettings().setRotateGesturesEnabled(false);
        mGoogleMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mGoogleMap.getUiSettings().setTiltGesturesEnabled(false);

        Uri.Builder builder = new Uri.Builder();
        builder.appendPath(MAP_READY_PATH);

        if (mListener != null){
            mListener.onStationMapFragmentInteraction(builder.build());
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (isPickedFavoriteMarkerVisible())
            mMarkerPickedFavorite.showInfoWindow();

        if (isPickedPlaceMarkerVisible())
            mMarkerPickedPlace.showInfoWindow();

        if (marker.getTitle().equalsIgnoreCase(mMarkerPickedFavorite.getTitle()) ||
            (mMarkerStationA.isVisible() &&
                mMarkerStationA.getPosition().latitude == marker.getPosition().latitude &&
                mMarkerStationA.getPosition().longitude == marker.getPosition().longitude) ||
            (mMarkerStationB.isVisible() &&
                mMarkerStationB.getPosition().latitude == marker.getPosition().latitude &&
                mMarkerStationB.getPosition().longitude == marker.getPosition().longitude) ||
            (mMarkerPickedPlace.isVisible() && //except if picked destination is favorite
                mMarkerPickedPlace.getPosition().latitude == marker.getPosition().latitude &&
                mMarkerPickedPlace.getPosition().longitude == marker.getPosition().longitude) )
            return true;

        if(mMarkerPickedFavorite.isVisible() &&
                //TODO: the following seems a bit patterny, checking if B pin and Favorite one are not on the same station
                //check connection with NearbyActivity::setupBTabSelection refactor ideas
                mMarkerPickedFavorite.getPosition().latitude == marker.getPosition().latitude &&
                mMarkerPickedFavorite.getPosition().longitude == marker.getPosition().longitude)
        {
            mMarkerPickedFavorite.hideInfoWindow();
        }

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
                doInitialCameraSetup(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15), false);
            }
        }
    }

    public void enableMyLocationCheckingPermission(){
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    public void doInitialCameraSetup(CameraUpdate cameraUpdate, boolean animate){
        if (animate)
            animateCamera(cameraUpdate);
        else
            mGoogleMap.moveCamera(cameraUpdate);

        mInitialCameraSetupDone = true;
    }

    public String getMarkerAStationId(){

        String toReturn = "";

        if (mMarkerStationA != null)
            toReturn = mMarkerStationA.getTitle();
        else if(mBufferedBundle != null)
            toReturn = mBufferedBundle.getString("pin_a_station_id");

        return toReturn;
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

    public void setMapPaddingLeft(int _paddingPx){
        CURRENT_MAP_PADDING_LEFT = _paddingPx;
        mGoogleMap.setPadding(CURRENT_MAP_PADDING_LEFT, 0, CURRENT_MAP_PADDING_RIGHT, 0);
    }

    public void setMapPaddingRight(int _paddingPx){
        CURRENT_MAP_PADDING_RIGHT = _paddingPx;
        mGoogleMap.setPadding(CURRENT_MAP_PADDING_LEFT, 0, CURRENT_MAP_PADDING_RIGHT, 0);
    }

    public void setScrollGesturesEnabled(boolean _toSet){
        mGoogleMap.getUiSettings().setScrollGesturesEnabled(_toSet);
    }

    //TODO: refactor so that the map fragment is a simple dumb view
    //Model and controller could be NearbyActivity
    //in any case, try to remove checks for visibility as a basis fro branching in NearbyActivity
    public boolean isPickedPlaceMarkerVisible(){
        return mMarkerPickedPlace != null && mMarkerPickedPlace.isVisible();
    }

    public void pickedFavoriteMarkerInfoWindowShow(){
        mMarkerPickedFavorite.showInfoWindow();
    }

    public void pickedFavoriteMarkerInfoWindowHide(){
        mMarkerPickedFavorite.hideInfoWindow();
    }

    //TODO: refactor so that the map fragment is a simple dumb view
    //Model and controller could be NearbyActivity
    //in any case, try to remove checks for visibility as a basis fro branching in NearbyActivity
    //investigate if that should extend to grabbing LatLng and Name
    public boolean isPickedFavoriteMarkerVisible(){
        return mMarkerPickedFavorite != null && mMarkerPickedFavorite.isVisible();
    }

    //LatLng
    public LatLng getMarkerPickedPlaceVisibleLatLng() {
        LatLng toReturn = null;
        if ( mMarkerPickedPlace != null  ) {
            if (mMarkerPickedPlace.isVisible())
                toReturn = mMarkerPickedPlace.getPosition();
        }
        else if (mBufferedBundle != null && mBufferedBundle.getBoolean("pin_picked_place_visibility") )
            toReturn = mBufferedBundle.getParcelable("pin_picked_place_latlng");

        return toReturn;
    }
    //Name
    public String getMarkerPickedPlaceVisibleName() {
        String toReturn = "";
        if ( mMarkerPickedPlace != null  ) {
            if (mMarkerPickedPlace.isVisible())
                toReturn = mMarkerPickedPlace.getTitle();
        }
        else if (mBufferedBundle != null && mBufferedBundle.getBoolean("pin_picked_place_visibility") )
            toReturn = mBufferedBundle.getParcelable("pin_picked_place_name");

        return toReturn;
    }

    //LatLng
    public LatLng getMarkerPickedFavoriteVisibleLatLng() {
        LatLng toReturn = null;
        if ( mMarkerPickedFavorite != null  ) {
            if (mMarkerPickedFavorite.isVisible())
                toReturn = mMarkerPickedFavorite.getPosition();
        }
        else if (mBufferedBundle != null && mBufferedBundle.getBoolean("pin_picked_favorite_visibility") )
            toReturn = mBufferedBundle.getParcelable("pin_picked_favorite_latlng");

        return toReturn;
    }

    public LatLngBounds getCameraLatLngBounds() {
        return mGoogleMap.getProjection().getVisibleRegion().latLngBounds;
    }

    public boolean isRestoring() { return mBufferedBundle != null; }

    public void clearMarkerB() {
        if (mMarkerStationB != null)
            mMarkerStationB.setVisible(false);
    }

    public boolean isMapReady(){ return mGoogleMap != null; }

    public void addMarkerForStationItem(boolean _outdated, StationItem item, boolean lookingForBike) {

        if (getContext() == null)
            return;

        mMapMarkersGfxData.add(new StationMapGfx(_outdated, item, lookingForBike, getContext()));
    }

    public void redrawMarkers() {

        boolean pinAVisible;
        String pinAStationId;
        boolean pinBVisible;
        boolean pinPickedPlaceVisible;
        boolean pinPickedFavoriteVisible;
        LatLng pinALatLng;
        LatLng pinBLatLng;
        LatLng pinPickedPlaceLatLng;
        LatLng pinPickedFavoriteLatLng;

        String pickedPlaceName;
        String pickedFavoriteName;

        if (mBufferedBundle != null){

            pinAVisible = mBufferedBundle.getBoolean("pin_A_visibility");
            pinBVisible = mBufferedBundle.getBoolean("pin_B_visibility");
            pinPickedPlaceVisible = mBufferedBundle.getBoolean("pin_picked_place_visibility");
            pinPickedFavoriteVisible = mBufferedBundle.getBoolean("pin_picked_favorite_visibility");

            pinALatLng = mBufferedBundle.getParcelable("pin_A_latlng");
            pinBLatLng = mBufferedBundle.getParcelable("pin_B_latlng");
            pinPickedPlaceLatLng = mBufferedBundle.getParcelable("pin_picked_place_latlng");
            pinPickedFavoriteLatLng = mBufferedBundle.getParcelable("pin_picked_favorite_latlng");

            pickedPlaceName = mBufferedBundle.getString("picked_place_name");
            pickedFavoriteName = mBufferedBundle.getString("picked_favorite_name");

            pinAStationId = mBufferedBundle.getString("pin_a_station_id");

            mBufferedBundle = null;
        } else {

            pinAVisible = mMarkerStationA != null && mMarkerStationA.isVisible();
            pinBVisible = mMarkerStationB != null && mMarkerStationB.isVisible();
            pinPickedPlaceVisible = mMarkerPickedPlace != null && mMarkerPickedPlace.isVisible();
            pinPickedFavoriteVisible = mMarkerPickedFavorite != null && mMarkerPickedFavorite.isVisible();

            pinALatLng = mMarkerStationA != null ? mMarkerStationA.getPosition() : MONTREAL_LATLNG;
            pinBLatLng = mMarkerStationB != null ? mMarkerStationB.getPosition() : MONTREAL_LATLNG;
            pinPickedPlaceLatLng = mMarkerPickedPlace != null ? mMarkerPickedPlace.getPosition() : MONTREAL_LATLNG;
            pinPickedFavoriteLatLng = mMarkerPickedFavorite != null ? mMarkerPickedFavorite.getPosition() : MONTREAL_LATLNG;

            pickedPlaceName = mMarkerPickedPlace != null ? mMarkerPickedPlace.getTitle() : "";
            pickedFavoriteName = mMarkerPickedFavorite != null ? mMarkerPickedFavorite.getTitle() : "";

            pinAStationId = mMarkerStationA != null ? mMarkerStationA.getTitle() : "";
        }

        mGoogleMap.clear();

        mMarkerStationA = mGoogleMap.addMarker(new MarkerOptions().position(pinALatLng)
                .icon(mPinAIconBitmapDescriptor)
                .visible(pinAVisible)
                .title(pinAStationId));
        mMarkerStationB = mGoogleMap.addMarker(new MarkerOptions().position(pinBLatLng)
                .icon(mPinBIconBitmapDescriptor)
                .visible(pinBVisible));
        mMarkerPickedPlace = mGoogleMap.addMarker(new MarkerOptions().position(pinPickedPlaceLatLng)
                .icon(mPinSearchIconBitmapDescriptor)
                .visible(pinPickedPlaceVisible)
                .title(pickedPlaceName));
        mMarkerPickedFavorite = mGoogleMap.addMarker(new MarkerOptions().position(pinPickedFavoriteLatLng)
                .icon(mPinFavoriteIconBitmapDescriptor)
                .visible(pinPickedFavoriteVisible)
                .zIndex(.5f)//so that it's on top of B pin (default Z is 0)
                .title(pickedFavoriteName));

        if (pinPickedPlaceVisible)
            mMarkerPickedPlace.showInfoWindow();

        if ( pinPickedFavoriteVisible &&
                (mMarkerPickedFavorite.getPosition().latitude != pinBLatLng.latitude ||
                mMarkerPickedFavorite.getPosition().longitude != pinBLatLng.longitude) )
            mMarkerPickedFavorite.showInfoWindow();

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

        if (getContext() != null) {

            mAnimCallback = new CustomCancellableCallback();

            hideAllStations();

            mGoogleMap.animateCamera(cameraUpdate, getResources().getInteger(R.integer.camera_animation_duration), mAnimCallback);
        }
        else{
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    public void hideAllStations() {

        try {
            for (StationMapGfx markerData : mMapMarkersGfxData) {
                markerData.hide();
            }
        }
        catch (ConcurrentModificationException e){
            //Can happen on screen orientation change. Simply retry
            hideAllStations();
        }
    }

    public void showAllStations() {

        try {
            for (StationMapGfx markerData : mMapMarkersGfxData){
                markerData.show(mGoogleMap.getCameraPosition().zoom);
            }
        } catch (ConcurrentModificationException e){
            //Can happen on screen orientation change. Simply retry
            showAllStations();
        }
    }

    public void lookingForBikes(boolean _outdated, boolean _lookingForBike) {

        if (mAnimCallback != null){
            mAnimCallback.mLookingForBikeWhenFinished = _lookingForBike;
            mAnimCallback.mOutdatedWhenFinished = _outdated;
        }
        else
            updateMarkerAll(_outdated, _lookingForBike);
    }

    private void updateMarkerAll(boolean _outdated, boolean _lookingForBike){

        if (getContext() == null)
            return;

        try {
            for (StationMapGfx markerData : mMapMarkersGfxData) {
                markerData.updateMarker(_outdated, _lookingForBike, getContext());
            }
        }
        catch (ConcurrentModificationException e){
            updateMarkerAll(_outdated, _lookingForBike);
        }
    }

    //TODO: if clients have a stationname, maybe they have the station LatLng on hand
    public void setPinOnStation(boolean _lookingForBike, String _stationId){

        try {
            for (StationMapGfx markerData : mMapMarkersGfxData) {
                if (markerData.getMarkerTitle().equalsIgnoreCase(_stationId)) {
                    if (_lookingForBike) {
                        mMarkerStationA.setPosition(markerData.getMarkerLatLng());
                        mMarkerStationA.setVisible(true);
                        mMarkerStationA.setTitle(markerData.getMarkerTitle());
                    } else {
                        mMarkerStationB.setPosition(markerData.getMarkerLatLng());
                        mMarkerStationB.setVisible(true);

                        if (isPickedFavoriteMarkerVisible()){
                            if (getMarkerPickedFavoriteVisibleLatLng().latitude == getMarkerBVisibleLatLng().latitude &&
                                    getMarkerPickedFavoriteVisibleLatLng().longitude == getMarkerBVisibleLatLng().longitude){
                                mMarkerPickedFavorite.setIcon(mNoPinFavoriteIconBitmapDescriptor);
                                mMarkerPickedFavorite.hideInfoWindow();
                            }
                            else {
                                mMarkerPickedFavorite.setIcon(mPinFavoriteIconBitmapDescriptor);
                                mMarkerPickedFavorite.showInfoWindow();
                            }
                        }
                    }

                    break;
                }
            }
        }
        catch (ConcurrentModificationException e){
            //Can happen on screen orientation change. Simply retry
            setPinOnStation(_lookingForBike, _stationId);
        }
    }

    public void setPinForPickedPlace(String _placeName, LatLng _placePosition, CharSequence _attributions){

        mMarkerPickedPlace.setTitle(_placeName);
        mMarkerPickedPlace.setPosition(_placePosition);
        mMarkerPickedPlace.setVisible(true);
        mMarkerPickedPlace.showInfoWindow();

        if (_attributions != null) {
            mAttributionsText.setText(Utils.fromHtml(_attributions.toString()));
            mAttributionsText.setVisibility(View.VISIBLE);
        }
    }

    public void setPinForPickedFavorite(String _favoriteName, LatLng _favoritePosition, CharSequence _attributions){

        mMarkerPickedFavorite.setTitle(_favoriteName);
        mMarkerPickedFavorite.setPosition(_favoritePosition);
        mMarkerPickedFavorite.setVisible(true);

        if (_favoritePosition.latitude == getMarkerBVisibleLatLng().latitude &&
                _favoritePosition.longitude == getMarkerBVisibleLatLng().longitude){
            mMarkerPickedFavorite.setIcon(mNoPinFavoriteIconBitmapDescriptor);
            mMarkerPickedFavorite.hideInfoWindow();
        }
        else {
            mMarkerPickedFavorite.setIcon(mPinFavoriteIconBitmapDescriptor);
            mMarkerPickedFavorite.showInfoWindow();
        }

        if (_attributions != null) {
            mAttributionsText.setText(Utils.fromHtml(_attributions.toString()));
            mAttributionsText.setVisibility(View.VISIBLE);
        }
    }

    public  void clearMarkerPickedPlace(){
        if(mMarkerPickedPlace != null)
            mMarkerPickedPlace.setVisible(false);

        mAttributionsText.setVisibility(View.GONE);
        mAttributionsText.setText("");
    }

    public  void clearMarkerPickedFavorite(){
        if(mMarkerPickedFavorite != null)
            mMarkerPickedFavorite.setVisible(false);
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
        void onStationMapFragmentInteraction(Uri uri);
    }

}
