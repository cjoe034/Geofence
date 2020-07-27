package com.test.geoexercise;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends Activity {

    private static final long POINT_RADIUS = 1000; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1; // never expire

    private static final String PROX_ALERT_INTENT = "com.test.geoexercise.MainActivity";

    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 1000; // in Milliseconds
    private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // in Meters

    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";

    private LocationManager locationManager;

    private EditText latitudeEditText;
    private EditText longitudeEditText;

    private Button getLastCoordinatesButton;
    private Button saveCoordinatesButton;

    private static final NumberFormat nf = new DecimalFormat("##.########");

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
              MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE,
              new MyLocationListener());

        latitudeEditText = (EditText) findViewById(R.id.point_latitude);
        longitudeEditText = (EditText) findViewById(R.id.point_longitude);
        getLastCoordinatesButton = (Button) findViewById(R.id.get_coordinates_button);
        saveCoordinatesButton = (Button) findViewById(R.id.save_coordinates_button);

        getLastCoordinatesButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                populateCoordinatesFromLastKnownLocation();
            }
        });

        saveCoordinatesButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                saveProximityAlertPoint();
            }
        });
    }

    private void saveProximityAlertPoint() {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location == null) {
            Toast.makeText(this, "No last known location. Aborting...", Toast.LENGTH_LONG).show();
            return;
        }

        saveCoordinatesInPreferences((float) location.getLatitude(),
              (float) location.getLongitude());

        addProximityAlert((double) location.getLatitude(), (double) location.getLongitude());
    }

    private void addProximityAlert(double latitude, double longitude) {
        Intent intent = new Intent(PROX_ALERT_INTENT);
        PendingIntent proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        locationManager.addProximityAlert(
              latitude, // the latitude of the central point of the alert region
              longitude, // the longitude of the central point of the alert region
              POINT_RADIUS, // the radius of the central point of the alert region, in meters
              PROX_ALERT_EXPIRATION, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
              proximityIntent // used to generate an Intent to fire when entry to or exit from the alert region is detected
        );

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        registerReceiver(new ProximityLoctionRecevier(), filter);
        Toast.makeText(getApplicationContext(), "Alert Added", Toast.LENGTH_SHORT).show();
    }

    private void populateCoordinatesFromLastKnownLocation() {
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            latitudeEditText.setText(nf.format(location.getLatitude()));
            longitudeEditText.setText(nf.format(location.getLongitude()));
        }
    }

    private void saveCoordinatesInPreferences(float latitude, float longitude) {
        SharedPreferences prefs =
              this.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);

        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putFloat(POINT_LATITUDE_KEY, latitude);
        prefsEditor.putFloat(POINT_LONGITUDE_KEY, longitude);
        prefsEditor.commit();
    }

    private Location retrievelocationFromPreferences() {
        SharedPreferences prefs =
              this.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);

        Location location = new Location("POINT_LOCATION");
        location.setLatitude(prefs.getFloat(POINT_LATITUDE_KEY, 0));
        location.setLongitude(prefs.getFloat(POINT_LONGITUDE_KEY, 0));

        return location;
    }

    private class MyLocationListener implements LocationListener {

        @Override public void onLocationChanged(Location location) {
            Location pointLocation = retrievelocationFromPreferences();
            float distance = location.distanceTo(pointLocation);
            Toast.makeText(MainActivity.this, "Distance from Point of Interest:" + distance,
                  Toast.LENGTH_LONG).show();
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override public void onProviderEnabled(String provider) {

        }

        @Override public void onProviderDisabled(String provider) {

        }
    }
}
