package com.udem.ift2906.bixitracksexplorer.BudgetInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.udem.ift2906.bixitracksexplorer.R;

import java.util.List;

/**
 * Created by F8Full on 2015-04-07.
 * Custom adapter used in BudgetInfoFragment
 */
public class BudgetInfoListViewAdapter extends BaseAdapter{
    LayoutInflater mInflater;

    private List<BudgetInfoItem> mItemList = null;

    public BudgetInfoListViewAdapter(Context _context, List<BudgetInfoItem> _itemList){
        mItemList = _itemList;
        mInflater = LayoutInflater.from(_context);
    }

    private class ViewHolder{
        TextView sortCriteria;
        TextView startStationName;
        TextView endStationName;
        TextView additionalInfoLine1;
        TextView additionalInfoLine2;
    }



    @Override
    public int getCount() { return mItemList.size();  }

    @Override
    public Object getItem(int position) { return mItemList.get(position); }

    @Override
    public long getItemId(int position) { return mItemList.get(position).getID(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.budgetinfolist_item, null);
            holder.sortCriteria = (TextView) convertView.findViewById(R.id.budgetinfoitem_sort_criteria);
            holder.startStationName = (TextView) convertView.findViewById(R.id.budgetinfoitem_start_station_name);
            holder.endStationName = (TextView) convertView.findViewById(R.id.budgetinfoitem_end_station_name);
            holder.additionalInfoLine1 = (TextView) convertView.findViewById(R.id.budgetinfoitem_info_line1);
            holder.additionalInfoLine2 = (TextView) convertView.findViewById(R.id.budgetinfoitem_info_line2);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.sortCriteria.setText(String.format("%.2f", mItemList.get(position).getCost())+"$");
        holder.startStationName.setText(mItemList.get(position).getStartStationName());
        holder.endStationName.setText(mItemList.get(position).getEndStationName());
        holder.additionalInfoLine1.setText("XX");
        holder.additionalInfoLine2.setText("min");

        return convertView;
    }
}
