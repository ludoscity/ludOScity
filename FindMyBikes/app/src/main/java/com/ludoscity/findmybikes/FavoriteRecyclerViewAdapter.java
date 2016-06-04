package com.ludoscity.findmybikes;

import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by F8Full on 2016-03-31.
 * Adapter for the RecyclerView displaying favorites station in a sheet
 * Also allows edition
 * 2016-06-03 partially from - https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf#.4okwgvgtx
 */
public class FavoriteRecyclerViewAdapter extends RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder>
                                        implements ItemTouchHelperAdapter {


    private final OnFavoriteListItemClickListener mListener;
    private final Context mCtx;

    private ArrayList<FavoriteItem> mFavoriteList = new ArrayList<>();

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mFavoriteList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mFavoriteList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;

    }

    @Override
    public void onItemDismiss(int position) {

    }

    public interface OnFavoriteListItemClickListener {
        void onFavoriteListItemClick(String _stationId);
        void onFavoristeListItemEditDone(String _stationId, String _newName );

        void onFavoristeListItemEditBegin();

        void onFavoristeListItemEditAbort();
    }

    public FavoriteRecyclerViewAdapter(OnFavoriteListItemClickListener _listener, Context _ctx){
        super();
        mListener = _listener;
        mCtx = _ctx;
    }

    public void setupFavoriteList(ArrayList<FavoriteItem> _toSet){
        mFavoriteList.clear();
        mFavoriteList.addAll(_toSet);

        notifyDataSetChanged();
    }


    @Override
    public FavoriteListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favoritelist_item, parent, false);
        return new FavoriteListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FavoriteListItemViewHolder holder, int position) {
        holder.bindFavorite(mFavoriteList.get(position));

    }

    @Override
    public int getItemCount() {
        return mFavoriteList.size();
    }

    public class FavoriteListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnFocusChangeListener {

        TextView mName;
        String mStationId;
        FloatingActionButton mEditFab;
        FloatingActionButton mDoneFab;

        boolean mEditing = false;
        String mNameBuffer;

        public String getStationId(){ return mStationId; }

        public FavoriteListItemViewHolder(View itemView) {
            super(itemView);

            mName = (TextView) itemView.findViewById(R.id.favorite_name);
            mEditFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_name_edit_fab);
            mDoneFab = (FloatingActionButton) itemView.findViewById(R.id.favorite_name_done_fab);

            mName.setOnClickListener(this);
            mEditFab.setOnClickListener(this);
            mDoneFab.setOnClickListener(this);
        }

        public void bindFavorite(FavoriteItem _favorite){

            if (_favorite.isDisplayNameDefault())
                mName.setTypeface(null, Typeface.ITALIC);
            else
                mName.setTypeface(null, Typeface.BOLD);

            mName.setText(_favorite.getDisplayName());
            mStationId = _favorite.getStationId();
        }

        @Override
        public void onClick(View v) {

            switch (v.getId()){
                case R.id.favorite_name:
                    if (!mEditing)
                        mListener.onFavoriteListItemClick(mStationId);
                    else //User pressed back to hide keyboard
                        showSoftInput();
                    break;

                case R.id.favorite_name_edit_fab:
                    mEditing = true;
                    setupEditMode(true);
                    mListener.onFavoristeListItemEditBegin();
                    break;

                case R.id.favorite_name_done_fab:
                    mEditing = false;
                    setupEditMode(false);
                    mListener.onFavoristeListItemEditDone(mStationId, mName.getText().toString());
                    break;
            }
        }

        private void setupEditMode(boolean _editing) {
            if (_editing)
            {
                mEditFab.hide();
                mDoneFab.show();

                mName.setCursorVisible(true);
                mName.setOnFocusChangeListener(this);
                mName.setTextIsSelectable(true);
                mName.setFocusableInTouchMode(true);
                mName.requestFocus();

                //API level 21+
                //mName.setShowSoftInputOnFocus(true);

                showSoftInput();
            }
            else {
                hideSoftInput();

                mName.setCursorVisible(false);
                mName.setOnFocusChangeListener(null);
                mName.setTextIsSelectable(false);
                mName.setFocusableInTouchMode(false);

                mName.setText(mName.getText().toString().trim());

                mDoneFab.hide();
                mEditFab.show();
            }
        }

        private void showSoftInput() {
            InputMethodManager imm = (InputMethodManager) mCtx.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mName, InputMethodManager.SHOW_FORCED);
        }

        private void hideSoftInput() {
            InputMethodManager imm = (InputMethodManager) mCtx.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mName.getWindowToken(), 0);
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {

            TextView vTV = (TextView)v;

            if (hasFocus){

                mNameBuffer = vTV.getText().toString();

            } else {

                if (mEditing) {
                    //Editing mode wasn't left from clicking done fab, restoring original name
                    vTV.setText(mNameBuffer);

                    mEditing = false;
                    setupEditMode(false);
                    mListener.onFavoristeListItemEditAbort();
                }
            }
        }
    }
}
