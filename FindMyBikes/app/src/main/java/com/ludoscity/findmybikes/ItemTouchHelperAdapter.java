package com.ludoscity.findmybikes;

/**
 * Created by F8Full on 2016-03-31.
 * Adapter for the RecyclerView displaying favorites station in a sheet
 * Also allows edition
 * from - https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.4okwgvgtx
 */

public interface ItemTouchHelperAdapter {

    boolean onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
