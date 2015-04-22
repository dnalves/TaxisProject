package dnalves.taxis99;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dnalves.taxis99.Model.Driver;

public class MapActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleMap.OnCameraChangeListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;

    private UpdateDriversTask mUpdateDriversTask;

    private VisibleRegion mVisibleRegion;

    private HashMap<String, Driver> mDriversMap;

    private Handler timerHandler;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {

            updateDriverList();

        }
    };

    public long mRefreshTime = 5000;

    private boolean mShouldShowOnlyLastRefresh = false;

    //region Activity LifeCycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        timerHandler = new Handler();
        mDriversMap = new HashMap<>();


        // Create a GoogleAPIClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();

        setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh_5) {
            mRefreshTime = 5000;
            return true;
        } else if (id == R.id.refresh_30) {
            mRefreshTime = 30000;
            return true;
        } else if (id == R.id.refresh_60) {
            mRefreshTime = 60000;
            return true;
        } else if (id == R.id.clear_map_no) {
            mShouldShowOnlyLastRefresh = false;
            return true;
        } else if (id == R.id.clear_map_yes) {
            mShouldShowOnlyLastRefresh = true;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //endregion

    private void updateMapCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    //region Map SetUp

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

    }
    //endregion

    //region ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle) {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        mMap.setOnCameraChangeListener(this);
        if (mLastLocation != null) {
            updateMapCamera(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }
        //scheduleTimerRunnable(0);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    //endregion

    /**
     * Handles the task creation for updating the drivers list. The task won't be created if there
     * is already another task fetching the drivers for the same visible region.
     */
    private void updateDriverList() {

        VisibleRegion currentVisibleRegion = mMap.getProjection().getVisibleRegion();

        if (mShouldShowOnlyLastRefresh) {
            mDriversMap = new HashMap<>();
        }

        if (mVisibleRegion == null || hasVisibleRegionChanged(currentVisibleRegion)) {
            if (mUpdateDriversTask != null) {
                mUpdateDriversTask.cancel(true);
            }
            mVisibleRegion = currentVisibleRegion;
            mUpdateDriversTask = new UpdateDriversTask(this);
            mUpdateDriversTask.execute(mMap.getProjection().getVisibleRegion().latLngBounds);
        } else {
            if (mUpdateDriversTask != null && mUpdateDriversTask.getStatus() == AsyncTask.Status.FINISHED) {
                mUpdateDriversTask = new UpdateDriversTask(this);
                mUpdateDriversTask.execute(mMap.getProjection().getVisibleRegion().latLngBounds);
            }
        }


    }

    //region OnCameraChangeListener

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        updateDriverList();
    }

    //endregion

    /**
     * Clear the markers of the map and add new ones with the correct positions. If a driver is in the
     * hashmap but it is outside the boundaries of the visible area of the map, he will be removed from the
     * hashmap and his marker won't be displayed.
     * @param list
     * @param bounds
     */
    public void clearAndUpdateMapWithDrivers(List<Driver> list, LatLngBounds bounds) {
        LatLng latLng = null;

        mMap.clear();

        Iterator it = mDriversMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            Driver driver = (Driver) pair.getValue();
            latLng = new LatLng(driver.latitude, driver.longitude);
            if (!bounds.contains(latLng)) {
                it.remove();
            } else {
                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_icon))
                        .anchor((float) 0.5, (float) 0.5)
                        .title("DriverId: " + driver.id)
                        .snippet("Available: " + (driver.available ? "Yes" : "No")));
            }

        }
    }


    /**
     * Checks whether the visibleRegion changed.
     * @param visibleRegion
     * @return
     */
    private boolean hasVisibleRegionChanged(VisibleRegion visibleRegion) {
        if (mVisibleRegion != null && visibleRegion.latLngBounds.southwest.latitude == mVisibleRegion.latLngBounds.southwest.latitude &&
                visibleRegion.latLngBounds.southwest.longitude == mVisibleRegion.latLngBounds.southwest.longitude &&
                visibleRegion.latLngBounds.northeast.latitude == mVisibleRegion.latLngBounds.northeast.latitude &&
                visibleRegion.latLngBounds.northeast.longitude == mVisibleRegion.latLngBounds.northeast.longitude) {
            return false;
        }
        return true;
    }

    public void scheduleTimerRunnable(long delayMillis) {
        timerHandler.postDelayed(timerRunnable, delayMillis);
    }

    public void updateDriverInHashMap(Driver driver) {
        mDriversMap.put(driver.id, driver);
    }

}
