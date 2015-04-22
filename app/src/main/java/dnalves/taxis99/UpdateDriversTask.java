package dnalves.taxis99;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dnalves.taxis99.Model.Driver;

/**
 * Created by Doug on 4/19/15.
 */
public class UpdateDriversTask extends AsyncTask<LatLngBounds, Void, LatLngBounds>{

    private MapActivity mMapActivity = null;

    public UpdateDriversTask(MapActivity activity) {
        this.mMapActivity = activity;
    }

    @Override
    protected LatLngBounds doInBackground(LatLngBounds... params) {
        LatLngBounds bounds = params[0];
        URL url = null;

        HttpURLConnection urlConnection = null;

        try {
            url = new URL("https://api.99taxis.com/lastLocations?sw=" + bounds.southwest.latitude +
                    "," + bounds.southwest.longitude + "&ne=" + bounds.northeast.latitude + "," +
                    bounds.northeast.longitude);

            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String stream = readStream(in);

            JSONArray jsonArray = new JSONArray(stream);

            for (int i = 0; i < jsonArray.length() ; i++) {
                Driver driver = new Driver(jsonArray.getJSONObject(i));
                mMapActivity.updateDriverInHashMap(driver);
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("UpdateDriversTask", "Error while parsing JSON.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("UpdateDriversTask", "Error while reading stream.");
        } finally{
            urlConnection.disconnect();
        }


        return bounds;
    }

    @Override
    protected void onPostExecute(LatLngBounds s) {

        if (!this.isCancelled() && mMapActivity != null) {
            mMapActivity.clearAndUpdateMapWithDrivers(null, s);
            mMapActivity.scheduleTimerRunnable(mMapActivity.mRefreshTime);
        }

        super.onPostExecute(s);
    }


    /**
     * Read the stream and convert it to a String.
     * @param in the InputStream to be read
     * @return a String containig the content of the InputStream
     * @throws IOException
     */
    private String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();
        String auxString = reader.readLine();
        while (auxString != null) {

            builder.append(auxString);

            auxString = reader.readLine();
        }

        return builder.toString();

    }
}
