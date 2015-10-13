package com.ludoscity.findmybikes.Budget;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.QueryRow;
import com.ludoscity.findmybikes.BixiTracksExplorerAPIHelper;
import com.ludoscity.findmybikes.Helpers.DBHelper;
import com.ludoscity.findmybikes.R;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.ListTracksResponse;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.Track;
import com.udem.ift2906.bixitracksexplorer.backend.bixiTracksExplorerAPI.model.TrackCollection;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public static final String BUDGETOVERVIEW_INFO_CLICK_PATH = "budget_info_click";
    public static final String BUDGETOVERVIEW_INFO_CLICK_TYPE_PARAM = "budget_info_click_type";
    public static final String BUDGETOVERVIEW_INFO_CLICK_TIMEPERIOD_PARAM = "budget_info_click_timeperiod";

    private OnFragmentInteractionListener mListener;

    private TextView mAccessCostValueTextView;
    private TextView mUseCostValueTextView;
    private TextView mTotalCostValueTextView;
    private RelativeLayout mLoadingInterfaceLayout;
    private LinearLayout mInterfaceLayout;

    private TextView mLoadingProgressTextView;

    private ImageButton mAccessCostInfoButton;
    private ImageButton mUseCostInfoButton;

    private String mSelectedPeriod;

    private static float mSeasonAccessCost;
    private static float mSeasonUseCost;

    private static float mMonthAccessCost;
    private static float mMonthUseCost;

    private static boolean mCostCalculated = false;

    private static boolean mDataLoaded = false;

    private ArrayList<BudgetInfoItem> mBudgetInfoItems = new ArrayList<>();

    private RetrieveTrackDataAndProcessCostTask mWebLoadingTask = null;
    private ProcessCostTask mProcessCostTask = null;

    //Bixi tarification grid
    private static final long m45minInms = 2700000;
    private static final long m15minInms = 900000;
    private static final float m46to60minAddedCost = 1.50f;
    private static final long m30minInms = 1800000;
    private static final float m61to90minCost = 3.5f;
    private static final float mEach30minAfter90 = 7.f;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        //setRetainInstance(true);
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

        mLoadingInterfaceLayout = (RelativeLayout) inflatedView.findViewById(R.id.budgetoverview_loading_interface);
        mInterfaceLayout = (LinearLayout) inflatedView.findViewById(R.id.budgetoverview_interface);

        mLoadingProgressTextView = (TextView)inflatedView.findViewById(R.id.budgetoverview_progressbar_label);

        setupInterface();

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
            //((MainActivity) activity).onSectionAttached(
              //      getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }


        try {
            mDataLoaded = DBHelper.gotTracks();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        if (!mDataLoaded) {

            mWebLoadingTask = new RetrieveTrackDataAndProcessCostTask();
            mWebLoadingTask.execute();
        }
        else{
            if (!mCostCalculated) {
                mProcessCostTask = new ProcessCostTask();
                mProcessCostTask.execute();
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mWebLoadingTask != null && !mWebLoadingTask.isCancelled())
        {
            mWebLoadingTask.cancel(false);
            mWebLoadingTask = null;
        }
        if(mProcessCostTask != null && !mProcessCostTask.isCancelled())
        {
            mProcessCostTask.cancel(false);
            mProcessCostTask = null;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        setupInterface();

        //Happens when user presses back from TrackBudgetInfoFragment
        //let's tell our parent activity to update the action bar
        if (mListener != null){
            Uri.Builder builder = new Uri.Builder();

            builder.appendPath("budget_overview_onresume");

            mListener.onBudgetOverviewFragmentInteraction(builder.build(), null);
        }
    }

    private void onInfoClick(String infoType)
    {
        //String retrieved from strings resource must be stripped of the " :" at the end
        infoType = infoType.substring(0, infoType.length()-" :".length());
        if (mListener != null){
            Uri.Builder builder = new Uri.Builder();

            builder.appendPath(BUDGETOVERVIEW_INFO_CLICK_PATH)
                    .appendQueryParameter(BUDGETOVERVIEW_INFO_CLICK_TYPE_PARAM, infoType)
                    .appendQueryParameter(BUDGETOVERVIEW_INFO_CLICK_TIMEPERIOD_PARAM, mSelectedPeriod);

            mListener.onBudgetOverviewFragmentInteraction(builder.build(), mBudgetInfoItems);
        }
    }

    private void updateCost()
    {
        if(mSelectedPeriod.equalsIgnoreCase(getString(R.string.timespan_season)))
        {
            mAccessCostValueTextView.setText(String.valueOf(mSeasonAccessCost) + "$");
            mUseCostValueTextView.setText(String.valueOf(mSeasonUseCost) + "$");
            mTotalCostValueTextView.setText(String.valueOf(mSeasonAccessCost + mSeasonUseCost) + "$");

        }
        else if (mSelectedPeriod.equalsIgnoreCase(getString(R.string.timespan_this_month)))
        {
            mAccessCostValueTextView.setText(String.valueOf(mMonthAccessCost) + "$");
            mUseCostValueTextView.setText(String.valueOf(mMonthUseCost) + "$");
            mTotalCostValueTextView.setText(String.valueOf(mMonthAccessCost + mMonthUseCost) + "$");
        }
    }

    private void setupInterface(){
        if (mDataLoaded && mCostCalculated) {
            mLoadingInterfaceLayout.setVisibility(View.GONE);
            mInterfaceLayout.setVisibility(View.VISIBLE);
            mUseCostInfoButton.setEnabled(true);
        }
        else
        {
            mLoadingInterfaceLayout.setVisibility(View.VISIBLE);
            mInterfaceLayout.setVisibility(View.INVISIBLE);
            mUseCostInfoButton.setEnabled(false);
            mAccessCostInfoButton.setEnabled(false);
        }
    }

    private void processCost() throws CouchbaseLiteException {

        mSeasonAccessCost = mSeasonUseCost = 0.f;
        mMonthAccessCost = mMonthUseCost = 0.f;

        int trackCount = 0;
        List<QueryRow> allTracks = DBHelper.getAllTracks();

        for (QueryRow qr : allTracks)
        {
            Document d = qr.getDocument();

            Map<String, Object> properties = d.getProperties();

            //Guaranteed to be present
            long trackDuration = Long.parseLong((String) properties.get("duration"));

            double trackCost;

            if (properties.get("cost") == null) {
                trackCost = processCostForDuration(trackDuration, 0.f, 0);
                //store the cost in the track document for later use
                DBHelper.putNewTrackPropertyAndSave((String)d.getProperty("key_TimeUTC"), "cost", trackCost );
            }
            else
                trackCost = (Double)properties.get("cost");


            mBudgetInfoItems.add(new BudgetInfoItem((String) properties.get("key_TimeUTC"),
                    (float)trackCost, (String) properties.get("startStationName"), (String) properties.get("endStationName"),
                    trackDuration));

            mSeasonUseCost += trackCost;

            ++trackCount;

            String progress = getString(R.string.budgetoverview_loading_costs) + trackCount + "/" + allTracks.size();

            if (mProcessCostTask != null){
                mProcessCostTask.publish(progress);
            }
            if (mWebLoadingTask != null){
                mWebLoadingTask.publish(progress);
            }
        }

        mCostCalculated = true;
    }

    private float processCostForDuration(long remainingTime, float accumulatedCost, int costStep) {
        if (remainingTime < 0.f)
            return accumulatedCost;

        switch (costStep){
            case 0: //free first 45 minutes
                remainingTime -= m45minInms;
                return processCostForDuration(remainingTime, accumulatedCost, ++costStep);
            case 1: //46 to 60 minutes
                accumulatedCost += m46to60minAddedCost;
                remainingTime -= m15minInms;
                return processCostForDuration(remainingTime, accumulatedCost, ++costStep);
            case 2: //61 to 90 minutes
                accumulatedCost += m61to90minCost;
                remainingTime -= m30minInms;
                return processCostForDuration(remainingTime, accumulatedCost, ++costStep);
            default:    //each subsequent half hour
                accumulatedCost += mEach30minAfter90;
                remainingTime -= m30minInms;
                return processCostForDuration(remainingTime, accumulatedCost, ++costStep);
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
        void onBudgetOverviewFragmentInteraction(Uri uri, ArrayList<BudgetInfoItem> _budgetInfoItemList);
    }

    public class RetrieveTrackDataAndProcessCostTask extends AsyncTask<Void, String, Void> {

        public void publish(String progress){
            publishProgress(progress);
        }

        @Override
        protected Void doInBackground(Void... params) {

            TrackCollection collection = new TrackCollection();
            collection.setItems(new ArrayList<Track>());
            final ListTracksResponse listTracksResponse = BixiTracksExplorerAPIHelper.listTrack();

            //test of good reception of meta data
            //JSONObject responseMeta = new JSONObject(listTracksResponse.getMeta());
            //String license = responseMeta.getString("license");


            if (listTracksResponse.getTrackList() != null) {
                collection.setItems(listTracksResponse.getTrackList());
            }

            List<Track> trackList = collection.getItems();
            int trackCounter = 0;

            for (Track t : trackList)
            {

                try {
                    DBHelper.saveTrack(t);
                } catch (CouchbaseLiteException | JSONException e) {
                    e.printStackTrace();
                }

                ++trackCounter;

                String progress = getString(R.string.budgetoverview_loading_tracks) + trackCounter +"/" + trackList.size();
                publishProgress(progress);
            }

            try {
                processCost();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            mLoadingProgressTextView.setText(progress[0]);

        }



        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);
            mDataLoaded = true;

            //Task is cancelled if fragment is detached

        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            updateCost();

            mDataLoaded = true;

            setupInterface();
        }
    }

    public class ProcessCostTask extends AsyncTask<Void, String, Void> {

        public void publish(String progress){
            publishProgress(progress);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                processCost();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            mLoadingProgressTextView.setText(progress[0]);

        }

        @Override
        protected void onCancelled (Void aVoid){
            super.onCancelled(aVoid);

            //Task is cancelled if fragment is detached

        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            updateCost();
            setupInterface();
        }
    }
}
