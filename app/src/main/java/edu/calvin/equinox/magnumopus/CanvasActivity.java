package edu.calvin.equinox.magnumopus;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;

/**
 * Activity for drawing on the canvas.
 */

public class CanvasActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{
    private Spinner m_brushSpinner;
    private String m_brushType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canvas);

        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        m_brushSpinner = (Spinner) findViewById(R.id.brush_spinner);
        m_brushType = (String) m_brushSpinner.getSelectedItem();
        m_brushSpinner.setOnItemSelectedListener(this);
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
        m_brushType = (String) m_brushSpinner.getSelectedItem();
        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.canvas_view);
        theCanvas.setBrush(m_brushType);

        if (theCanvas.isErasing())
        {
            toggleErase(findViewById(R.id.toggle_erase_btn));
        }
        else if (theCanvas.isNavigating())
        {
            toggleNavigation(findViewById(R.id.toggle_nav_btn));
        }
    }

    /**
     * If nothing is selected, do nothing
     * @param adapterView
     */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

    }

    /**
     * Switch between navigation and painting modes.
     *
     * @param view
     *  The View that called this.
     */
    public void toggleNavigation(View view)
    {
        ImageButton btn = (ImageButton)view;
        if (btn == null)
        {
            return;
        }

        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.canvas_view);
        if (theCanvas.toggleNavigating())
        {
            btn.setColorFilter(getResources().getColor(R.color.bright_foreground_material_light));

            if (theCanvas.isErasing())
            {
                toggleErase(findViewById(R.id.toggle_erase_btn));
            }
        }
        else
        {
            btn.setColorFilter(getResources().getColor(R.color.dim_foreground_disabled_material_dark));
        }
    }

    /**
     * Switch between erasing and painting modes.
     *
     * @param view
     *  The View that called this.
     */
    public void toggleErase(View view)
    {
        ImageButton btn = (ImageButton)view;
        if (btn == null)
        {
            return;
        }

        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.canvas_view);
        if (theCanvas.toggleErasing())
        {
            theCanvas.setBrush( "Eraser" );
            btn.setColorFilter(getResources().getColor(R.color.bright_foreground_material_light));

            if (theCanvas.isNavigating())
            {
                toggleNavigation(findViewById(R.id.toggle_nav_btn));
            }
        }
        else
        {
            theCanvas.setBrush( m_brushType );
            btn.setColorFilter(getResources().getColor(R.color.dim_foreground_disabled_material_dark));
        }
    }
}
