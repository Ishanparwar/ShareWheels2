package com.project.sharewheels;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Objects;

public class Rider_map_activity extends AppCompatActivity implements LocationListener {

    final static String[] PERMISSIONS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    final static int PERMISSIONS_ALL = 1;
    LocationManager locationManager;
    MapView map;
    Marker mMarker;

    private Button mLogout ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //requested permission.
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // map api
        map = (MapView) findViewById(R.id.map);
        map.getTileProvider().clearTileCache();
        Configuration.getInstance().setCacheMapTileCount((short) 12);
        Configuration.getInstance().setCacheMapTileOvershoot((short) 12);
        // Create a custom tile source
        map.setTileSource(new OnlineTileSourceBase("", 1, 20, 512, ".png",
                new String[]{"https://a.tile.openstreetmap.org/"}) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl()
                        + MapTileIndex.getZoom(pMapTileIndex)
                        + "/" + MapTileIndex.getX(pMapTileIndex)
                        + "/" + MapTileIndex.getY(pMapTileIndex)
                        + mImageFilenameEnding;
            }
        });

        map.setMultiTouchControls(true);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(PERMISSIONS, PERMISSIONS_ALL);
            }else {
                requestLocation();
            }
        }

        mLogout = (Button) findViewById(R.id.logout);
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(Rider_map_activity.this ,MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

    }

    public void createMarker(Location location) {
        if (map == null) {
            return;
        }

        IMapController mapController = map.getController();
        GeoPoint startPoint;
        startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.setZoom(16.5);
        mapController.setCenter(startPoint);
        map.invalidate();


        mMarker = new Marker(map);
        mMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
        mMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mMarker.setTitle("Give it a title");
        mMarker.setPanToView(true);
        map.getOverlays().add(mMarker);
        map.invalidate();
    }


    /*public void createMarker(Location location){
        if(map == null) {
            return;
        }

        Marker my_marker = new Marker(map);
        my_marker.setPosition(new GeoPoint(location.getLatitude(),location.getLongitude()));
        my_marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        my_marker.setTitle("Give it a title");
        my_marker.setPanToView(true);
        map.getOverlays().add(my_marker);
        map.invalidate();
    }*/

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("myLog", "Got Location" + location.getLatitude() + "," + location.getLongitude());
        //Toast.makeText(this, "Got Location " + location.getLatitude() +","+location.getLongitude() , Toast.LENGTH_SHORT).show();
        //locationManager.removeUpdates(this);

        if(mMarker != null){
            mMarker.setPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
            map.invalidate();
            IMapController mapController = map.getController();
            GeoPoint startPoint;
            startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setZoom(16.5);
            mapController.setCenter(startPoint);
            map.invalidate();
        }else{
            createMarker(location);
        }



        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ridersAvailable");

        GeoFire geofire = new GeoFire(ref);
        geofire.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Now request location.
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestLocation();
                    handler.postDelayed(this, 1000 * 60 * 2);
                }
            }, 1000);
        }
    }

    public void requestLocation() {
        Log.w("requestLocation","requestLocation");

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Log.w("requestLocation","start");
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                ;

            Log.w("requestLocation","gps");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, this);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                ;

            Log.w("requestLocation","NETWORK");
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        try{
            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("ridersAvailable");

            GeoFire geofire = new GeoFire(ref);
            geofire.removeLocation(userId);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}