package com.example.public_transport_tracking;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
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
import java.security.Provider;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends Service {
    FirebaseDatabase database;
    FirebaseAuth mAuth;

    FusedLocationProviderClient fusedLocationProviderClient;
    private final static int REQUEST_CODE = 100;

    float distanceInMeters;
    private Double Ulattitude = 5.99, Ulongitude = 70.44;

    private Double latitude, longitude;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MyFirebaseMessagingService.this);

                        database = FirebaseDatabase.getInstance("https://travelmobix-7d56c-default-rtdb.asia-southeast1.firebasedatabase.app");

                        mAuth = FirebaseAuth.getInstance();
                        while (true) {
                            Log.e("Service", "Service is running...");


                            if (ContextCompat.checkSelfPermission(MyFirebaseMessagingService.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {


                                fusedLocationProviderClient.getLastLocation()
                                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                                            @Override
                                            public void onSuccess(Location location) {

                                                if (location != null) {


                                                    try {

                                                        Geocoder geocoder = new Geocoder(MyFirebaseMessagingService.this, Locale.getDefault());
                                                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                                        Ulattitude = addresses.get(0).getLatitude();
                                                        Ulongitude = addresses.get(0).getLongitude();


                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }


                                                }

                                            }
                                        });


                            }

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

                                                    if (latitude != null && longitude != null) {
                                                        Location.distanceBetween(Ulattitude, Ulongitude, latitude, longitude, results);
                                                        distanceInMeters = results[0];

                                                        // Check if the distance is less than 500 meters
                                                        if (distanceInMeters < 500) {
                                                            // Create a notification with a unique ID for each bus
                                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(MyFirebaseMessagingService.this, "bus_notifications_channel")
                                                                    .setSmallIcon(R.drawable.travel_mobix_app_icon)
                                                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.travel_mobix_logo))
                                                                    .setContentTitle(busName + " is Nearby") // Use the bus name in the notification
                                                                    .setContentText("Get ready to catch " + busName + ". It's less than 500m away!")
                                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MyFirebaseMessagingService.this);
                                                            if (ActivityCompat.checkSelfPermission(MyFirebaseMessagingService.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                                // TODO: Consider calling
                                                                //    ActivityCompat#requestPermissions

                                                                return;
                                                            }
                                                            notificationManager.notify(notificationId++, builder.build());
                                                        }

                                                        LatLng location = new LatLng(latitude, longitude);

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
                                            Toast.makeText(MyFirebaseMessagingService.this, "No valid bus locations found.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(MyFirebaseMessagingService.this, "No data in the 'buses' node.", Toast.LENGTH_SHORT).show();
                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    //handle databaseError
                                    Log.e("FirebaseError", "Failed to get data. Error: " + databaseError.getMessage());

                                    Toast.makeText(MyFirebaseMessagingService.this, "Fail to get data.", Toast.LENGTH_SHORT).show();

                                }
                            });
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
               ).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


