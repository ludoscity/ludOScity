package com.ludoscity.findmybikes;

import android.content.Context;
import android.os.Bundle;
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
    private LatLng mCurrentUserLatLng;

    private Context mCtx;

    private boolean mIsLookingForBikes;

    private int mSelectedPos = NO_POSITION;

    //TODO: move those into resources and retrieve them for display in Settings Fragment UI
    //in km/h
    private static float WALKING_AVERAGE_SPEED = 4.0f;
    private static float BIKING_AVERAGE_SPEED = 12.0f;

    private OnStationListItemClickListener mListener;

    public void saveStationList(Bundle outState) {
        outState.putParcelableArrayList("stationitem_arraylist", mStationList);
    }

    public boolean removeItem(StationItem toRemove) {
        int positionToRemove = getPositionInList(toRemove.getName());

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
        void onStationListItemClick();
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

        public StationListItemViewHolder(View itemView) {
            super(itemView);

            mProximity = (TextView) itemView.findViewById(R.id.station_proximity);
            mName = (TextView) itemView.findViewById(R.id.station_name);
            mAvailability = (TextView) itemView.findViewById(R.id.station_availability);

            mFavoriteFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_fab);
            mDirectionsFab = (FloatingActionButton) itemView.findViewById(R.id.directions_fab);
            mFabsAnchor = (FrameLayout) itemView.findViewById(R.id.fabs_anchor);
            itemView.setOnClickListener(this);
        }

        public void bindStation(StationItem _station, boolean _selected){

            mName.setText(_station.getName());

            if (mCurrentUserLatLng != null) {

                String proximityString;
                if (mIsLookingForBikes){
                    proximityString = _station.getProximityStringFromLatLng(mCurrentUserLatLng,
                            DBHelper.getWalkingProximityAsDistance(mCtx), WALKING_AVERAGE_SPEED, mCtx);
                }
                else{
                    proximityString = _station.getProximityStringFromLatLng(mCurrentUserLatLng,
                            DBHelper.getBikingProximityAsDistance(mCtx), BIKING_AVERAGE_SPEED, mCtx);
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
                mFavoriteFab.setVisibility(View.VISIBLE);
                mDirectionsFab.setVisibility(View.VISIBLE);

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

            if (mIsLookingForBikes) {
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
            StationRecyclerViewAdapter.this.setSelectionFromName(mName.getText().toString(), true);
            mListener.onStationListItemClick();
        }
    }

    public void setupStationList(ArrayList<StationItem> toSet){
        String selectedNameBefore = null;

        if (null != getSelected())
            selectedNameBefore = getSelected().getName();

        //Making a copy as sorting shouldn't interfere with the rest of the code
        mStationList.clear();
        mStationList.addAll(toSet);

        //So that list get sorted before setting selection again
        if (mCurrentUserLatLng != null) {
            LatLng buffered = mCurrentUserLatLng;
            mCurrentUserLatLng = null;
            setCurrentUserLatLng(buffered, false);
        }

        if (selectedNameBefore != null)
            setSelectionFromName(selectedNameBefore, false);
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng, boolean notify) {
        if (mCurrentUserLatLng != currentUserLatLng) {
            this.mCurrentUserLatLng = currentUserLatLng;
            sortStationListByClosest();
            if (notify)
                notifyDataSetChanged();
        }
    }

    public LatLng getCurrentUserLatLng(){
        return mCurrentUserLatLng;
    }

    public int setSelectionFromName(String stationName, boolean unselectOnTwice){

        return setSelectedPos(getPositionInList(stationName), unselectOnTwice);
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



    private int getPositionInList(String stationName){

        int toReturn = NO_POSITION;

        int i=0;
        for (StationItem stationItem: mStationList){
            if (stationItem.getName().equals(stationName)) {
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
        if (mIsLookingForBikes != isLookingForBikes){
            mIsLookingForBikes = isLookingForBikes;
            notifyDataSetChanged();
        }
    }

    public void sortStationListByClosest(){

        String selectedNameBefore = null;

        if (null != getSelected())
            selectedNameBefore = getSelected().getName();

        if (mCurrentUserLatLng != null) {
            Collections.sort(mStationList, new Comparator<StationItem>() {
                @Override
                public int compare(StationItem lhs, StationItem rhs) {
                    return (int) (lhs.getMeterFromLatLng(mCurrentUserLatLng) - rhs.getMeterFromLatLng(mCurrentUserLatLng));
                }
            });
        }

        if (selectedNameBefore != null)
            setSelectionFromName(selectedNameBefore, false);
    }
}
