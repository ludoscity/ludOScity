package com.udem.ift2906.bixitracksexplorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Created by F8Full on 2015-03-27.
 * This file is part of BixiTracksExplorer
 */
public class BudgetOverviewFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BudgetOverviewFragment newInstance(int sectionNumber) {
        BudgetOverviewFragment fragment = new BudgetOverviewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

     @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
         menuInflater.inflate(R.menu.menu_budget_overview, menu);
    }
}
