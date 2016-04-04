package com.ludoscity.findmybikes;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.ludoscity.findmybikes.fragments.StationListFragment;
import com.ludoscity.findmybikes.helpers.DBHelper;
import com.ludoscity.findmybikes.utils.Utils;

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

    private ArrayList<StationItem> mStationList = new ArrayList<>();
    private LatLng mDistanceSortReferenceLatLng;
    private LatLng mDistanceDisplayReferenceLatLng;

    private Context mCtx;

    private boolean mIsLookingForBike;

    private int mSelectedPos = NO_POSITION;

    private boolean mFabAnimationRequested = false;

    private OnStationListItemClickListener mListener;

    public void saveStationList(Bundle outState) {
        outState.putParcelableArrayList("stationitem_arraylist", mStationList);
    }

    public void saveLookingForBike(Bundle outState) {
        outState.putBoolean("looking_for_bike", mIsLookingForBike);
    }

    public boolean removeItem(StationItem toRemove) {
        int positionToRemove = getStationItemPositionInList(toRemove.getId());

        if ( positionToRemove == mSelectedPos)
            mSelectedPos = NO_POSITION;

        mStationList.remove(positionToRemove);

        notifyDataSetChanged();

        return mStationList.isEmpty();
    }

    public void addItem(StationItem toAdd) {
        mStationList.add(toAdd);

        notifyDataSetChanged();
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

        holder.bindStation(mStationList.get(position), position == mSelectedPos);
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

    public class StationListItemViewHolder extends RecyclerView.ViewHolder
                implements View.OnClickListener{

        TextView mProximity;
        TextView mName;
        TextView mAvailability;

        FloatingActionButton mFavoriteFab;
        FloatingActionButton mDirectionsFab;

        //This View is gone by default. It becomes visible when a row in the recycler View is tapped
        //It's used in two ways
        //-clear the space underneath fabs final positions
        //-anchor fabs to their final position
        FrameLayout mFabsAnchor;

        private Handler mFabAnimHandler = null;
        private String mStationId;

        public StationListItemViewHolder(View itemView) {
            super(itemView);

            mProximity = (TextView) itemView.findViewById(R.id.station_proximity);
            mName = (TextView) itemView.findViewById(R.id.station_name);
            mAvailability = (TextView) itemView.findViewById(R.id.station_availability);

            mFavoriteFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_fab);
            mDirectionsFab = (FloatingActionButton) itemView.findViewById(R.id.directions_fab);
            mFabsAnchor = (FrameLayout) itemView.findViewById(R.id.fabs_anchor);
            itemView.setOnClickListener(this);

            mFavoriteFab.setOnClickListener(this);
            mDirectionsFab.setOnClickListener(this);
        }

        public void bindStation(StationItem _station, boolean _selected){

            mStationId = _station.getId();

            mName.setText(_station.getName());

            if (mDistanceDisplayReferenceLatLng != null) {

                String proximityString;
                if (mIsLookingForBike){
                    proximityString = _station.getProximityStringFromLatLng(mDistanceDisplayReferenceLatLng,
                            DBHelper.getWalkingProximityAsDistance(mCtx),
                            mCtx.getResources().getInteger(R.integer.average_walking_speed_kmh),
                            mCtx);
                }
                else{
                    proximityString = _station.getProximityStringFromLatLng(mDistanceDisplayReferenceLatLng,
                            DBHelper.getBikingProximityAsDistance(mCtx),
                            mCtx.getResources().getInteger(R.integer.average_biking_speed_kmh),
                            mCtx);
                }

                mProximity.setVisibility(View.VISIBLE);
                mProximity.setText(proximityString);
            } else {
                mProximity.setVisibility(View.GONE);
            }

            if (_selected){

                //The width percentage is updated so that the name TextView gives room to the fabs
                //RecyclerView gives us free opacity/bounds resizing animations
                PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();

                info.widthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_selected_percent, true);
                mName.requestLayout();

                //Show two fabs, anchored through app:layout_anchor="@id/fabs_anchor" stationlist_item.xml
                mFabsAnchor.setVisibility(View.VISIBLE);

                if (_station.isFavorite(mCtx))
                    mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
                else
                    mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);

                if (mFabAnimationRequested){
                    mFavoriteFab.setVisibility(View.GONE);
                    mDirectionsFab.setVisibility(View.GONE);

                    mFabAnimHandler = new Handler();

                    mFabAnimHandler.postDelayed(new Runnable() {
                        public void run() {
                            mFavoriteFab.show();
                            mDirectionsFab.show();
                            mFabAnimHandler = null;
                        }
                    }, 50);
                    mFabAnimationRequested = false;
                }
                else if (mFabAnimHandler == null)
                {
                    mFavoriteFab.setVisibility(View.VISIBLE);
                    mDirectionsFab.setVisibility(View.VISIBLE);
                }

                //manipulating last item column, displaying bikes or docks numbers
                //padding is direct but didn't give desired results
                //mAvailability.setPadding(Utils.dpToPx(20,mCtx), 0,0,0);
                //So we will update MARGINS
                //This is to correctly position the directions fab
                //it got app:layout_anchorGravity="end|center_vertical"
                //but uses the *middle* of the sprite
                //So I want it offset by half its width minus any padding I wannna put in there
                //40/2 - 4 = 20 - 4 = 16.
                PercentRelativeLayout.LayoutParams availParams =(PercentRelativeLayout.LayoutParams) mAvailability.getLayoutParams();
                availParams.setMargins((int)mCtx.getResources().getDimension(R.dimen.station_availability_margin_left_selected), 0, 0, 0);
                mAvailability.requestLayout();

            }
            else{
                //name width percentage restoration
                PercentRelativeLayout.LayoutParams params =(PercentRelativeLayout.LayoutParams) mName.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                info.widthPercent = Utils.getPercentResource(mCtx, R.dimen.name_column_width_default_percent, true);
                mName.requestLayout();

                //Hidding two fabs and their anchor
                mFabsAnchor.setVisibility(View.GONE);
                mFavoriteFab.setVisibility(View.GONE);
                mDirectionsFab.setVisibility(View.GONE);

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
            }

            if (mIsLookingForBike) {
                mAvailability.setText(String.valueOf(_station.getFree_bikes()));
                setBackgroundColor(_selected, _station.getFree_bikes());

            }
            else {
                mAvailability.setText(String.valueOf(_station.getEmpty_slots()));
                setBackgroundColor(_selected, _station.getEmpty_slots());
            }
        }

        private void setBackgroundColor(boolean selected, int availabilityValue){
            if (!selected)
                itemView.setBackgroundResource(android.R.color.transparent);
            else{
                if (availabilityValue == 0)
                    itemView.setBackgroundResource(R.color.stationlist_item_background_red);
                else if (availabilityValue < 3)
                    itemView.setBackgroundResource(R.color.stationlist_item_background_yellow);
                else
                    itemView.setBackgroundResource(R.color.stationlist_item_background_green);
            }
        }

        @Override
        public void onClick(View view) {

            switch (view.getId()){
                case R.id.list_item_root:

                    int newlySelectedPos = StationRecyclerViewAdapter.this.setSelection(mStationId, false);

                    mListener.onStationListItemClick(StationListFragment.STATION_LIST_ITEM_CLICK_PATH);
                    mFabAnimationRequested = newlySelectedPos != mSelectedPos;

                    break;
                case R.id.favorite_fab:
                    mListener.onStationListItemClick(StationListFragment.STATION_LIST_FAVORITE_FAB_CLICK_PATH);
                    //ordering matters
                    if (getSelected().isFavorite(mCtx))
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_24dp);
                    else
                        mFavoriteFab.setImageResource(R.drawable.ic_action_favorite_outline_24dp);
                    break;

                case R.id.directions_fab:
                    mListener.onStationListItemClick(StationListFragment.STATION_LIST_DIRECTIONS_FAB_CLICK_PATH);
                    break;
            }
        }
    }

    public void requestFabAnimation(){ mFabAnimationRequested = true; }

    public void setupStationList(ArrayList<StationItem> _toSet, LatLng _sortReferenceLatLng,
                                 LatLng _distanceReferenceLatLng){
        String selectedIdBefore = null;

        if (null != getSelected())
            selectedIdBefore = getSelected().getId();

        //Making a copy as sorting shouldn't interfere with the rest of the code
        mStationList.clear();
        mStationList.addAll(_toSet);

        //Forcing sorting so that a currently displayed selection doesn't glitch when set again
        setDistanceSortReferenceLatLngAndSortIfRequired(_sortReferenceLatLng, true);

        mDistanceDisplayReferenceLatLng = _distanceReferenceLatLng;

        if (selectedIdBefore != null)
            setSelection(selectedIdBefore, false);
    }

    public void setDistanceSortReferenceLatLngAndSortIfRequired(LatLng _sortReferenceLatLng, boolean _forceSort) {
        if (_forceSort || mDistanceSortReferenceLatLng != _sortReferenceLatLng) {
            mDistanceSortReferenceLatLng = _sortReferenceLatLng;
            sortStationListByClosestToReference();
            notifyDataSetChanged();
        }
    }

    public void setDistanceDisplayReferenceLatLng(LatLng _toSet, boolean _notify) {
        mDistanceDisplayReferenceLatLng = _toSet;
        if (_notify)
            notifyDataSetChanged();
    }

    public LatLng getSortReferenceLatLng(){
        return mDistanceSortReferenceLatLng;
    }

    public LatLng getDistanceReferenceLatLng(){
        return mDistanceDisplayReferenceLatLng;
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

    public String getClosestStationWithAvailability(boolean _lookingForBike){

        String toReturn = "";

        //TODO : take in account lock status
        for (StationItem stationItem: mStationList){
            if (_lookingForBike) {
                if (stationItem.getFree_bikes() > 0) {
                    toReturn = stationItem.getId();
                    break;
                }
            }
            else {
                if (stationItem.getEmpty_slots() > 0){
                    toReturn = stationItem.getId();
                    break;
                }
            }
        }

        //failsafe
        if (toReturn.length() == 0 && !mStationList.isEmpty())
            toReturn = mStationList.get(0).getId();

        return toReturn;
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

    public void sortStationListByClosestToReference(){

        String selectedIdBefore = null;

        if (null != getSelected())
            selectedIdBefore = getSelected().getId();

        if (mDistanceSortReferenceLatLng != null) {
            Collections.sort(mStationList, new Comparator<StationItem>() {
                @Override
                public int compare(StationItem lhs, StationItem rhs) {
                    return (int) (lhs.getMeterFromLatLng(mDistanceSortReferenceLatLng) - rhs.getMeterFromLatLng(mDistanceSortReferenceLatLng));
                }
            });
        }

        if (selectedIdBefore != null)
            setSelection(selectedIdBefore, false);
    }
}
