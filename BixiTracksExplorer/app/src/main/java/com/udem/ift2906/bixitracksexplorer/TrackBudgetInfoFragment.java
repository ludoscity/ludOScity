package com.udem.ift2906.bixitracksexplorer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.udem.ift2906.bixitracksexplorer.dummy.DummyContent;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class TrackBudgetInfoFragment extends ListFragment {

    private static final String ARG_TIMEPERIOD = "timePeriod";
    private static final String ARG_INFOTYPE = "infoType";

    private String mInfoType;
    private String mTimePeriod;

    private boolean mSortOrderHighToLow = true;
    private int mSortCriteria = 0;  //0 : cost, 1: duration, 2: date

    private OnFragmentInteractionListener mListener;

    public static TrackBudgetInfoFragment newInstance(String infoType, String timePeriod) {
        TrackBudgetInfoFragment fragment = new TrackBudgetInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INFOTYPE, infoType);
        args.putString(ARG_TIMEPERIOD, timePeriod);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public TrackBudgetInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mInfoType = getArguments().getString(ARG_INFOTYPE);
            mTimePeriod = getArguments().getString(ARG_TIMEPERIOD);
        }

        // TODO: Change Adapter to display your content
        setListAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, DummyContent.ITEMS));


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up affordance/carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
            case R.id.budgetInfoSortOrder:
                if(mSortOrderHighToLow){
                    item.setIcon(R.drawable.ic_action_sort_low_to_high);
                }
                else{
                    item.setIcon(R.drawable.ic_action_sort_high_to_low);
                }
                mSortOrderHighToLow = !mSortOrderHighToLow;
                return true;
            case R.id.budgetInfoSortCriteria:
                if (mSortCriteria == 0){
                    item.setIcon(R.drawable.ic_action_duration);
                    mSortCriteria = 1;
                }
                else if (mSortCriteria == 1){
                    item.setIcon(R.drawable.ic_action_date);
                    mSortCriteria = 2;
                }
                else if (mSortCriteria == 2){
                    item.setIcon(R.drawable.ic_action_cost);
                    mSortCriteria = 0;
                }
                return true;
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_budget_info, menu);

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        // update the actionbar to show the up carat/affordance
        ((ActionBarActivity)activity).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onTrackBudgetInfoFragmentInteraction(DummyContent.ITEMS.get(position).id);
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
        // TODO: Update argument type and name
        public void onTrackBudgetInfoFragmentInteraction(String id);
    }

}
