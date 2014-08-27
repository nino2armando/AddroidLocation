package com.example.ninokhodabandeh.androidlocation;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import javax.sql.ConnectionEvent;


public class MainActivity extends FragmentActivity implements
        com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener
{

    // request to connect to location services
    private LocationRequest _locationRequest;
    // stores the current instance of the locatipon client in the app
    private LocationClient _locationClient;

    // UI widgets
    private TextView _textViewLatLng;
    private TextView _textViewAddress;
    private ProgressBar _activityIndicatior;
    private TextView _textViewConnectionState;
    private TextView _textViewConnectionStatus;

    // sharedPrefrences
    SharedPreferences _prefs;

    // sharedPreferences editor
    SharedPreferences.Editor _editor;

     /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
     boolean _updatesRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI widgets
        _textViewLatLng = (TextView) findViewById(R.id.lat_lng);
        _textViewAddress = (TextView) findViewById(R.id.address);
        _textViewConnectionState = (TextView) findViewById(R.id.text_connection_state);
        _textViewConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        _activityIndicatior = (ProgressBar) findViewById(R.id.address_progress);

        _locationRequest = LocationRequest.create();

        _locationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        _locationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        _updatesRequested = false;

        _prefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        _editor = _prefs.edit();

        _locationClient = new LocationClient(this, this, this);
    }

    // Activity
     /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */

    @Override
    protected void onStop() {
        if(_locationClient.isConnected()){
            stopPeriodicUpdates();
        }
        _locationClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        // save the current settings for updates
        _editor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, _updatesRequested);
        _editor.commit();

        super.onPause();
    }

    @Override
    protected void onStart() {
        _locationClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {

        if(_prefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)){
            _updatesRequested = _prefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
        }else{
            _editor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            _editor.commit();
        }
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Choose what to do based on the request code
        switch (requestCode){
            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNETION_FAILURE_RESOLUTION_REQUEST:

                switch (resultCode){
                    // If google play service resolved the problem
                    case Activity.RESULT_OK:
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));
                        _textViewConnectionState.setText(R.string.connected);
                        _textViewConnectionStatus.setText(R.string.resolved);
                        break;
                    // if any other results was returned by google play services
                    default:
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));
                        _textViewConnectionState.setText(R.string.disconnected);
                        _textViewConnectionStatus.setText(R.string.no_resolution);
                }
                    // any other request code other than the original one we made
                default:
                    Log.d(LocationUtils.APPTAG, getString(R.string.unknown_activity_request_code, requestCode));
                    break;
        }

    }

    // LocationListener
    @Override
    public void onLocationChanged(Location location) {
        _textViewConnectionStatus.setText(R.string.location_updated);
        _textViewLatLng.setText(LocationUtils.getLatLong(this, location));
    }

    // GooglePlayServicesClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle) {
        _textViewConnectionStatus.setText(getString(R.string.connected));
        if(_updatesRequested){
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDisconnected() {
        _textViewConnectionStatus.setText(R.string.disconnected);
    }

    // GooglePlayServicesClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
         /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if(connectionResult.hasResolution()){
            try{
                // start an activity that tries to resolve that error
                connectionResult.startResolutionForResult(this, LocationUtils.CONNETION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            }catch(IntentSender.SendIntentException ex){
                ex.printStackTrace();
            }
        }else {
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    private void startPeriodicUpdates(){
        try{
            _locationClient.requestLocationUpdates(_locationRequest, this);
        }catch (Exception ex){
            Log.e(LocationUtils.APPTAG ,ex.getMessage());
        }

        _textViewConnectionState.setText(R.string.location_requested);
    }

    private void stopPeriodicUpdates(){
        try {
            _locationClient.removeLocationUpdates(this);
        }catch (Exception ex){
            Log.e(LocationUtils.APPTAG ,ex.getMessage());
        }
            _textViewConnectionStatus.setText(R.string.location_updates_stopped);
    }

    public void getAddress(View v){
        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()){
            Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
            return;
        }

        if(serviceConnected()){
            Location location = _locationClient.getLastLocation();
            _activityIndicatior.setVisibility(View.VISIBLE);

            (new GetAddressTask(this)).execute(location);
        }
    }

    public void getLocation(View v){
        if(serviceConnected()){
            Location location = _locationClient.getLastLocation();
            _textViewLatLng.setText(LocationUtils.getLatLong(this, location));
        }
    }

    public void startUpdates(View v){
        _updatesRequested = true;

        if(serviceConnected()){
            startPeriodicUpdates();
        }
    }

    public void stopUpdates(View v){
        _updatesRequested = false;
        if(serviceConnected()){
            stopPeriodicUpdates();
        }
    }


    @SuppressWarnings("NewApi")
    private boolean serviceConnected(){
        // check google play service is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // available
        if(ConnectionResult.SUCCESS == resultCode){
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));
            return true;
        }else {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if(dialog != null){
                ErrorFragment errorFragment = new ErrorFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }


    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode){
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNETION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorFragment errorFragment = new ErrorFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(), LocationUtils.APPTAG);
        }

    }

    public static class ErrorFragment extends DialogFragment{

        private Dialog _dialog;

        public ErrorFragment(){
            super();
            _dialog = null;
        }

        public void setDialog(Dialog dialog){
            _dialog =  dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return _dialog;
        }
    }

    protected class GetAddressTask extends AsyncTask<Location, Void, String>{

        Context _context;

        public GetAddressTask(Context context){
           super();
           _context = context;
        }

        @Override
        protected String doInBackground(Location... locations) {

            Geocoder geocoder = new Geocoder(_context, Locale.getDefault());
            Location location = locations[0];
            List<Address> addresses = null;

            try{

                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);

            }catch(IOException ex1){

                Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));
                ex1.printStackTrace();
                return (getString(R.string.IO_Exception_getFromLocation));

            }catch(IllegalArgumentException ex2){

                String errorString = getString(R.string.illegal_argument_exception, location.getLatitude(), location.getLongitude());
                Log.e(LocationUtils.APPTAG, errorString);
                ex2.printStackTrace();

                return errorString;
            }

            if(addresses != null && addresses.size() > 0){
                Address address = addresses.get(0);

                String addressText = getString(R.string.address_output_string,
                        // if there is a street address
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",

                        // locality is the city
                        address.getLocality(),
                        address.getCountryName()
                        );

                return addressText;
            }else {
                return getString(R.string.no_address_found);
            }
        }

        @Override
        protected void onPostExecute(String address) {
            _activityIndicatior.setVisibility(View.GONE);
            _textViewAddress.setText(address);
        }
    }
}
