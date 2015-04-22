package dnalves.taxis99.Model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Doug on 4/19/15.
 */
public class Driver {

    public String id;

    public boolean available;

    public double latitude;

    public double longitude;

    public Driver(String id, boolean available, double latitude, double longitude) {
        this.id = id;
        this.available = available;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Driver(JSONObject json) throws JSONException {
        this.id = json.getString("driverId");
        this.available = json.getBoolean("driverAvailable");
        this.latitude = json.getDouble("latitude");
        this.longitude = json.getDouble("longitude");
    }

}
