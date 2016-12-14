package edu.calvin.equinox.magnumopus;

import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * MainActivity()
 * This is the main "entry point" for the app
 */

public class MainActivity extends AppCompatActivity
{
    /**
     * OnCreate()
     * This creates the main activity layout
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);
            new GetSearchCanvas(this).execute("http://cs262.cs.calvin.edu:8085/equinox/search/canvas/" + query);
        }
    }

    /**
     *Retrieves a canvas from the server that has been searched for
     */
    private class GetSearchCanvas extends AsyncTask<String, Void, JSONObject>
    {
        String theUrl ="";
        MainActivity m_Search;

        public GetSearchCanvas(MainActivity search)
        {
            m_Search = search;
        }

        @Override
        protected JSONObject doInBackground(String... params)
        {
            theUrl = params[0];

            URL apiURL;
            try
            {
                apiURL = new URL(theUrl);
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
                return null;
            }

            JSONObject output = null;

            try{
                HttpURLConnection conn = (HttpURLConnection)apiURL.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // 10 sec
                conn.setReadTimeout(10000);    // 10 sec
                conn.setRequestProperty("Content-Type", "application/json; charset=utf8");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);
                    }

                    try
                    {
                        output = new JSONObject(result.toString());
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    Log.e("GetSearchCanvas", "HTTP error " + conn.getResponseCode());
                }
                conn.disconnect();

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return output;
        }

        /**
         *After the search has been executed then try to find the canvas
         * @param canvasData
         */
        @Override
        protected void onPostExecute(JSONObject canvasData)
        {
            Intent intent = new Intent(m_Search, CanvasActivity.class);
            try
            {
                intent.putExtra("EXTRA_CANVAS_ID", canvasData.getInt("ID"));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            m_Search.startActivity(intent);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    /**
     * This shows the menu items, about and help
     * @param item
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;

            case R.id.menu_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Open the canvas.
     *
     * @param view
     *  The View that called this.
     */
    public void openCanvas(View view)
    {
        FragmentManager fm = getSupportFragmentManager();
        SelectCanvasName dialog = new SelectCanvasName(this);
        dialog.show(fm, "fragment_name");
    }

    /**
     * Open the search box for finding a canvas.
     *
     * @param view
     *  The View that called this.
     */
    public void findCanvas(View view)
    {
        onSearchRequested();
    }
}
