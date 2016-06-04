package com.ludoscity.findmybikes;

import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.gordonwong.materialsheetfab.MaterialSheetFab;

/**
 * Created by F8Full on 2016-06-03.
 * extends library provided class for sheet to support 'editable' mode for the whole sheet
 */
public class EditableMaterialSheetFab extends MaterialSheetFab{

    private FloatingActionButton mEditFAB;
    /**
     * Creates a MaterialSheetFab instance and sets up the necessary click listeners.
     *
     * @param view       The FAB view.
     * @param sheet      The sheet view.
     * @param overlay    The overlay view.
     * @param sheetColor The background color of the material sheet.
     * @param fabColor   The background color of the FAB.
     */
    public EditableMaterialSheetFab( View view, View sheet, View overlay, int sheetColor, int fabColor) {
        super(view, sheet, overlay, sheetColor, fabColor);
        mEditFAB = (FloatingActionButton)sheet.findViewById(R.id.favorite_sheet_edit_fab);
    }


    public void hideEditFab(){ mEditFAB.hide(); }
    public void showEditFab(){ mEditFAB.show(); }
}
