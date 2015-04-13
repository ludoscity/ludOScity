package com.udem.ift2906.bixitracksexplorer;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.widget.ArrayAdapter;

public class FavoritesFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    public static FavoritesFragment newInstance(int sectionNumber) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        fragment.setHasOptionsMenu(true);
        return fragment;
    }


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



