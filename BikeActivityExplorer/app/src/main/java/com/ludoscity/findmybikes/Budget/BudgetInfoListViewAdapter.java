package com.ludoscity.findmybikes.Budget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by F8Full on 2015-04-07.
 * Custom adapter used in BudgetInfoFragment
 */
public class BudgetInfoListViewAdapter extends BaseAdapter{
    public static final int SORT_CRITERIA_COST = 0;
    public static final int SORT_CRITERIA_DURATION = 1;
    public static final int SORT_CRITERIA_DATE = 2;

    private int mCurrentSortCriteria;
    LayoutInflater mInflater;

    private List<BudgetInfoItem> mItemList = null;

    public BudgetInfoListViewAdapter(Context _context, List<BudgetInfoItem> _itemList){
        mItemList = _itemList;
        mInflater = LayoutInflater.from(_context);
        sortTracksByCostAndNotify(true);
    }

    public void reverseSortOrderAndNotify(){
        Collections.reverse(mItemList);
        notifyDataSetChanged();
    }

    public void sortTracksByCostAndNotify(boolean _sortHighToLow){
        Collections.sort(mItemList, new Comparator<BudgetInfoItem>() {
            //We want them in reverse order
            @Override
            public int compare(BudgetInfoItem lhs, BudgetInfoItem rhs) {
                if (lhs.getCost() == rhs.getCost()) {
                    return 0;
                } else if (lhs.getCost() < rhs.getCost()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        mCurrentSortCriteria = SORT_CRITERIA_COST;

        if (!_sortHighToLow){
            reverseSortOrderAndNotify();
        }
        else{
            notifyDataSetChanged();
        }
    }

    public void sortTracksByDurationAndNotify(boolean _sortHighToLow){
        Collections.sort(mItemList, new Comparator<BudgetInfoItem>() {
            @Override
            public int compare(BudgetInfoItem lhs, BudgetInfoItem rhs) {
                if (lhs.getDurationInMinutes() == rhs.getDurationInMinutes()) {
                    return 0;
                } else if (lhs.getDurationInMinutes() < rhs.getDurationInMinutes()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        mCurrentSortCriteria = SORT_CRITERIA_DURATION;

        if (!_sortHighToLow){
            reverseSortOrderAndNotify();
        }
        else{
            notifyDataSetChanged();
        }
    }

    public void sortTracksByDateAndNotify(boolean _sortHighToLow){
        Collections.sort(mItemList, new Comparator<BudgetInfoItem>() {
            @Override
            public int compare(BudgetInfoItem lhs, BudgetInfoItem rhs) {
                return lhs.getTimestampAsDate().compareTo(rhs.getTimestampAsDate());
            }
        });
        mCurrentSortCriteria = SORT_CRITERIA_DATE;

        if (!_sortHighToLow){
            reverseSortOrderAndNotify();
        }
        else{
            notifyDataSetChanged();
        }
    }

    private class ViewHolder{
        TextView sortCriteriaLine1;
        TextView sortCriteriaLine2;
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
    public long getItemId(int position) { return mItemList.get(position).getIDAsLong(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null){
            holder = new ViewHolder();
            convertView = mInflater.inflate(com.ludoscity.findmybikes.R.layout.budgetinfolist_item, null);
            holder.sortCriteriaLine1 = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_sort_criteria_line1);
            holder.sortCriteriaLine2 = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_sort_criteria_line2);
            holder.startStationName = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_start_station_name);
            holder.endStationName = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_end_station_name);
            holder.additionalInfoLine1 = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_info_line1);
            holder.additionalInfoLine2 = (TextView) convertView.findViewById(com.ludoscity.findmybikes.R.id.budgetinfoitem_info_line2);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.startStationName.setText(mItemList.get(position).getStartStationName());
        holder.endStationName.setText(mItemList.get(position).getEndStationName());

        if (mCurrentSortCriteria == SORT_CRITERIA_COST) {
            holder.sortCriteriaLine1.setText(String.format("%.2f", mItemList.get(position).getCost()) + "$");
            holder.sortCriteriaLine2.setVisibility(View.GONE);
            holder.additionalInfoLine1.setText(String.valueOf(mItemList.get(position).getDurationInMinutes()));
            holder.additionalInfoLine2.setVisibility(View.VISIBLE);
            holder.additionalInfoLine2.setText("min");
        }
        else if (mCurrentSortCriteria == SORT_CRITERIA_DURATION){
            holder.sortCriteriaLine1.setText(String.valueOf(mItemList.get(position).getDurationInMinutes()));
            holder.sortCriteriaLine2.setVisibility(View.VISIBLE);
            holder.sortCriteriaLine2.setText("min");
            holder.additionalInfoLine1.setText(String.format("%.2f", mItemList.get(position).getCost()) + "$");
            holder.additionalInfoLine2.setVisibility(View.GONE);
        }
        else{   //SORT_CRITERIA_DATE
            Calendar cal = Calendar.getInstance();
            cal.setTime(mItemList.get(position).getTimestampAsDate());

            holder.sortCriteriaLine1.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            holder.sortCriteriaLine2.setVisibility(View.VISIBLE);
            holder.sortCriteriaLine2.setText(new SimpleDateFormat("MMM").format(cal.getTime()));
            holder.additionalInfoLine1.setText(String.format("%.2f", mItemList.get(position).getCost()) + "$");
            holder.additionalInfoLine2.setVisibility(View.GONE);
        }

        return convertView;
    }
}
