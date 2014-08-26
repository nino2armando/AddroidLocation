package com.example.ninokhodabandeh.androidlocation;

import android.content.Context;
import android.location.Location;

/**
 * Created by nino.khodabandeh on 8/25/2014.
 */
public class LocationUtils {
    public static final int CONNETION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final java.lang.String APPTAG = "AndroidLocation";
    public static final String STRING_EMPTY = new String();
    public static final String KEY_UPDATES_REQUESTED = "com.example.ninokhodabandeh.androidlocation.KEY_UPDATES_REQUESTED";
    public static final String SHARED_PREFERENCES = "com.example.ninokhodabandeh.androidlocation.SHARED_PREFERENCES";

    /*
 * Constants for location update parameters
 */
    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;

    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 1;

    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;

    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;

    public static String getLatLong(Context context, Location currentLocation){
        if(currentLocation != null){
            return context.getString(R.string.Lat_long,currentLocation.getLatitude(), currentLocation.getLongitude());
        }else {
            return STRING_EMPTY;
        }
    }
}
