package com.uisrael.recadots.activities.driver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.uisrael.recadots.R;
import com.uisrael.recadots.activities.MainActivity;
import com.uisrael.recadots.activities.client.HistoryBookingClientActivity;
import com.uisrael.recadots.activities.client.MapClientActivity;
import com.uisrael.recadots.activities.client.UpdateProfileActivity;
import com.uisrael.recadots.includes.MyToolbar;
import com.uisrael.recadots.providers.AuthProvider;
import com.uisrael.recadots.providers.GeofireProvider;
import com.uisrael.recadots.providers.TokenProvider;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private TokenProvider mTokenProvider;

    private LocationRequest mLocationRequest; //gps
    private FusedLocationProviderClient mFusedLocation;

    private final static int LOCATION_REQUEST_CODE = 1; // BAMDERA PARA SABER SI DEBE SOLISITAR LOS PERMISOS DE LOCLIZACION
    private final static int SETTING_REQUEST_CODE = 2;

    private Marker mMarker;

    private Button mButtonConnect;
    private boolean mIsConnect = false;

    private LatLng mCurrentLatLng;

    private ValueEventListener mListener;


    //cada ves que el usuario se mueva

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if(mMarker != null){
                        mMarker.remove();
                    }

                    mMarker = mMap.addMarker(new MarkerOptions().position( //icono de la posicion
                            new LatLng(location.getLatitude(), location.getLongitude())
                            )
                                    .title("Tu posicion")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.iconcar))
                    );

                    //optener una locacion del usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))//la posicion que se ubique la camara
                                    .zoom(16f)
                                    .build()

                    ));

                    updateLocation();

                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);

        MyToolbar.show(this, "Conductor", false);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();


        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mButtonConnect= findViewById(R.id.btnConnect);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsConnect){
                    disconnect();
                }else {
                    startLocation();
                }

            }
        });
        generateToken();
        isDriverWorking();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mListener != null){
            mGeofireProvider.isDriverWorking(mAuthProvider.getId()).removeEventListener(mListener);
        }
    }

    private void isDriverWorking() {

        mListener  = mGeofireProvider.isDriverWorking(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    disconnect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void updateLocation(){
        if(mAuthProvider.existSession() && mCurrentLatLng != null){

            mGeofireProvider.saveLocation(mAuthProvider.getId(), mCurrentLatLng);

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //con mas presicion
        mLocationRequest.setSmallestDisplacement(5);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

         if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                } else {
                    checkLocationPermissions();
                }
            } else {
                checkLocationPermissions();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTING_REQUEST_CODE && gpsActived()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (gpsActived()){
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                showAlertDialogNOGPS();
            }


        }else{
            mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }

    }

    private void showAlertDialogNOGPS(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicacion para continuar")
        .setPositiveButton("configuraciones", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),SETTING_REQUEST_CODE);

            }
        }).create().show();
    }



    private boolean gpsActived(){
        boolean isActive = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            isActive = true;
        }
        return isActive;
    }


    private void disconnect(){

        if (mFusedLocation != null){
            mButtonConnect.setText("Conectarse");
            mIsConnect = false;
            mFusedLocation.removeLocationUpdates(mLocationCallback);

            if (mAuthProvider.existSession()){
                mGeofireProvider.removeLocation(mAuthProvider.getId());
            }

        }else{
            Toast.makeText(this, "No te puedes desconectar", Toast.LENGTH_SHORT).show();
        }


    }




    private void startLocation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
               if(gpsActived()){
                   mButtonConnect.setText("Desconectarse");
                   mIsConnect = true;

                   mFusedLocation.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
                   mMap.setMyLocationEnabled(true);
               }else{
                   showAlertDialogNOGPS();
               }


            }else{
                checkLocationPermissions();
            }
        }else{
            if (gpsActived()){
                mFusedLocation.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }else{
                showAlertDialogNOGPS();
            }

        }

    }

    private void checkLocationPermissions(){

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("esta aplicacion requiere de los permisos de ubicacion para poder utilizar")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MapDriverActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                                // abilita los permisos para uti;izar la ubicacion del celular

                            }
                        })
                            .create()
                            .show();
            }
            else{
                ActivityCompat.requestPermissions(MapDriverActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                // abilita los permisos para uti;izar la ubicacion del celular


            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.driver_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_logout){
            logout();
        }

        if(item.getItemId() == R.id.action_update){
            Intent intent = new Intent(MapDriverActivity.this, UpdateProfileDriverActivity.class);
            startActivity(intent);
        }

        if(item.getItemId() == R.id.action_history){
            Intent intent = new Intent(MapDriverActivity.this, HistoryBookingDriverActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    void logout(){

        disconnect();

        mAuthProvider.logout();
        Intent intent = new Intent(MapDriverActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

    }

    void generateToken(){

        mTokenProvider.create(mAuthProvider.getId());

    }

}






