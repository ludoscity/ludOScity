package com.ludoscity.findmybikes;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
public class StationRecyclerViewAdapter extends RecyclerView.Adapter<StationRecyclerViewAdapter.StationListItemViewHolder> {

    public static final String AVAILABILITY_POSTFIX_START_SEQUENCE = "_AVAILABILITY_";
    public static final String AOK_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "AOK";
    public static final String BAD_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "BAD";
    public static final String CRITICAL_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "CRI";
    private static final String LOCKED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "LCK";
    private static final String OUTDATED_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "OUT";
    private static final String ERROR_AVAILABILITY_POSTFIX = AVAILABILITY_POSTFIX_START_SEQUENCE + "ERR";

    private ArrayList<StationItem> mStationList = new ArrayList<>();
    private Comparator<StationItem> mStationSortComparator;

    private Context mCtx;

    private boolean mIsLookingForBike;
    private boolean mShowProximity;

    private int mSelectedPos = NO_POSITION;

    private boolean mFabAnimationRequested = false;

    private OnStationListItemClickListener mListener;
    private boolean mRespondToClick = true;
    private boolean mOutdatedAvailability = false;

    //TODO: come up with a design that doesn't require dynamic casting
    public static class DistanceComparator implements Comparator<StationItem>, Parcelable {

        LatLng mRefLatLng;
        public DistanceComparator(LatLng _fromLatLng){mRefLatLng = _fromLatLng;}

        LatLng getDistanceRef() { return mRefLatLng; }

        @Override
        public int compare(StationItem lhs, StationItem rhs) {
            return (int) (lhs.getMeterFromLatLng(mRefLatLng) - rhs.getMeterFromLatLng(mRefLatLng));
        }

        private DistanceComparator(Parcel in){
            double latitude = in.readDouble();
            double longitude = in.readDouble();

            mRefLatLng = new LatLng(latitude, longitude);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(mRefLatLng.latitude);
            dest.writeDouble(mRefLatLng.longitude);
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

            @Override
            public DistanceComparator createFromParcel(Parcel source) {
                return new DistanceComparator(source);
            }

            @Override
            public DistanceComparator[] newArray(int size) {
                return new DistanceComparator[size];
            }
        };
    }

    public static class TotalTripTimeComparator implements Comparator<StationItem>, Parcelable{

        private final LatLng mStationALatLng;
        private final LatLng mDestinationLatLng;
        private final float mWalkingSpeedKmh;
        private final float mBikingSpeedKmh;

        private final int mTimeUserToAMinutes;

        public TotalTripTimeComparator(float _walkingSpeedKmh, float _bikingSpeedKmh, LatLng _userLatLng,
                                       LatLng _stationALatLng, LatLng _destinationLatLng){

            mWalkingSpeedKmh = _walkingSpeedKmh;
            mBikingSpeedKmh = _bikingSpeedKmh;

            mTimeUserToAMinutes = Utils.computeTimeBetweenInMinutes(_userLatLng, _stationALatLng, mWalkingSpeedKmh);

            mStationALatLng = _stationALatLng;
            mDestinationLatLng = _destinationLatLng;
        }

        private TotalTripTimeComparator(Parcel in){

            double latitude = in.readDouble();
            double longitude = in.readDouble();
            mStationALatLng = new LatLng(latitude, longitude);

            latitude = in.readDouble();
            longitude = in.readDouble();
            mDestinationLatLng = new LatLng(latitude, longitude);

            mWalkingSpeedKmh = in.readFloat();
            mBikingSpeedKmh = in.readFloat();
            mTimeUserToAMinutes = in.readInt();
        }

        TotalTripTimeComparator getUpdatedComparatorFor(LatLng _userLatLng, LatLng _stationALatLng){
            return new TotalTripTimeComparator(mWalkingSpeedKmh, mBikingSpeedKmh, _userLatLng,
                    _stationALatLng != null ? _stationALatLng : mStationALatLng, mDestinationLatLng );
        }

        @Override
        public int compare(StationItem lhs, StationItem rhs) {

            int lhsWalkTime = calculateWalkTimeMinute(lhs);
            int rhsWalkTime = calculateWalkTimeMinute(rhs);

            int lhsBikeTime = calculateBikeTimeMinute(lhs);
            int rhsBikeTime = calculateBikeTimeMinute(rhs);

            int totalTimeDiff = (lhsWalkTime + lhsBikeTime) - (rhsWalkTime + rhsBikeTime);

            if (totalTimeDiff != 0)
                return totalTimeDiff;
            else
                return lhsWalkTime - rhsWalkTime;
        }

        int calculateWalkTimeMinute(StationItem _stationB){

            int timeBtoDestMinutes = 0;

            if (mDestinationLatLng != null)
                timeBtoDestMinutes = Utils.computeTimeBetweenInMinutes(_stationB.getLocation(),
                        mDestinationLatLng, mWalkingSpeedKmh);

            return mTimeUserToAMinutes + timeBtoDestMinutes;

        }

        int calculateBikeTimeMinute(StationItem _stationB){

            return Utils.computeTimeBetweenInMinutes(mStationALatLng, _stationB.getLocation(),
                    mBikingSpeedKmh);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(mStationALatLng.latitude);
            dest.writeDouble(mStationALatLng.longitude);
            dest.writeDouble(mDestinationLatLng.latitude);
            dest.writeDouble(mDestinationLatLng.longitude);
            dest.writeFloat(mWalkingSpeedKmh);
            dest.writeFloat(mBikingSpeedKmh);
            dest.writeInt(mTimeUserToAMinutes);
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){

            @Override
            public TotalTripTimeComparator createFromParcel(Parcel source) {
                return new TotalTripTimeComparator(source);
            }

            @Override
            public TotalTripTimeComparator[] newArray(int size) {
                return new TotalTripTimeComparator[size];
            }
        };
    }

    public void saveLookingForBike(Bundle outState) {
        outState.putBoolean("looking_for_bike", mIsLookingForBike);
    }

    public void saveIsAvailabilityOutdated(Bundle outState){
        outState.putBoolean("availability_outdated", mOutdatedAvailability);
    }

    public void notifyStationChanged(String _stationId) {
        notifyItemChanged(getStationItemPositionInList(_stationId));
    }

    public boolean removeItem(StationItem toRemove) {
        int positionToRemove = getStationItemPositionInList(toRemove.getId());

        if ( positionToRemove == mSelectedPos)
            mSelectedPos = NO_POSITION;

        mStationList.remove(positionToRemove);

        notifyItemRemoved(positionToRemove);

        return mStationList.isEmpty();
    }

    public void addItem(StationItem toAdd) {
        mStationList.add(toAdd);

        notifyItemInserted(mStationList.size()-1);
    }

    public void setAvailabilityOutdated(boolean _toSet) {

        //TODO: refactor with MVC in mind. Outdated status is model
        //There's a copy in NearbyActivity (loosely being the controller)
        //This local one is used for bindings
        if (mOutdatedAvailability != _toSet) {

            mOutdatedAvailability = _toSet;
            notifyDataSetChanged();
        }
    }

    public void setClickResponsiveness(boolean _toSet) {
        mRespondToClick = _toSet;
    }

    public interface OnStationListItemClickListener {
        void onStationListItemClick(String _path);
    }

    public StationRecyclerViewAdapter(OnStationListItemClickListener listener,
                                      Context _ctx){
        super();
        mListener = listener;
        mCtx = _ctx;
    }

    @Override
    public void onBindViewHolder(StationListItemViewHolder holder, int position) {

        holder.bindStation(mStationList.get(position), position == mSelectedPos, mShowProximity);
    }

    @Override
    public StationListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.stationlist_item, parent, false);
        return new StationListItemViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mStationList.size();
    }

    class StationListItemViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

        TextView mProximity;
        TextView mName;
        TextView mAvailability;

        FloatingActionButton mFavoriteFab;

        //This View is gone by default. It becomes visible when a row in the recycler View is tapped
        //It's used in two ways
        //-clear the space underneath fabs final positions
        //-anchor fabs to their final position
        FrameLayout mFabsAnchor;

        private Handler mFabAnimHandler = null;
        private String mStationId;

        private NumberFormat mNumberFormat = NumberFormat.getInstance();

        StationListItemViewHolder(View itemView) {
            super(itemView);

            mProximity = (TextView) itemView.findViewById(R.id.station_proximity);
            mName = (TextView) itemView.findViewById(R.id.station_name);
            mAvailability = (TextView) itemView.findViewById(R.id.station_availability);

            mFavoriteFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_fab);
            mFabsAnchor = (FrameLayout) itemView.findViewById(R.id.fabs_anchor);
            itemView.setOnClickListener(this);

            mFavoriteFab.setOnClickListener(this);
        }

        void bindStation(StationItem _station, boolean _selected, boolean _showProximity){

            mStationId = _station.getId();

            if (_showProximity && mStationSortComparator != null) {

                String proximityString;

                if (mStationSortComparator instanceof TotalTripTimeComparator){
                    TotalTripTimeComparator comparator = (TotalTripTimeComparator) mStationSortComparator;

                    int totalTime = comparator.calculateWalkTimeMinute(_station) + comparator.calculateBikeTimeMinute(_station);

                    proximityString = Utils.durationToProximityString(totalTime, false, mNumberFormat, mCtx);
                }
                else {

                    DistanceComparator comparator = (DistanceComparator) mStationSortComparator;

                    proximityString = mIsLookingForBike ?
                            Utils.getWalkingProximityString(
                                    _station.getLocation(), comparator.getDistanceRef(), false, mNumberFormat, mCtx) :
                            Utils.getBikingProximityString(
                                    _station.getLocation(), comparator.getDistanceRef(), false, mNumberFormat, mCtx);
                }

                mProximity.setVisibility(View.VISIBLE);
                mProximity.setText(proximityString);
            } else {
                mProximity.setVisibility(View.GONE);
            }

            if (_station.isFavorite(mCtx))
                mName.setText(_station.getFavoriteName(mCtx, false));
            else
                mName.setText(_station.getName());


            /////////////////////////////////////////////////////////////////////
            //Remove this code to display a fab on selected station
            float nameWidthPercent;

            if (mShowProximity) {
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true);
            }
            else{
                nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true) + Utils.getPercentResource(mCtx, R.dimen.proximity_column_width_percent, true);
            }

            //name width percentage restoration
            PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
            info.widthPercent = nameWidthPercent;
            mName.requestLayout();
            /////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////
            ////Use this code for displaying a fab on selected station
            /*
            if (_selected){

                float nameWidthPercent;

                if (mShowProximity) {
                    nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_selected_percent, true);
                }
                else{
                    nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_selected_percent, true) + Utils.getPercentResource(mCtx, R.dimen.proximity_column_width_percent, true);
                }

                //The width percentage is updated so that the name TextView gives room to the fabs
                //RecyclerView gives us free opacity/bounds resizing animations
                PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                info.widthPercent = nameWidthPercent;
                mName.requestLayout();

                //Show two fabs, anchored through app:layout_anchor="@id/fabs_anchor" stationlist_item.xml
                /*mFabsAnchor.setVisibility(View.VISIBLE);

                if (_station.isFavorite(mCtx))
                    mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
                else
                    mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);

                if (mFabAnimationRequested){
                    mFavoriteFab.setVisibility(View.GONE);

                    mFabAnimHandler = new Handler();

                    mFabAnimHandler.postDelayed(new Runnable() {
                        public void run() {
                            mFavoriteFab.show();
                            mFabAnimHandler = null;
                        }
                    }, 50);
                    mFabAnimationRequested = false;
                }
                else if (mFabAnimHandler == null)
                {
                    mFavoriteFab.setVisibility(View.VISIBLE);
                }*/
                //TODO: use this to rework landscape layout
                //manipulating last item column, displaying bikes or docks numbers
                //padding is direct but didn't give desired results
                //mAvailability.setPadding(Utils.dpToPx(20,mCtx), 0,0,0);
                //So we will update MARGINS
                //This is to correctly position the directions fab
                //it got app:layout_anchorGravity="end|center_vertical"
                //but uses the *middle* of the sprite
                //So I want it offset by half its width minus any padding I wannna put in there
                //40/2 - 4 = 20 - 4 = 16.
                /*PercentRelativeLayout.LayoutParams availParams =(PercentRelativeLayout.LayoutParams) mAvailability.getLayoutParams();
                availParams.setMargins((int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_left_selected), 0, 0, 0);
                mAvailability.requestLayout();

            }
            else{

                float nameWidthPercent;

                if (mShowProximity) {
                    nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true);
                }
                else{
                    nameWidthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true) + Utils.getPercentResource(mCtx, R.dimen.proximity_column_width_percent, true);
                }

                //name width percentage restoration
                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                info.widthPercent = nameWidthPercent;
                mName.requestLayout();

                //Hidding two fabs and their anchor
                mFabsAnchor.setVisibility(View.GONE);
                mFavoriteFab.setVisibility(View.GONE);

                //TODO: use this to rework landscape layout
                //margins, not padding. Set to 0 when item is not selected (hidden fabs)
                //mAvailability.setPadding(0, 0, 0, 0);
                //see if(_selected)
                PercentRelativeLayout.LayoutParams availParams =(PercentRelativeLayout.LayoutParams) mAvailability.getLayoutParams();
                //availParams.setMargins(0,0,0,0);
                availParams.setMargins((int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_default),
                        (int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_default),
                        (int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_default),
                        (int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_default));
                mAvailability.requestLayout();
            }*/
            ////////////////////////////////////////////////////////////////////////////////

            if (mIsLookingForBike) {
                mAvailability.setText(mNumberFormat.format(_station.getFree_bikes()));
                setColorAndTransparencyFeedback(_selected, _station.getFree_bikes());

            }
            else {
                mAvailability.setText(mNumberFormat.format(_station.getEmpty_slots()));
                setColorAndTransparencyFeedback(_selected, _station.getEmpty_slots());
            }

            if (mOutdatedAvailability) {
                mAvailability.getPaint().setStrikeThruText(true);
                mAvailability.getPaint().setTypeface(Typeface.DEFAULT);
                //mAvailability.getPaint().setStrokeWidth(6.5f);
            }
            else {
                mAvailability.getPaint().setTypeface(Typeface.DEFAULT_BOLD);
                mAvailability.getPaint().setStrikeThruText(false);
                //thks @romainguy ! ;)
                //http://stackoverflow.com/questions/6796809/remove-a-paint-flag-in-android
            }
        }

        private void setColorAndTransparencyFeedback(boolean selected, int availabilityValue){

            if (!mOutdatedAvailability) {

                if (availabilityValue <= DBHelper.getCriticalAvailabilityMax(mCtx)) {
                    if (selected) {
                        itemView.setBackgroundResource(R.color.stationlist_item_selected_background_red);
                        mProximity.setAlpha(1.f);
                        mName.setAlpha(1.f);
                        mAvailability.setAlpha(1.f);
                    }
                    else {
                        itemView.setBackgroundResource(R.color.stationlist_item_background_red);
                        float alpha = mCtx.getResources().getFraction(R.fraction.station_item_critical_availability_alpha, 1, 1);
                        mProximity.setAlpha(alpha);
                        mName.setAlpha(alpha);
                        mAvailability.setAlpha(alpha);
                    }
                } else if (availabilityValue <= DBHelper.getBadAvailabilityMax(mCtx)) {
                    if (selected) {
                        itemView.setBackgroundResource(R.color.stationlist_item_selected_background_yellow);
                        mProximity.setAlpha(1.f);
                        mName.setAlpha(1.f);
                        mAvailability.setAlpha(1.f);
                    }
                    else {
                        itemView.setBackgroundResource(R.color.stationlist_item_background_yellow);
                        mName.setAlpha(mCtx.getResources().getFraction(R.fraction.station_item_name_bad_availability_alpha, 1, 1));
                        mAvailability.setAlpha(mCtx.getResources().getFraction(R.fraction.station_item_availability_bad_availability_alpha, 1, 1));
                        mProximity.setAlpha(1.f);
                    }
                } else {
                    if (selected)
                        itemView.setBackgroundResource(R.color.stationlist_item_selected_background_green);
                    else
                        itemView.setBackgroundResource(android.R.color.transparent);

                    mName.setAlpha(1.f);
                    mAvailability.setAlpha(1.f);
                    mProximity.setAlpha(1.f);
                }
            }
            else {
                if (selected)
                    itemView.setBackgroundResource(R.color.theme_accent);
                else
                    itemView.setBackgroundResource(android.R.color.transparent);

                mAvailability.setAlpha(mCtx.getResources().getFraction(R.fraction.station_item_critical_availability_alpha, 1, 1));
                mProximity.setAlpha(1.f);
                mName.setAlpha(1.f);
            }
        }

        @Override
        public void onClick(View view) {

            switch (view.getId()){

                case R.id.list_item_root:

                    if (!mRespondToClick){
                        mListener.onStationListItemClick(StationListFragment.STATION_LIST_INACTIVE_ITEM_CLICK_PATH);
                    } else {

                        int oldSelectedPos = mSelectedPos;

                        StationRecyclerViewAdapter.this.setSelection(mStationId, false);

                        if (oldSelectedPos != mSelectedPos) {

                            mListener.onStationListItemClick(StationListFragment.STATION_LIST_ITEM_CLICK_PATH);
                            requestFabAnimation();
                        }
                    }
                    break;

                case R.id.favorite_fab:
                    //must redo binding for favorite name display
                    notifyItemChanged(getStationItemPositionInList(getSelected().getId()));
                    mListener.onStationListItemClick(StationListFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH);
                    //ordering matters
                    if (getSelected().isFavorite(mCtx))
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
                    else
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
                    break;
            }
        }

        ViewTarget getFavoriteFabTarget(){
            return new ViewTarget(mFavoriteFab);
        }
    }

    public ViewTarget getSelectedItemFavoriteFabViewTarget(final RecyclerView _recyclerView){

        ViewTarget toReturn = null;

        if (mSelectedPos != NO_POSITION)
        {
            toReturn = ((StationRecyclerViewAdapter.StationListItemViewHolder)_recyclerView.findViewHolderForAdapterPosition(mSelectedPos)).getFavoriteFabTarget();
        }

        return toReturn;
    }

    public void requestFabAnimation(){ mFabAnimationRequested = true; }

    public void setupStationList(ArrayList<StationItem> _toSet, Comparator<StationItem> _sortComparator){
        String selectedIdBefore = null;

        if (null != getSelected())
            selectedIdBefore = getSelected().getId();

        //Making a copy as sorting shouldn't interfere with the rest of the code
        mStationList.clear();
        mStationList.addAll(_toSet);

        setStationSortComparatorAndSort(_sortComparator);

        if (selectedIdBefore != null)
            setSelection(selectedIdBefore, false);
    }

    public void setShowProximity(boolean _showProximity){
        mShowProximity = _showProximity;
    }

    public void setStationSortComparatorAndSort(Comparator<StationItem> _comparator){
        mStationSortComparator = _comparator;
        sortStationList();
        notifyDataSetChanged();
    }

    //_stationALatLng can be null
    public void updateTotalTripSortComparator(LatLng _userLatLng, LatLng _stationALatLng){
        if (mStationSortComparator instanceof TotalTripTimeComparator){
            TotalTripTimeComparator comparator = (TotalTripTimeComparator)mStationSortComparator;

            setStationSortComparatorAndSort(comparator.getUpdatedComparatorFor(_userLatLng, _stationALatLng));
        }
    }

    //TODO: remove this accessor (don't share your private parts)
    public Comparator<StationItem> getSortComparator(){
        return mStationSortComparator;
    }

    public int setSelection(String _stationId, boolean unselectOnTwice){

        return setSelectedPos(getStationItemPositionInList(_stationId), unselectOnTwice);
    }

    public StationItem getSelected(){
        StationItem toReturn = null;

        if (mSelectedPos != NO_POSITION && mSelectedPos < mStationList.size())
            toReturn = mStationList.get(mSelectedPos);

        return toReturn;
    }

    public int getSelectedPos(){
        return mSelectedPos;
    }

    public void clearSelection(){
        int selectedBefore = mSelectedPos;
        mSelectedPos = NO_POSITION;

        if (selectedBefore != NO_POSITION)
            notifyItemChanged(selectedBefore);
    }

    private String getClosestStationIdWithAvailability(boolean _lookingForBike){

        StringBuilder availabilityDataPostfixBuilder = new StringBuilder("");

        if (mOutdatedAvailability){
            availabilityDataPostfixBuilder.append(mStationList.get(0).getId()).append(OUTDATED_AVAILABILITY_POSTFIX);
            return availabilityDataPostfixBuilder.toString();
        }

        int badOrAOKStationCount = 0;
        final long minimumServiceCount = Math.round(mStationList.size()*0.1);   //10%

        for (StationItem stationItem: mStationList){ //mStationList is SORTED BY DISTANCE
            if (_lookingForBike) {
                if (!stationItem.isLocked()) {

                    if (stationItem.getFree_bikes() > DBHelper.getCriticalAvailabilityMax(mCtx)) {

                        if (badOrAOKStationCount == 0) {
                            if (stationItem.getFree_bikes() <= DBHelper.getBadAvailabilityMax(mCtx)) {

                                availabilityDataPostfixBuilder.insert(0, stationItem.getId() + BAD_AVAILABILITY_POSTFIX);
                            } else {
                                availabilityDataPostfixBuilder.insert(0, stationItem.getId() + AOK_AVAILABILITY_POSTFIX);
                            }
                        }

                        ++badOrAOKStationCount;

                        if (badOrAOKStationCount >= minimumServiceCount)
                            break;
                    }
                    else if (badOrAOKStationCount == 0){
                        availabilityDataPostfixBuilder.append(stationItem.getId())
                                .append(CRITICAL_AVAILABILITY_POSTFIX);
                    }
                }
                else if(badOrAOKStationCount == 0) {
                    availabilityDataPostfixBuilder.append(stationItem.getId())
                            .append(LOCKED_AVAILABILITY_POSTFIX);
                }
            }
            else {  //A locked station accepts bike returns

                if (stationItem.getEmpty_slots() > DBHelper.getCriticalAvailabilityMax(mCtx)){

                    if (badOrAOKStationCount == 0) {

                        if (stationItem.getEmpty_slots() <= DBHelper.getBadAvailabilityMax(mCtx)) {

                            availabilityDataPostfixBuilder.insert(0, stationItem.getId() + BAD_AVAILABILITY_POSTFIX);
                        } else {

                            availabilityDataPostfixBuilder.insert(0, stationItem.getId() + AOK_AVAILABILITY_POSTFIX);
                        }
                    }

                    ++badOrAOKStationCount;

                    if (badOrAOKStationCount >= minimumServiceCount)
                        break;
                }
                else if (badOrAOKStationCount == 0){
                    availabilityDataPostfixBuilder.append(stationItem.getId())
                            .append(CRITICAL_AVAILABILITY_POSTFIX);
                }
            }
        }

        //failsafe if no bike could be found
        if (badOrAOKStationCount == 0 && !mStationList.isEmpty())
            availabilityDataPostfixBuilder.append(mStationList.get(0).getId()).append(LOCKED_AVAILABILITY_POSTFIX);
        else if(badOrAOKStationCount != 0 && badOrAOKStationCount < minimumServiceCount){
            //if less than 10% of the network could provide service but a bike could still be found, let's prevent tweeting from happening
            int firstBadOrOkIdx = availabilityDataPostfixBuilder.indexOf(BAD_AVAILABILITY_POSTFIX) != -1 ?
                    availabilityDataPostfixBuilder.indexOf(BAD_AVAILABILITY_POSTFIX) :
                    availabilityDataPostfixBuilder.indexOf(AOK_AVAILABILITY_POSTFIX);

            availabilityDataPostfixBuilder.replace(firstBadOrOkIdx, firstBadOrOkIdx + ERROR_AVAILABILITY_POSTFIX.length(), ERROR_AVAILABILITY_POSTFIX);
        }

        return availabilityDataPostfixBuilder.toString();
    }

    public String retrieveClosestRawIdAndAvailability(boolean _lookingForBike){

            return getClosestStationIdWithAvailability(_lookingForBike);
    }

    public LatLng getClosestAvailabilityLatLng(boolean _lookingForBike){
        //DBHelper can't be trusted !!
        //StationItem closest = DBHelper.getStation(Utils.extractClosestAvailableStationIdFromProcessedString(getClosestStationIdWithAvailability(_lookingForBike)));

        StationItem closest = null;

        String closestId = Utils.extractClosestAvailableStationIdFromProcessedString(getClosestStationIdWithAvailability(_lookingForBike));

        for (StationItem station : mStationList){
            if (station.getId().equalsIgnoreCase(closestId)){
                closest = station;
                break;
            }
        }

        if (closest != null)
            return closest.getLocation();

        return null;
    }

    private int getStationItemPositionInList(String _stationId){

        int toReturn = NO_POSITION;

        int i=0;
        for (StationItem stationItem: mStationList){
            if (stationItem.getId().equals(_stationId)) {
                toReturn = i;
                break;
            }
            ++i;
        }
        return toReturn;
    }

    public int setSelectedPos(int pos, boolean unselectedOnTwice){

        int toReturn = NO_POSITION;

        if (mSelectedPos == pos)
            if (unselectedOnTwice)
                clearSelection();
            else
                toReturn = mSelectedPos;
        else {
            notifyItemChanged(mSelectedPos);
            mSelectedPos = pos;
            notifyItemChanged(pos);
            toReturn = mSelectedPos;
        }

        return toReturn;
    }

    public void lookingForBikesNotify(boolean isLookingForBikes) {
        if (mIsLookingForBike != isLookingForBikes){
            mIsLookingForBike = isLookingForBikes;
            notifyDataSetChanged();
        }
    }

    private void sortStationList(){
        String selectedIdBefore = null;

        if (null != getSelected())
            selectedIdBefore = getSelected().getId();

        if (mStationSortComparator != null)
            Collections.sort(mStationList, mStationSortComparator);

        if (selectedIdBefore != null)
            setSelection(selectedIdBefore, false);
    }
}
