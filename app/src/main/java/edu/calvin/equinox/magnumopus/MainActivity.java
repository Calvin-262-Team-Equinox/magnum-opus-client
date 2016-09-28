package edu.calvin.equinox.magnumopus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

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
}
