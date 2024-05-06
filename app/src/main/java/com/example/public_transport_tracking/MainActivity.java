package com.example.public_transport_tracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    FusedLocationProviderClient fusedLocationProviderClient;
    //TextView lattitude,longitude;
    Button logOut;
    private final static int REQUEST_CODE = 100;

    private GoogleMap gMap;
    float distanceInMeters;
    private Double Ulattitude=5.99,Ulongitude=70.44;

    private Double latitude, longitude;

    FirebaseDatabase database;
    DatabaseReference reference;
    FirebaseAuth mAuth;
    BitmapDescriptor userIcon;
    BitmapDescriptor busIcon;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent=new Intent(MainActivity.this, MyFirebaseMessagingService.class);
        startService(serviceIntent);

        database = FirebaseDatabase.getInstance( "https://travelmobix-7d56c-default-rtdb.asia-southeast1.firebasedatabase.app");

        mAuth = FirebaseAuth.getInstance();

        userIcon= BitmapDescriptorFactory.fromResource(R.drawable.passenger);
        busIcon= BitmapDescriptorFactory.fromResource(R.drawable.buss);
        logOut=findViewById(R.id.logOut);

        LocationUtils.checkLocationSettings(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map);
        mapFragment.getMapAsync(this);



        float[] results = new float[1];

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Location.distanceBetween(Ulattitude, Ulongitude, latitude, longitude, results);

                distanceInMeters = results[0];

                //Toast.makeText(MainActivity.this, "Distance: " + distanceInMeters, Toast.LENGTH_SHORT).show();

            }
        }, 2000);

        if (distanceInMeters < 500) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "bus_notifications_channel";
                CharSequence channelName = "Bus Notifications";

                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);

            }

            logOut.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAuth.signOut();

                    Intent intent = new Intent(MainActivity.this, Login.class);
                    startActivity(intent);

                    finish();
                }
            });
        }



    }




    private void getLastLocation(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){


            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if (location != null){


                                try {

                                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    Ulattitude= addresses.get(0).getLatitude();
                                    Ulongitude= addresses.get(0).getLongitude();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }

                        }
                    });


        }else {

            askPermission();


        }


    }
    private void askPermission() {

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {

        if (requestCode == REQUEST_CODE){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){


                getLastLocation();

            }else {


                Toast.makeText(MainActivity.this,"Please provide the required permission",Toast.LENGTH_SHORT).show();

            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //Log.e("latitude", Ulattitude.toString());
       // Toast.makeText(MainActivity.this, "Locations: " + Ulattitude.toString(), Toast.LENGTH_SHORT).show();


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                LatLng Ulocation = new LatLng(Ulattitude, Ulongitude);

                Bitmap userBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.user_marker_icon);

                int desiredWidth = 100;
                int desiredHeight = 100;

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(userBitmap, desiredWidth, desiredHeight, false);

                BitmapDescriptor userIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

                googleMap.addMarker(new MarkerOptions().position(Ulocation).icon(userIcon));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Ulocation, 12));
            }
        }, 2000);



        DatabaseReference ref = database.getReference().child("buses");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> buses;
                buses = (Map<String, Object>) dataSnapshot.getValue();

                if (buses != null) {
                    int notificationId = 1;
                    float[] results = new float[1];

                    for (Map.Entry<String, Object> entry : buses.entrySet()) {
                        String busKey = entry.getKey();
                        Map<String, Object> singleBus = (Map<String, Object>) entry.getValue();

                        // Check if the bus has a "location" and "name" node
                        if (singleBus.containsKey("location") && singleBus.get("location") instanceof Map && singleBus.containsKey("name")) {
                            Map<String, Object> locationData = (Map<String, Object>) singleBus.get("location");
                            String busName = (String) singleBus.get("name");

                            // Check if latitude and longitude are not null before accessing
                            if (locationData.containsKey("latitude") && locationData.containsKey("longitude")) {
                                latitude = (Double) locationData.get("latitude");
                                longitude = (Double) locationData.get("longitude");

                                Log.e("FirebaseDebug", "Latitude: " + latitude);


                                if (latitude != null && longitude != null) {
                                    Location.distanceBetween(Ulattitude, Ulongitude, latitude, longitude, results);
                                    distanceInMeters = results[0];

                                    // Check if the distance is less than 500 meters
                                    if (distanceInMeters < 500) {
                                        // Create a notification with a unique ID for each bus
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "bus_notifications_channel")
                                                .setSmallIcon(R.drawable.travel_mobix_app_icon)
                                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.travel_mobix_logo))
                                                .setContentTitle(busName + " is Nearby") // Use the bus name in the notification
                                                .setContentText("Get ready to catch " + busName + ". It's less than 500m away!")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                                        //notificationManager.notify(notificationId++, builder.build());
                                    }

                                    LatLng location = new LatLng(latitude, longitude);
                                    Bitmap userBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.buss);

                                    int desiredWidth = 75;
                                    int desiredHeight = 75;

                                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(userBitmap, desiredWidth, desiredHeight, false);

                                    BitmapDescriptor busIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

                                    googleMap.addMarker(new MarkerOptions().position(location).icon(busIcon));
                                } else {
                                    Log.e("FirebaseDebug", "Latitude or Longitude is null for bus: " + busKey);
                                }
                            } else {
                                Log.e("FirebaseDebug", "Location node does not contain 'latitude' or 'longitude' for bus: " + busKey);
                            }
                        } else {
                            Log.e("FirebaseDebug", "Bus node does not contain 'location' or 'name' for bus: " + busKey);
                        }
                    }

                    if (!buses.isEmpty()) {
                        System.out.println(buses.toString());
                       // Toast.makeText(MainActivity.this, "Locations: " + buses.toString(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "No valid bus locations found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No data in the 'buses' node.", Toast.LENGTH_SHORT).show();
                }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("FirebaseError", "Failed to get data. Error: " + databaseError.getMessage());

                        Toast.makeText(MainActivity.this, "Fail to get data.", Toast.LENGTH_SHORT).show();

                    }
                });
    }
}