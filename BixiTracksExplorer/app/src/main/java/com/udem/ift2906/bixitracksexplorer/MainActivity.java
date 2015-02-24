package com.udem.ift2906.bixitracksexplorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState) ;
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        //final static String ARG_POSITION = "position";
        //int mCurrentPosition = -1;

        ExpandableListView mExpListView;
        //ExpandableListAdapter mListAdapter;



        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {


            //Expan
            return inflater.inflate(R.layout.fragment_main, container, false);
        }

        @Override
        public void onStart() {
            super.onStart();

//            // During startup, check if there are arguments passed to the fragment.
//            // onStart is a good place to do this because the layout has already been
//            // applied to the fragment at this point so we can safely call the method
//            // below that sets the article text.
//            Bundle args = getArguments();
//            if (args != null) {
//                // Set article based on argument passed in
//                updateArticleView(args.getInt(ARG_POSITION));
//            } else if (mCurrentPosition != -1) {
//                // Set article based on saved instance state defined during onCreateView
//                updateArticleView(mCurrentPosition);
//            }

            // get the listview
            mExpListView = (ExpandableListView) getActivity().findViewById(R.id.lvExp);

            //I should have a progress bar in my group / item layouts and activate them
            //OR have a completely separated loading fragment
            // preparing list data
            prepareListData();


        }

        //public void updateArticleView(int position) {
            //TextView article = (TextView) getActivity().findViewById(R.id.article);
            //article.setText(Ipsum.Articles[position]);
            //mCurrentPosition = position;
        //}

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            // Save the current article selection in case we need to recreate the fragment
            //outState.putInt(ARG_POSITION, mCurrentPosition);
        }

        private void prepareListData()
        {

            //start ASynchTask that retrieves data over the web
            new RetrieveTrackListTask(getActivity()).execute(mExpListView);
        }
    }


}
