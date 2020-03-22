package com.example.custom_maps;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    public static final int ZOOM_CAMERA = 15;

    private GoogleMap mMap;
    private List<MarkerOptions> markers = new ArrayList<MarkerOptions>();
    private Marker userMarker;
    private MarkerOptions lastMarker;
    private MarkerOptions markerNear;
    private double minDist = Double.MAX_VALUE;
    private double Ulat = 0.0, Ulng = 0.0;
    private EditText search_Text;
    private EditText name_Marker;
    private Button addMarker;
    private TextView infoTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        search_Text = findViewById(R.id.input_search);
        name_Marker = findViewById(R.id.name_Marker);
        addMarker = findViewById(R.id.addMarker);
        infoTextView = findViewById(R.id.infoTextView);

        addMarker.setOnClickListener(
                (v) -> {
                    addMarker();
                }
        );

        initMap();
        init();
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
    }

    private void init() {
        search_Text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == KeyEvent.ACTION_DOWN
                    || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    //Se ejecuta el metodo para buscar
                    geoLocate();
                }
                    return false;
            }
        });
    }

    private void geoLocate() {
        String searchString = search_Text.getText().toString();
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e){
            //error
        }
        if(list.size() > 0) {
            Address address = list.get(0);
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), ZOOM_CAMERA);
        }

    }

    private void moveCamera(LatLng latLng, float zoom){
        //Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        lastMarker = new MarkerOptions();
        lastMarker.position(latLng);
    }

    public void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    public void addMarker() {
        String title = name_Marker.getText().toString();
        if(title.isEmpty()){
            Toast.makeText(this, "Name marker empty", Toast.LENGTH_SHORT).show();
        } else if(lastMarker.getPosition() == null) {
            Toast.makeText(this, "Position marker empty", Toast.LENGTH_SHORT).show();
        } else {
            lastMarker.title(title);
            double distanceM = distanceBetweenTwoPoints(userMarker.getPosition(), lastMarker.getPosition());
            lastMarker.snippet("Distance in Meters is: " + distanceM);
            lastMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon));
            if(distanceM < minDist) {
                minDist = distanceM;
                markerNear = lastMarker;
                String address = getInfoGeolocate(lastMarker.getPosition()).get(0).getAddressLine(0);
                if(minDist < 100.0) { //la distancia menor a 100 metros esta en el mismo lugar
                    infoTextView.setText("Your locate is: " + address);
                } else {
                    infoTextView.setText("The closest marker is: " + title +" with direction: "+ address);
                }
            }
            markers.add(lastMarker);
            mMap.addMarker(lastMarker);
        }
    }

    public void updateDistanceMarkers() {
        for(MarkerOptions m: markers){
            double distanceM = distanceBetweenTwoPoints(userMarker.getPosition(), m.getPosition());
            m.snippet("Distance in Meters is: " + distanceM);
            if(distanceM < minDist) {
                minDist = distanceM;
                markerNear = m;
                String address = getInfoGeolocate(m.getPosition()).get(0).getAddressLine(0);
                if(minDist < 100.0) { //la distancia menor a 100 metros esta en el mismo lugar
                    infoTextView.setText("Your locate is: " + address);
                } else {
                    infoTextView.setText("The closest marker is: " + m.getTitle() +" with direction: "+ address);
                }
            }
        }


    }

    private double distanceBetweenTwoPoints(LatLng l1, LatLng l2) {
        float[] results = new float[1];
        Location.distanceBetween(l1.latitude, l1.longitude,
                l2.latitude, l2.longitude,
                results);

        return  results[0];
    }

    private List<Address> getInfoGeolocate(LatLng latLng) {
        Geocoder geocoder;
        List<Address> addresses = new ArrayList<>();
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }

        /**
        String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        String city = addresses.get(0).getLocality();
        String state = addresses.get(0).getAdminArea();
        String country = addresses.get(0).getCountryName();
        String postalCode = addresses.get(0).getPostalCode();
        String knownName = addresses.get(0).getFeatureName();*/
        return addresses;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        myLocation();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        // Add a marker in Sydney and move the camera
        /**LatLng sydney = new LatLng(-34, 151);
         mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
         mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/

    }

    private void addMarkerUser(double lat, double lng) {
        LatLng coordinates = new LatLng(lat, lng);
        CameraUpdate myLocate = CameraUpdateFactory.newLatLngZoom(coordinates, ZOOM_CAMERA);


        String address = getInfoGeolocate(coordinates).get(0).getAddressLine(0);

        if (userMarker != null) userMarker.remove();
        userMarker = mMap.addMarker(new MarkerOptions()
                .position(coordinates)
                .title("User Position")
                .snippet(address)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_icon)));
        mMap.animateCamera(myLocate);
    }

    private void updateLocation(Location location) {
        if (location != null) {
            Ulat = location.getLatitude();
            Ulng = location.getLongitude();
            addMarkerUser(Ulat, Ulng);
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
            updateDistanceMarkers();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void myLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        updateLocation(location);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 0, locationListener);
    }
}
