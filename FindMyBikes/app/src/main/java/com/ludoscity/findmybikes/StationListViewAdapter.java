package com.ludoscity.findmybikes;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
public class StationListViewAdapter extends BaseAdapter {
    LayoutInflater mInflater;
    Context mContext;

    private List<StationItem> mStationList = null;
    private LatLng mCurrentUserLatLng;

    private boolean mIsLookingForBikes;

    public StationListViewAdapter(Context _context, ArrayList<StationItem> _stationsNetwork, LatLng _currentUserLatLng, boolean isLookingForBikes){
        mContext = _context;
        //Making a copy as sorting shouldn't interfere with the rest of the code
        mStationList = new ArrayList<>(_stationsNetwork);
        mInflater = LayoutInflater.from(_context);
        mCurrentUserLatLng = _currentUserLatLng;
        mIsLookingForBikes = isLookingForBikes;
        sortStationListByClosest();
    }

    public void setCurrentUserLatLng(LatLng currentUserLatLng) {
        if (mCurrentUserLatLng != currentUserLatLng) {
            this.mCurrentUserLatLng = currentUserLatLng;
            sortStationListByClosest();
            notifyDataSetChanged();
        }
    }

    public LatLng getCurrentUserLatLng(){
        return mCurrentUserLatLng;
    }

    public int getPositionInList(String stationName){
        int i = 0;
        for (StationItem stationItem: mStationList){
            if (stationItem.getName().equals(stationName))
                return i;
            ++i;
        }
        return -1;
    }

    public void lookingForBikesNotify(boolean isLookingForBikes) {
        if (mIsLookingForBikes != isLookingForBikes){
            mIsLookingForBikes = isLookingForBikes;
            notifyDataSetChanged();
        }
    }

    public void sortStationListByClosest(){
        if (mCurrentUserLatLng != null) {
            Collections.sort(mStationList, new Comparator<StationItem>() {
                @Override
                public int compare(StationItem lhs, StationItem rhs) {
                    return (int) (lhs.getMeterFromLatLng(mCurrentUserLatLng) - rhs.getMeterFromLatLng(mCurrentUserLatLng));
                }
            });
        }
    }

    public class ViewHolder{
        TextView distance;
        TextView name;
        TextView availability;
    }

    @Override
    public int getCount() {
        return mStationList.size();
    }

    @Override
    public Object getItem(int position) {
        return mStationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StationItem currentStation= mStationList.get(position);
        final ViewHolder holder;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.stationlist_item, null);
            holder.distance = (TextView) convertView.findViewById(R.id.station_distance);
            holder.name = (TextView) convertView.findViewById(R.id.station_name);
            holder.availability = (TextView) convertView.findViewById(R.id.station_availability);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (mCurrentUserLatLng != null) {
            holder.distance.setVisibility(View.VISIBLE);
            holder.distance.setText(currentStation.getDistanceStringFromLatLng(mCurrentUserLatLng));
        } else {
            holder.distance.setVisibility(View.GONE);
        }

        holder.name.setText(currentStation.getName());
        // Show bike/parking ammount depending on search parameter
        if (mIsLookingForBikes)
            holder.availability.setText(String.valueOf(currentStation.getFree_bikes()));
        else
            holder.availability.setText(String.valueOf(currentStation.getEmpty_slots()));


        //That's a mystery
        holder.name.setTextColor(Color.DKGRAY);
        holder.availability.setTextColor(Color.DKGRAY);
        holder.distance.setTextColor(Color.DKGRAY);

        return convertView;
    }
}
