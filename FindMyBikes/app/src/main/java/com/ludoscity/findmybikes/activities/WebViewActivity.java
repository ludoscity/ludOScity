package com.ludoscity.findmybikes.activities;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;

import com.ludoscity.findmybikes.R;

public class WebViewActivity extends AppCompatActivity {

    public final static String EXTRA_URL = "webviewactivity.URL";
    public final static String EXTRA_ACTIONBAR_SUBTITLE = "webviewactivity.SUBTITLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_main));

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        String url = intent.getStringExtra(EXTRA_URL);

        WebView webview = (WebView) findViewById(R.id.webview);

        webview.loadUrl(url);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Intent intent = getIntent();

        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(intent.getStringExtra(EXTRA_ACTIONBAR_SUBTITLE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}