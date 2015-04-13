package com.udem.ift2906.bixitracksexplorer.Budget;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.udem.ift2906.bixitracksexplorer.BixiTracksExplorerAPIHelper;
import com.udem.ift2906.bixitracksexplorer.DBHelper.DBHelper;
import com.udem.ift2906.bixitracksexplorer.R;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.GetTrackFromTimeUTCKeyStringResponse;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.TrackPoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.udem.ift2906.bixitracksexplorer.Budget.BudgetTrackDetailsFragment.OnBudgetTrackDetailsFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BudgetTrackDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BudgetTrackDetailsFragment extends Fragment
        implements OnMapReadyCallback{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String TRACK_ID = "param1";
    private static final String ARG_ROW_BITMAP_RENDER = "infolistRowBitmapRender";

    //Can't be of type Track because we add data in Couchbase documents that wouldn't
    //map to API model fields
    //Somehow this seems to go against best pratice, but I can't shake the urge of calling
    //DBHelper as few times as possible. It allows retrieving from DB only on fragment creation
    //and AsyncTask
    // I should perform some tests sometimes to see if DB performance
    //is good enough to not impact UI responsiveness.
    private static Map<String,Object> mTrackDataFromDB;

    // TODO: Rename and change types of parameters
    private String mTrackID;
    //private String mParam2;

    private OnBudgetTrackDetailsFragmentInteractionListener mListener;

    private GoogleMap mMap;
    private LatLngBounds mTrackBounds;  //Contains bounding are after call to addTrackPolylineToMap

    private ProgressBar mDataLoadingProgressBar;

    private Bitmap mInfoListRowBitmapRender;
    private ImageView mInfoListRowImageView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param trackID Parameter 1.
     * @param _infoListRowBitmapRender
     * @return A new instance of fragment BudgetTrackDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BudgetTrackDetailsFragment newInstance(String trackID, Bitmap _infoListRowBitmapRender) {
        BudgetTrackDetailsFragment fragment = new BudgetTrackDetailsFragment();
        Bundle args = new Bundle();
        args.putString(BudgetInfoFragment.BUDGETINFOITEM_TRACKID_PARAM, trackID);
        args.putParcelable(ARG_ROW_BITMAP_RENDER, _infoListRowBitmapRender);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    public BudgetTrackDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTrackID = getArguments().getString(BudgetInfoFragment.BUDGETINFOITEM_TRACKID_PARAM);
            mInfoListRowBitmapRender = getArguments().getParcelable(ARG_ROW_BITMAP_RENDER);

            try {
                //Happens on UI thread
                mTrackDataFromDB = DBHelper.retrieveTrack(mTrackID);
            } catch (CouchbaseLiteException e) {

                //e.printStackTrace();
                //Keep going, only mean ID is not valid (that happens in multifragments configuration)
            }

            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_budget_track_details, container, false);
        if (mMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.budgetinfotrackdetails_mapfragment)).getMapAsync(this);

        mInfoListRowImageView = ((ImageView)inflatedView.findViewById(R.id.budgettrackdetails_row_imageview));

        if (mInfoListRowBitmapRender != null)   //Can be null on tablet configuration
        {

            mInfoListRowImageView.setImageBitmap(mInfoListRowBitmapRender);

            //Oddly I have to adjust the ImageView height myself to twice the height of the Bitmap
            //It took ages to find out and is out of reach of my understanding
            //It only affects how the fragment appears on phone (one fragment on screen)
            applyWeirdFixForPhones();
        }

        mDataLoadingProgressBar = (ProgressBar) inflatedView.findViewById(R.id.budgettrackdetails_progressBar);
        // Inflate the layout for this fragment
        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {

        menuInflater.inflate(R.menu.menu_budget_track_details, menu);
    }

    //On enter animation end we want to switch the Bitmap ImageView visibility
    //That doesn't happen in multiscreen configuration (fragments are always displayed)
    //Maybe using an anim as a way to switch the fragment layout ImageView fr the row would
    //fix the disgracious content overlapping with the animated row
    @Override
    public Animation onCreateAnimation (int transit, boolean enter, int nextAnim) {
        //Check if the superclass already created the animation
        Animation anim = super.onCreateAnimation(transit, enter, nextAnim);

        //If not, and an animation is defined, load it now
        if (anim == null && nextAnim != 0) {
            anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        }

        if (anim != null) {

            //Attach listener only on enter animation
            if (enter) {
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mInfoListRowImageView.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mInfoListRowImageView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

            }
        }

        return anim;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getActivity().getFragmentManager()
                .findFragmentById(R.id.budgetinfotrackdetails_mapfragment);
        if (f != null)
            getActivity().getFragmentManager().beginTransaction().remove(f).commit();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onBudgetTrackDetailsFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnBudgetTrackDetailsFragmentInteractionListener) activity;
            super.onAttach(activity);
            //((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
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
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
        }

        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //mMap.setContentDescription("TestContentDescription");
        mMap.setMyLocationEnabled(false);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 10));

        setupUIandTask();
    }

    private void setupUIandTask(){

        if(mTrackDataFromDB != null && mTrackDataFromDB.containsKey("points"))  //Already retrived from database
        {
            mDataLoadingProgressBar.setVisibility(View.GONE);
            //AddPolyLine
            addTrackPolylineToMap();
            //Animate camera
            animateToTrack();
            //Enable gestures
            mMap.getUiSettings().setAllGesturesEnabled(true);

        }
        else
        {
            //Disable map interactions
            mMap.getUiSettings().setAllGesturesEnabled(false);
            //Display progressBar
            mDataLoadingProgressBar.setVisibility(View.VISIBLE);
            //Start retrieve task
            new RetrieveFullTrackFromBackend().execute(mTrackID);
        }
    }

    public void updateWithNewTrack(String _trackID, Bitmap _infoListRowBitmapRender){
        mTrackID = _trackID;
        mInfoListRowBitmapRender = _infoListRowBitmapRender;

        try {
            //Happens on UI thread
            mTrackDataFromDB = DBHelper.retrieveTrack(mTrackID);
        } catch (CouchbaseLiteException e) {

            //e.printStackTrace();
            //Keep going, only mean ID is not valid
        }

        setupUIandTask();

        mInfoListRowImageView.setImageBitmap(mInfoListRowBitmapRender);

        //Oddly I have to adjust the ImageView height myself to twice the height of the Bitmap
        //It took ages to find out and is out of reach of my understanding
        applyWeirdFixForPhones();

    }

    private void applyWeirdFixForPhones(){
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mInfoListRowImageView.getLayoutParams();
        params.width = mInfoListRowBitmapRender.getWidth();
        params.height = mInfoListRowBitmapRender.getHeight()*2;
        mInfoListRowImageView.setLayoutParams(params);
    }

    private void addTrackPolylineToMap(){
        List<LatLng> latLngList = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        //TODO: work out why the ValueType for "points" key is not stable
        //I'm mystified, if anyone has any kind of explanation, I want to understand !
        //Spent a few hours trying to understand it, to none available
        try{
            Iterable<TrackPoint> listTp = (Iterable<TrackPoint>)(mTrackDataFromDB.get("points"));
            for(TrackPoint tp : listTp){
                final LatLng latLng = new LatLng(tp.getLat(), tp.getLon());
                latLngList.add(latLng);
                boundsBuilder.include(latLng);
            }

        }catch (ClassCastException e){
            Iterable<LinkedHashMap> listHm = (Iterable<LinkedHashMap>)(mTrackDataFromDB.get("points"));
            for(LinkedHashMap hm : listHm){
                final LatLng latLng = new LatLng((Double) hm.get("lat"), (Double) hm.get("lon"));
                latLngList.add(latLng);
                boundsBuilder.include(latLng);
            }
        }
        //End weird hack

        mTrackBounds = boundsBuilder.build();

        //TODO: Add paid/free color distinction + map legend
        //TODO: Animate camera to contain Track at appropriate zoom level
        mMap.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .width(5)
                .color(Color.BLUE));
    }

    private void animateToTrack(){
        //10 padding pixels around
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mTrackBounds, 10));
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
    public interface OnBudgetTrackDetailsFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onBudgetTrackDetailsFragmentInteraction(Uri uri);
    }

    //Class for ASync Loading of points data from backend
    public class RetrieveFullTrackFromBackend extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... params) {
            final GetTrackFromTimeUTCKeyStringResponse fullTrackResponse = BixiTracksExplorerAPIHelper.retrieveFullTrack(params[0]);

            if (fullTrackResponse.getTrack() != null){
                try {
                    DBHelper.putNewTrackPropertyAndSave(mTrackID, "points", fullTrackResponse.getTrack().getPoints());
                    //Happens in ASyncTask
                    mTrackDataFromDB = DBHelper.retrieveTrack(mTrackID);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid){
            super.onPostExecute(aVoid);

            if(mTrackDataFromDB != null)
            {
                //addPolyline
                BudgetTrackDetailsFragment.this.addTrackPolylineToMap();
                //remove progressBar
                mDataLoadingProgressBar.setVisibility(View.GONE);
                //Animates camera
                animateToTrack();
                //enables map interactions
                mMap.getUiSettings().setAllGesturesEnabled(true);
            }

        }

    }
}
