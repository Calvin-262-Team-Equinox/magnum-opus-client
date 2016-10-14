package edu.calvin.equinox.magnumopus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

/**
 * MainActivity()
 * This is the main "entry point" for the app
 */

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private Spinner brushSpinner;
    public String brushType;

    /**
     * OnCreate()
     * This creates the main activity layout
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        brushSpinner = (Spinner) findViewById(R.id.brushSpinner);
        brushType = (String) brushSpinner.getSelectedItem();
        brushSpinner.setOnItemSelectedListener(this);
    }

    /**
     * Set the canvas's brush to the specified brush type when an item is selected
     *
     * @param adapterView
     * @param view
     * @param i
     * @param l
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
    {
        brushType = (String) brushSpinner.getSelectedItem();
        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.view);
        theCanvas.setBrush(brushType);

    }

    /**
     * If nothing is selected, do nothing
     * @param adapterView
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

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
}
