package com.ludoscity.findmybikes;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

/**
 * Created by F8Full on 2016-06-03.
 * extends library provided class for sheet to support 'editable' mode for the whole sheet
 */
public class EditableMaterialSheetFab extends MaterialSheetFab
                        implements View.OnClickListener{

    public interface OnFavoriteSheetEventListener {
        void onFavoriteSheetEditDone();
        void onFavoriteSheetEditCancel();
    }

    private OnFavoriteSheetEventListener mListener;

    private FloatingActionButton mEditFAB;
    private FloatingActionButton mEditDoneFAB;
    private RecyclerView mFavRecyclerview;
    /**
     * Creates a MaterialSheetFab instance and sets up the necessary click listeners.
     *
     * @param view       The FAB view.
     * @param sheet      The sheet view.
     * @param overlay    The overlay view.
     * @param sheetColor The background color of the material sheet.
     * @param fabColor   The background color of the FAB.
     */
    public EditableMaterialSheetFab( View view, View sheet, View overlay, int sheetColor, int fabColor, OnFavoriteSheetEventListener _listener) {
        super(view, sheet, overlay, sheetColor, fabColor);
        mEditFAB = (FloatingActionButton)sheet.findViewById(R.id.favorite_sheet_edit_fab);
        mEditFAB.setOnClickListener(this);

        mEditDoneFAB = (FloatingActionButton)sheet.findViewById(R.id.favorite_sheet_edit_done_fab);
        mEditDoneFAB.setOnClickListener(this);

        mFavRecyclerview = (RecyclerView) sheet.findViewById(R.id.favorites_sheet_recyclerview);

        mListener = _listener;
    }


    public void hideEditFab(){ mEditFAB.hide(); }
    public void showEditFab(){ mEditFAB.show(); }
    public void smoothScrollToTop(){

        if (mFavRecyclerview.getAdapter().getItemCount() > 1)
            mFavRecyclerview.smoothScrollToPosition(0);}

    @Override
    public void hideSheet() {

        if (mEditDoneFAB.getVisibility() == View.VISIBLE){
            ((FavoriteRecyclerViewAdapter)mFavRecyclerview.getAdapter()).setSheetEditing(false);

            mFavRecyclerview.getAdapter().notifyDataSetChanged();

            mEditDoneFAB.hide();
            mEditFAB.show();

            mListener.onFavoriteSheetEditCancel();

        }

        super.hideSheet();
    }

    @Override
    public void onClick(View v) {

        //hide all item edit fabs
        //show all affordance handles
        ((FavoriteRecyclerViewAdapter)mFavRecyclerview.getAdapter()).setSheetEditing(v.getId()==R.id.favorite_sheet_edit_fab);

        mFavRecyclerview.getAdapter().notifyDataSetChanged();

        switch (v.getId()){
            case R.id.favorite_sheet_edit_fab:
                mEditFAB.hide();
                mEditDoneFAB.show();

                break;
            case R.id.favorite_sheet_edit_done_fab:
                mEditDoneFAB.hide();
                mEditFAB.show();

                mListener.onFavoriteSheetEditDone();
                break;
        }
    }
}
