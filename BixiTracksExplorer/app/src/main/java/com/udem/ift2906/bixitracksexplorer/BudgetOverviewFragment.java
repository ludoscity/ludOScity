package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

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

    private OnFragmentInteractionListener mListener;

    private TextView mAccessCostValueTextView;
    private TextView mUseCostValueTextView;
    private TextView mTotalCostValueTextView;

    private ImageButton mAccessCostInfoButton;
    private ImageButton mUseCostInfoButton;

    private String mSelectedPeriod;


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_budget_overview, container, false);

        mAccessCostValueTextView = (TextView) inflatedView.findViewById(R.id.accessCostValue);
        mUseCostValueTextView = (TextView) inflatedView.findViewById(R.id.useCostValue);
        mTotalCostValueTextView = (TextView) inflatedView.findViewById(R.id.totalCostValue);

        mAccessCostInfoButton = (ImageButton) inflatedView.findViewById(R.id.accessCostInfoButton);
        mAccessCostInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfoClick(getString(R.string.access_cost_label));
            }
        });


        mUseCostInfoButton = (ImageButton) inflatedView.findViewById(R.id.useCostInfoButton);
        mUseCostInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onInfoClick(getString(R.string.use_cost_label));
            }
        });

        return inflatedView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
         menuInflater.inflate(R.menu.menu_budget_overview, menu);

         Spinner spinner = (Spinner)menu.findItem(R.id.budgetOverviewSpinner).getActionView();
         spinner.setAdapter(ArrayAdapter.createFromResource(getActivity(), R.array.timeperiod, android.R.layout.simple_spinner_dropdown_item));
         spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                 mSelectedPeriod = parent.getItemAtPosition(position).toString();
                 updateCost();
             }

             @Override
             public void onNothingSelected(AdapterView<?> parent) {

             }
         });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Happens when user presses back from TrackBudgetInfoFragment
        //let's tell our parent activity to update the action bar
        if (mListener != null){
            Uri.Builder builder = new Uri.Builder();

            builder.appendPath("budget_overview_onresume");

            mListener.onBudgetOverviewFragmentInteraction(builder.build());
        }
    }

    private void onInfoClick(String infoType)
    {
        if (mListener != null){
            Uri.Builder builder = new Uri.Builder();

            builder.appendPath("budget_info")
                    .appendQueryParameter("info_type", infoType)
                    .appendQueryParameter("selected_period", mSelectedPeriod);

            mListener.onBudgetOverviewFragmentInteraction(builder.build());
        }
    }

    private void updateCost()
    {
        if(mSelectedPeriod.equalsIgnoreCase(getString(R.string.timespan_season)))
        {
            mAccessCostValueTextView.setText("XX,X$");
            mUseCostValueTextView.setText("XX,X$");
            mTotalCostValueTextView.setText("XX,X$");

        }
        else if (mSelectedPeriod.equalsIgnoreCase(getString(R.string.timespan_this_month)))
        {
            mAccessCostValueTextView.setText("YY,Y$");
            mUseCostValueTextView.setText("YY,Y$");
            mTotalCostValueTextView.setText("YY,Y$");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onBudgetOverviewFragmentInteraction(Uri uri);
    }
}
