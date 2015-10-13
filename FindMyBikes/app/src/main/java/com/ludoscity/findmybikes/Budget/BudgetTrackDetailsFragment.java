package com.ludoscity.findmybikes.Budget;

import android.app.Activity;
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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ludoscity.findmybikes.BixiTracksExplorerAPIHelper;
import com.ludoscity.findmybikes.Helpers.DBHelper;
import com.ludoscity.findmybikes.R;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.GetTrackFromTimeUTCKeyStringResponse;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.TrackPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BudgetTrackDetailsFragment.OnBudgetTrackDetailsFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BudgetTrackDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BudgetTrackDetailsFragment extends Fragment
        implements OnMapReadyCallback{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String TRACK_ID = "param1";
    private static final String ARG_BUDGETINFOITEM_LIST = "budgetInfoItemList";

    //Can't be of type Track because we add data in Couchbase documents that wouldn't
    //map to API model fields
    //Somehow this seems to go against best pratice, but I can't shake the urge of calling
    //DBHelper as few times as possible. It allows retrieving from DB only on fragment creation
    //and AsyncTask
    // I should perform some tests sometimes to see if DB performance
    //is good enough to not impact UI responsiveness.
    private static Map<String,Object> mTrackDataFromDB;

    private int mBudgetInfoItemPos;
    private List<BudgetInfoItem> mBudgetInfoItemList;
    private int mCurrentSortCriteria;

    private OnBudgetTrackDetailsFragmentInteractionListener mListener;

    private GoogleMap mMap;
    private LatLngBounds mTrackBounds;  //Contains bounding are after call to addTrackPolylineToMap

    private ProgressBar mDataLoadingProgressBar;

    private View mInfoListRowView;

    private MenuItem mNextMenuItem = null;
    private MenuItem mPreviousMenuItem = null;
    private MenuItem mCriteriaMenuItem = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param _budgetInfoItemPos where did the click happened.
     * @param _budgetInfoItemList the actual list of BudgetInfoItem
     * @return A new instance of fragment BudgetTrackDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BudgetTrackDetailsFragment newInstance(int _budgetInfoItemPos, ArrayList<BudgetInfoItem> _budgetInfoItemList, int _sortCriteria, boolean _multiFragment) {
        BudgetTrackDetailsFragment fragment = new BudgetTrackDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(BudgetInfoFragment.CLICK_ITEMPOS_PARAM, _budgetInfoItemPos);
        args.putInt(BudgetInfoFragment.CLICK_SORT_CRITERIA_PARAM, _sortCriteria);
        args.putParcelableArrayList(ARG_BUDGETINFOITEM_LIST, _budgetInfoItemList);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(!_multiFragment);
        return fragment;
    }

    public BudgetTrackDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBudgetInfoItemPos = getArguments().getInt(BudgetInfoFragment.CLICK_ITEMPOS_PARAM);
            mBudgetInfoItemList = getArguments().getParcelableArrayList(ARG_BUDGETINFOITEM_LIST);
            mCurrentSortCriteria = getArguments().getInt(BudgetInfoFragment.CLICK_SORT_CRITERIA_PARAM);

            if (mBudgetInfoItemPos != -1) {

                try {
                    //Happens on UI thread
                    mTrackDataFromDB = DBHelper.retrieveTrack(mBudgetInfoItemList.get(mBudgetInfoItemPos).getIDAsString());
                } catch (CouchbaseLiteException e) {

                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_budget_track_details, container, false);
        if (mMap == null)
            ((MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.budgetinfotrackdetails_mapfragment)).getMapAsync(this);

        mInfoListRowView = inflatedView.findViewById(R.id.budgettrackdetails_row_view);

        //Bugfix : crash when clicking on Row View in details fragment
        mInfoListRowView.setOnClickListener(null);

        FrameLayout endFragment = (FrameLayout)getActivity().findViewById(R.id.end_fragment_container);
        if (endFragment != null){
            mInfoListRowView.setVisibility(View.GONE);
        }
        else{
            mInfoListRowView.setActivated(true);
        }

        mDataLoadingProgressBar = (ProgressBar) inflatedView.findViewById(R.id.budgettrackdetails_progressBar);
        // Inflate the layout for this fragment
        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {

        menuInflater.inflate(R.menu.menu_budget_track_details, menu);

        mPreviousMenuItem = menu.findItem(R.id.budgetTrackDetailsPrevious);
        mNextMenuItem = menu.findItem(R.id.budgetTrackDetailsNext);
        mCriteriaMenuItem = menu.findItem(R.id.budgetTrackDetailsCriteria);

        setupActionItems();
    }

    private void setupActionItems(){

        if (mPreviousMenuItem == null)  //We are in multifragments config, no actions
            return;

        mPreviousMenuItem.setVisible(true);
        mNextMenuItem.setVisible(true);

        if(mBudgetInfoItemPos == -1) {
            mPreviousMenuItem.setVisible(false);
            mNextMenuItem.setVisible(false);
        }
        else if (mBudgetInfoItemPos == 0) {
            mPreviousMenuItem.setVisible(false);
        }
        else if (mBudgetInfoItemPos == mBudgetInfoItemList.size()-1){
            mNextMenuItem.setVisible(false);
        }

        switch (mCurrentSortCriteria){
            case BudgetInfoListViewAdapter.SORT_CRITERIA_COST:
                mCriteriaMenuItem.setIcon(R.drawable.ic_action_cost);
                break;
            case BudgetInfoListViewAdapter.SORT_CRITERIA_DURATION:
                mCriteriaMenuItem.setIcon(R.drawable.ic_action_duration);
                break;
            case BudgetInfoListViewAdapter.SORT_CRITERIA_DATE:
                mCriteriaMenuItem.setIcon(R.drawable.ic_action_date);
                break;
        }
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
                        mInfoListRowView.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        getActivity().findViewById(R.id.animatedrow_bitmap_holder_imageview).setVisibility(View.INVISIBLE);
                        mInfoListRowView.setVisibility(View.VISIBLE);
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
            case R.id.budgetTrackDetailsNext:
                ++mBudgetInfoItemPos;
                trackChanged();
                return true;
            case R.id.budgetTrackDetailsPrevious:
                --mBudgetInfoItemPos;
                trackChanged();
                return true;

            case R.id.budgetTrackDetailsCriteria:
                if (mCurrentSortCriteria == BudgetInfoListViewAdapter.SORT_CRITERIA_COST){
                    item.setIcon(R.drawable.ic_action_duration);
                    mCurrentSortCriteria = BudgetInfoListViewAdapter.SORT_CRITERIA_DURATION;
                }
                else if (mCurrentSortCriteria == BudgetInfoListViewAdapter.SORT_CRITERIA_DURATION){
                    item.setIcon(R.drawable.ic_action_date);
                    mCurrentSortCriteria = BudgetInfoListViewAdapter.SORT_CRITERIA_DATE;
                }
                else if (mCurrentSortCriteria == BudgetInfoListViewAdapter.SORT_CRITERIA_DATE){
                    item.setIcon(R.drawable.ic_action_cost);
                    mCurrentSortCriteria = BudgetInfoListViewAdapter.SORT_CRITERIA_COST;
                }

                setupInfoItemView();

                return true;
        }

        return false;
    }

    private void trackChanged(){

        mMap.clear();

        try {
            //Happens on UI thread
            mTrackDataFromDB = DBHelper.retrieveTrack(mBudgetInfoItemList.get(mBudgetInfoItemPos).getIDAsString());
        } catch (CouchbaseLiteException e) {

            e.printStackTrace();
        }

        setupUIandTask();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //mMap.setContentDescription("TestContentDescription");
        mMap.setMyLocationEnabled(false);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.5086699, -73.5539925), 10));

        setupUIandTask();
    }

    private class ViewHolder{
        TextView sortCriteriaLine1;
        TextView sortCriteriaLine2;
        TextView startStationName;
        TextView endStationName;
        TextView additionalInfoLine1;
        TextView additionalInfoLine2;
    }

    private void setupUIandTask(){

        setupActionItems();

        FrameLayout endFragment = (FrameLayout)getActivity().findViewById(R.id.end_fragment_container);
        if (endFragment == null){
            setupInfoItemView();
        }

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

            //Start retrieve task
            if (mBudgetInfoItemPos != -1){
                //Display progressBar
                mDataLoadingProgressBar.setVisibility(View.VISIBLE);

                new RetrieveFullTrackFromBackend().execute(mBudgetInfoItemList.get(mBudgetInfoItemPos).getIDAsString());
            }

        }
    }

    private void setupInfoItemView(){

        ViewHolder holder = (ViewHolder) mInfoListRowView.getTag();

        if (holder == null){
            holder = new ViewHolder();

            holder.sortCriteriaLine1 = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_sort_criteria_line1);
            holder.sortCriteriaLine2 = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_sort_criteria_line2);
            holder.startStationName = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_start_station_name);
            holder.endStationName = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_end_station_name);
            holder.additionalInfoLine1 = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_info_line1);
            holder.additionalInfoLine2 = (TextView) mInfoListRowView.findViewById(R.id.budgetinfoitem_info_line2);
            mInfoListRowView.setTag(holder);
        }

        holder.startStationName.setText(mBudgetInfoItemList.get(mBudgetInfoItemPos).getStartStationName());
        holder.endStationName.setText(mBudgetInfoItemList.get(mBudgetInfoItemPos).getEndStationName());

        if (mCurrentSortCriteria == BudgetInfoListViewAdapter.SORT_CRITERIA_COST) {
            holder.sortCriteriaLine1.setText(String.format("%.2f", mBudgetInfoItemList.get(mBudgetInfoItemPos).getCost()) + "$");
            holder.sortCriteriaLine2.setVisibility(View.GONE);
            holder.additionalInfoLine1.setText(String.valueOf(mBudgetInfoItemList.get(mBudgetInfoItemPos).getDurationInMinutes()));
            holder.additionalInfoLine2.setVisibility(View.VISIBLE);
            holder.additionalInfoLine2.setText("min");
        }
        else if (mCurrentSortCriteria == BudgetInfoListViewAdapter.SORT_CRITERIA_DURATION){
            holder.sortCriteriaLine1.setText(String.valueOf(mBudgetInfoItemList.get(mBudgetInfoItemPos).getDurationInMinutes()));
            holder.sortCriteriaLine2.setVisibility(View.VISIBLE);
            holder.sortCriteriaLine2.setText("min");
            holder.additionalInfoLine1.setText(String.format("%.2f", mBudgetInfoItemList.get(mBudgetInfoItemPos).getCost()) + "$");
            holder.additionalInfoLine2.setVisibility(View.GONE);
        }
        else{   //SORT_CRITERIA_DATE
            Calendar cal = Calendar.getInstance();
            cal.setTime(mBudgetInfoItemList.get(mBudgetInfoItemPos).getTimestampAsDate());

            holder.sortCriteriaLine1.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            holder.sortCriteriaLine2.setVisibility(View.VISIBLE);
            holder.sortCriteriaLine2.setText(new SimpleDateFormat("MMM").format(cal.getTime()));
            holder.additionalInfoLine1.setText(String.format("%.2f", mBudgetInfoItemList.get(mBudgetInfoItemPos).getCost()) + "$");
            holder.additionalInfoLine2.setVisibility(View.GONE);
        }
    }

    public void updateWithNewTrack(int _budgetInfoItemPos, ArrayList<BudgetInfoItem> _budgetInfoItemList, int _sortCriteria){
        mBudgetInfoItemList = _budgetInfoItemList;
        mBudgetInfoItemPos = _budgetInfoItemPos;
        mCurrentSortCriteria = _sortCriteria;

        trackChanged();
    }

    private void addTrackPolylineToMap(){
        List<LatLng> latLngList = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        if(mTrackDataFromDB.get("points") != null) {
            //TODO: work out why the ValueType for "points" key is not stable
            //I'm mystified, if anyone has any kind of explanation, I want to understand !
            //Spent a few hours trying to understand it, to none available
            try {
                Iterable<TrackPoint> listTp = (Iterable<TrackPoint>) (mTrackDataFromDB.get("points"));
                for (TrackPoint tp : listTp) {
                    final LatLng latLng = new LatLng(tp.getLat(), tp.getLon());
                    latLngList.add(latLng);
                    boundsBuilder.include(latLng);
                }

            } catch (ClassCastException e) {
                Iterable<LinkedHashMap> listHm = (Iterable<LinkedHashMap>) (mTrackDataFromDB.get("points"));
                for (LinkedHashMap hm : listHm) {
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
        void onBudgetTrackDetailsFragmentInteraction(Uri uri);
    }

    //Class for ASync Loading of points data from backend
    public class RetrieveFullTrackFromBackend extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... params) {
            final GetTrackFromTimeUTCKeyStringResponse fullTrackResponse = BixiTracksExplorerAPIHelper.retrieveFullTrack(params[0]);

            if (fullTrackResponse.getTrack() != null){
                try {
                    DBHelper.putNewTrackPropertyAndSave(mBudgetInfoItemList.get(mBudgetInfoItemPos).getIDAsString(), "points", fullTrackResponse.getTrack().getPoints());
                    //Happens in ASyncTask
                    mTrackDataFromDB = DBHelper.retrieveTrack(mBudgetInfoItemList.get(mBudgetInfoItemPos).getIDAsString());
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
