package edu.calvin.equinox.magnumopus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
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
        startActivity(new Intent(this, CanvasActivity.class));
    }
}
