package edu.calvin.equinox.magnumopus;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

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

    /**
     * Change the color of the brush
     *
     * @param view
     *  The View that called this.
     */
    public void chooseColor(View view)
    {
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(Color.BLUE)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                        Log.d("ColorText", "onColorSelected: 0x" + Integer.toHexString(selectedColor));
                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.canvas_view);
                        theCanvas.setColor(selectedColor);
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();

        //Turn off the eraser or panning button if you pick a new color
        TilingCanvasView theCanvas = (TilingCanvasView)findViewById(R.id.canvas_view);

        if(theCanvas.isErasing())
        {
            toggleErase(findViewById(R.id.toggle_erase_btn));
        }

        if (theCanvas.isNavigating())
        {
            toggleNavigation(findViewById(R.id.toggle_nav_btn));
        }
    }
}
