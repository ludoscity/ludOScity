package com.ludoscity.findmybikes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Gevrai on 2015-04-03.
 *
 * Adapter used to show the datas of every stationItem
 */
public class FavoritesListViewAdapter extends BaseAdapter {
    LayoutInflater mInflater;
    Context mContext;

    private List<StationItem> mStationList = null;
    private LatLng mCurrentUserLatLng;

    public FavoritesListViewAdapter(Context _context, List<StationItem> _stationsNetwork, LatLng _currentUserLatLng){
        mContext = _context;
        mStationList = _stationsNetwork;
        mInflater = LayoutInflater.from(_context);
        mCurrentUserLatLng = _currentUserLatLng;
        sortStationListByClosest();
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
        TextView name;
        TextView distance;
        TextView bikeAvailability;
        TextView parkingAvailability;
        TextView locked;
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
            convertView = mInflater.inflate(R.layout.stationlistfavorites_item, null);
            holder.distance = (TextView) convertView.findViewById(R.id.favorite_distance);
            holder.name = (TextView) convertView.findViewById(R.id.favorite_stationName);
            holder.bikeAvailability = (TextView) convertView.findViewById(R.id.favorite_bikeAmount);
            holder.parkingAvailability = (TextView) convertView.findViewById(R.id.favorite_parkingAmount);
            holder.locked = (TextView) convertView.findViewById(R.id.favorite_stationLocked);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.name.setText(currentStation.getName());
        if (mCurrentUserLatLng != null) {
            holder.distance.setVisibility(View.VISIBLE);
            holder.distance.setText(currentStation.getDistanceStringFromLatLng(mCurrentUserLatLng));
        } else {
            holder.distance.setVisibility(View.GONE);
        }
        if (currentStation.isLocked()){
            holder.bikeAvailability.setVisibility(View.GONE);
            holder.parkingAvailability.setVisibility(View.GONE);
            holder.locked.setVisibility(View.VISIBLE);
        } else {
            holder.bikeAvailability.setVisibility(View.VISIBLE);
            holder.parkingAvailability.setVisibility(View.VISIBLE);
            holder.locked.setVisibility(View.GONE);
            holder.bikeAvailability.setText(String.valueOf(currentStation.getFree_bikes()));
            holder.parkingAvailability.setText(String.valueOf(currentStation.getEmpty_slots()));
        }
        return convertView;
    }
}
