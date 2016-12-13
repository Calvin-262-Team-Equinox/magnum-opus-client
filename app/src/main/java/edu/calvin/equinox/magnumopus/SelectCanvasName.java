package edu.calvin.equinox.magnumopus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Create a dialog box so the user can select a canvas name
 */
public class SelectCanvasName extends DialogFragment
{
    String name = "";
    MainActivity m_main;

    public SelectCanvasName(MainActivity main){
        m_main = main;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //Create the dialog box
        builder.setMessage(R.string.dialog_text);
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input)

                //When ok is pressed, create a new canvas
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        name = input.getText().toString();
                        new PostCreateCanvas(m_main).execute( "http://153.106.116.82:8085/equinox/create/canvas/" + name );
                    }
                })
                //When cancel is pressed, cancel the dialog box
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    private class PostCreateCanvas extends AsyncTask<String, Void, JSONObject>
    {
        String theUrl = "";
        MainActivity m_dialog;

        public PostCreateCanvas(MainActivity dialog)
        {
            m_dialog = dialog;
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

            try
            {
                HttpURLConnection conn = (HttpURLConnection)apiURL.openConnection();
                conn.setRequestMethod("POST");
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
                    Log.e("PostTileUpdateTask", "HTTP error " + conn.getResponseCode());
                }
                conn.disconnect();

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return output;
        }

        @Override
        protected void onPostExecute(JSONObject canvasData)
        {
            Intent intent = new Intent(m_dialog, CanvasActivity.class);
            try
            {
                intent.putExtra("EXTRA_CANVAS_ID", canvasData.getInt("key"));
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            m_dialog.startActivity(intent);
        }

    }
}
