package com.udem.ift2906.bixitracksexplorer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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

    StationListViewAdapter(Context _context, List<StationItem> _stationList){

        mContext = _context;
        mStationList = _stationList;
        mInflater = LayoutInflater.from(_context);
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //TODO POSITION ISNT NULL...
        holder.distance.setText(String.valueOf(mStationList.get(position).getMeterFromUserLocation(null)));
        holder.name.setText(String.valueOf(mStationList.get(position).getName()));

        //TODO A REVOIR
        holder.availability.setText("" + mStationList.get(position).getFree_bikes()
                + "/"
                + (mStationList.get(position).getFree_bikes()+mStationList.get(position).getEmpty_slots()));

        return convertView;
    }
}
