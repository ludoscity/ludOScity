package com.udem.ift2906.bixitracksexplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 * TODO sort by proximity to user
 *
 */
public class StationListViewAdapter extends BaseAdapter {
    LayoutInflater mInflater;
    Context mContext;

    private List<StationItem> mStationList = null;
    private LatLng mCurrentUserLatLng;

    StationListViewAdapter(Context _context, StationsNetwork _stationsNetwork, LatLng _currentUserLatLng){
        mContext = _context;
        mStationList = _stationsNetwork.stations;
        mInflater = LayoutInflater.from(_context);
        mCurrentUserLatLng = _currentUserLatLng;
        sortStationListByClosest();
    }

    public void setCurrentUserLatLng(LatLng mCurrentUserLatLng) {
        this.mCurrentUserLatLng = mCurrentUserLatLng;
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
        ImageView directionArrow;
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
        return (long) mStationList.get(position).getUid();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.stationlist_item, null);
            holder.distance = (TextView) convertView.findViewById(R.id.station_distance);
            holder.name = (TextView) convertView.findViewById(R.id.station_name);
            holder.availability = (TextView) convertView.findViewById(R.id.station_availability);
            holder.directionArrow = (ImageView) convertView.findViewById(R.id.station_direction_arrow);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        StationItem currentStation= mStationList.get(position);
        holder.directionArrow.setRotation((float) currentStation.getBearingFromLatLng(mCurrentUserLatLng));
        if (mCurrentUserLatLng != null) {
            holder.distance.setText(String.valueOf((int)currentStation.getMeterFromLatLng(mCurrentUserLatLng)) + " m ");
            holder.directionArrow.setRotation((float) currentStation.getBearingFromLatLng(mCurrentUserLatLng));
        } else {
            //TODO better distance message if no user location available
            holder.distance.setText(String.valueOf("???"));
        }
        holder.name.setText(String.valueOf(currentStation.getName()));

        //TODO A REVOIR
        holder.availability.setText("" + currentStation.getFree_bikes()
                + "/"
                + (mStationList.get(position).getFree_bikes()+currentStation.getEmpty_slots()));

        return convertView;
    }
}
