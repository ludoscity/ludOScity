package com.udem.ift2906.bixitracksexplorer;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.app.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.ArrayAdapter;

public class FavoritesFragment extends Fragment  {
    private Context mContext;
    private OnFragmentInteractionListener mListener;

    private static final String ARG_SECTION_NUMBER = "section_number";

    public interface OnFragmentInteractionListener {
        public void onFavoritesFragmentInteraction();
    }

    public static FavoritesFragment newInstance(int sectionNumber) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        try {
            mListener = (OnFragmentInteractionListener) activity;
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ArrayAdapter<String> mFavoritesAdapter;


          //fake data pour afficher les favoris
           String[] data = {
                "Mon ",
                "Tue ",
                "Wed ",
                "Thurs",
                "Fri",
                "Sat",
                "Sun"
        };


        List<String> favoritesTab = new ArrayList<String>(Arrays.asList(data));
        mFavoritesAdapter =
                                new ArrayAdapter<String>(
                                        getActivity(),
                                        R.layout.list_item_favoris,
                                        R.id.list_item_favoris_textview,
                                        favoritesTab);

       View favoritesView = inflater.inflate(R.layout.fragment_favoris, container, false);

        return favoritesView;



}
}



