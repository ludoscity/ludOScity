package com.ludoscity.findmybikes;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by F8Full on 2016-03-31.
 * Adapter for the RecyclerView displaying favorites station in a sheet
 */
public class FavoriteRecyclerViewAdapter extends RecyclerView.Adapter<FavoriteRecyclerViewAdapter.FavoriteListItemViewHolder> {


    private final OnFavoriteListItemClickListener mListener;
    private final Context mCtx;

    private ArrayList<FavoriteItem> mFavoriteList = new ArrayList<>();

    public interface OnFavoriteListItemClickListener {
        void onFavoriteListItemClick(String _path);
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

    public class FavoriteListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mName;

        public FavoriteListItemViewHolder(View itemView) {
            super(itemView);

            mName = (TextView) itemView.findViewById(R.id.favorite_name);

            itemView.setOnClickListener(this);
        }

        public void bindFavorite(FavoriteItem _favorite){
            mName.setText(_favorite.getDisplayName());
        }

        @Override
        public void onClick(View v) {

            switch (v.getId()){
                case R.id.favoritelist_item_root:
                    mListener.onFavoriteListItemClick("clicked");
                    break;
            }
        }
    }
}
