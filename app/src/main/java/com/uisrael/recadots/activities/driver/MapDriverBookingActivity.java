package com.uisrael.recadots.activities.driver;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.uisrael.recadots.R;
import com.uisrael.recadots.activities.client.CalificationDriverActivity;
import com.uisrael.recadots.activities.client.DetailRequestActivity;
import com.uisrael.recadots.activities.client.RequestDriverActivity;
import com.uisrael.recadots.models.Client;
import com.uisrael.recadots.models.ClientBooking;
import com.uisrael.recadots.models.FCMBody;
import com.uisrael.recadots.models.FCMResponse;
import com.uisrael.recadots.providers.AuthProvider;
import com.uisrael.recadots.providers.ClientBookingProvider;
import com.uisrael.recadots.providers.ClientProvider;
import com.uisrael.recadots.providers.GeofireProvider;
import com.uisrael.recadots.providers.GoogleApiProvider;
import com.uisrael.recadots.providers.NotificationProvider;
import com.uisrael.recadots.providers.TokenProvider;
import com.uisrael.recadots.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapDriverBookingActivity extends AppCompatActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvider mAuthProvider;
    private GeofireProvider mGeofireProvider;
    private ClientProvider mClientProvider;
    private ClientBookingProvider mClientBookingProvider;
    private TokenProvider mTokenProvider;
    private NotificationProvider mNoticationProvider;

    private LocationRequest mLocationRequest; //gps
    private FusedLocationProviderClient mFusedLocation;

    private final static int LOCATION_REQUEST_CODE = 1; // BAMDERA PARA SABER SI DEBE SOLISITAR LOS PERMISOS DE LOCLIZACION
    private final static int SETTING_REQUEST_CODE = 2;

    private Marker mMarker;
    private LatLng mCurrentLatLng;

    private TextView mTextViewClientBooking;
    private TextView mTextViewEmailClientBooking;
    private TextView mTextViewOriginClientBooking;
    private TextView mTextViewDestinationClientBooking;
    private ImageView mImageViewBooking;

    private String mExtraClientId;

    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private GoogleApiProvider mGoogleApiProvider;
    private List<LatLng> mPolylineList;
    private PolylineOptions mPolylineOptions;

    private boolean mIsFirstTime = true;
    private boolean mIsCloseToClient = false;
    private Button mButtonStartBooking;
    private Button mButtonFinishBooking;


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                    if(mMarker != null){
                        mMarker.remove();
                    }
                    mMarker = mMap.addMarker(new MarkerOptions().position(
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

                    if (mIsFirstTime){
                        mIsFirstTime = false;
                        getClientBooking();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver_booking);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider("drivers_working");
        mTokenProvider = new TokenProvider();
        mClientProvider = new ClientProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mNoticationProvider = new NotificationProvider();

        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mTextViewClientBooking = findViewById(R.id.textViewClientBooking);
        mTextViewEmailClientBooking = findViewById(R.id.textViewEmailClientBooking);
        mTextViewOriginClientBooking = findViewById(R.id.textViewOriginClientBooking);
        mTextViewDestinationClientBooking = findViewById(R.id.textViewDestinationClientBooking);
        mButtonStartBooking = findViewById(R.id.btnStartBooking);
        mButtonFinishBooking = findViewById(R.id.btnFinishBooking);
        mImageViewBooking = findViewById(R.id.imageViewClientBooking);

        //mButtonStartBooking.setEnabled(false);

        mExtraClientId = getIntent().getStringExtra("idClient");

        mGoogleApiProvider = new GoogleApiProvider(MapDriverBookingActivity.this);

        getClient();

        mButtonStartBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsCloseToClient){
                    startBooking();

                }else{
                    Toast.makeText(MapDriverBookingActivity.this, "Debes estar mas cerca a la posicion de recogida", Toast.LENGTH_SHORT).show();
                }

            }
        });

        mButtonFinishBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishBooking();
            }
        });


    }

    private void finishBooking() {
        mClientBookingProvider.updateStatus(mExtraClientId, "finish");
        mClientBookingProvider.updateIdHistoryBooking(mExtraClientId);
        sendNotification("Viaje finalizado");
        if (mFusedLocation != null){
            mFusedLocation.removeLocationUpdates(mLocationCallback);
        }
        mGeofireProvider.removeLocation(mAuthProvider.getId());
        Intent intent = new Intent(MapDriverBookingActivity.this, CalificationClientActivity.class);//client
        intent.putExtra("idClient", mExtraClientId);
        startActivity(intent);
        finish();
    }

    private void startBooking() {
        mClientBookingProvider.updateStatus(mExtraClientId,"start");
        mButtonStartBooking.setVisibility(View.GONE);
        mButtonFinishBooking.setVisibility(View.VISIBLE);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_pin_blue)));
        drawRoute(mDestinationLatLng);
        sendNotification("Viaje Iniciado");

    }

    private double getDistanceBetween(LatLng clientLatLng, LatLng driverLatLng){
        double distance = 0;
        Location clientLocation = new Location("");
        Location driverLocation = new Location("");
        clientLocation.setLatitude(clientLatLng.latitude);
        clientLocation.setLongitude(clientLatLng.longitude);
        driverLocation.setLatitude(driverLatLng.latitude);
        driverLocation.setLongitude(driverLatLng.longitude);
        distance = clientLocation.distanceTo(driverLocation);
        return distance;

    }

    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String destination = snapshot.child("destination").getValue().toString();
                    String origin = snapshot.child("origin").getValue().toString();
                    double destinationLat = Double.parseDouble(snapshot.child("destinationLat").getValue().toString());
                    double destinationLng = Double.parseDouble(snapshot.child("destinationLng").getValue().toString());

                    double originLat = Double.parseDouble(snapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(snapshot.child("originLng").getValue().toString());
                    mOriginLatLng = new LatLng(originLat, originLng);
                    mDestinationLatLng = new LatLng(destinationLat, destinationLng);
                    mTextViewOriginClientBooking.setText("recojer en : " + origin);
                    mTextViewDestinationClientBooking.setText("destion: " + destination);
                    mMap.addMarker(new MarkerOptions().position(mOriginLatLng).title("Recoger aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_map_pin_red)));
                    drawRoute(mOriginLatLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void drawRoute(LatLng latLng){ // dibujar una ruta
        mGoogleApiProvider.getDirections(mCurrentLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                try {

                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray =  jsonObject.getJSONArray("routes");
                    JSONObject router = jsonArray.getJSONObject(0);
                    JSONObject polylines = router.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    mPolylineList = DecodePoints.decodePoly(points);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.DKGRAY);
                    mPolylineOptions.width(13f); // para aumentar o disminuir la linea de ubicacion
                    mPolylineOptions.startCap(new SquareCap());
                    mPolylineOptions.jointType(JointType.ROUND);
                    mPolylineOptions.addAll(mPolylineList);
                    mMap.addPolyline(mPolylineOptions);

                    JSONArray legs = router.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");




                }catch (Exception e){
                    Log.d("Error", "Error encontrado" + e.getMessage());

                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }


    private void getClient() {
        mClientProvider.getClient(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    String email = snapshot.child("email").getValue().toString();
                    String name = snapshot.child("name").getValue().toString();
                    String image = "";
                    if (snapshot.hasChild("image")){
                        image = snapshot.child("image").getValue().toString();
                        Picasso.with(MapDriverBookingActivity.this).load(image).into(mImageViewBooking);
                    }
                    mTextViewClientBooking.setText(name);
                    mTextViewEmailClientBooking.setText(email);
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
            if (!mIsCloseToClient){
                if (mOriginLatLng != null && mCurrentLatLng != null){
                    double distance = getDistanceBetween(mOriginLatLng, mCurrentLatLng);// retorna en metros
                    if (distance <= 200){
                        //mButtonStartBooking.setEnabled(true);
                        mIsCloseToClient = true;
                        Toast.makeText(this, "estas cerca a la posicion de recogida", Toast.LENGTH_SHORT).show();
                    }
                }

            }

        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true); //3:08 55

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //con mas presicion
        mLocationRequest.setSmallestDisplacement(5);

        startLocation();

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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (gpsActived()){
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
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
                                ActivityCompat.requestPermissions(MapDriverBookingActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                                // abilita los permisos para uti;izar la ubicacion del celular

                            }
                        })
                        .create()
                        .show();
            }
            else{
                ActivityCompat.requestPermissions(MapDriverBookingActivity.this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                // abilita los permisos para uti;izar la ubicacion del celular


            }

        }

    }

    private void sendNotification(final String status) {

        mTokenProvider.getToken(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    String token = snapshot.child("token").getValue().toString();
                    final Map<String, String> map = new HashMap<>();
                    map.put("title", "ESTADO DE TU VIAJE");
                    map.put("body","Tu estado del viaje es : "+ status);
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s",map);
                    mNoticationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() != 1){
                                    Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificacion ", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("Error","Error" + t.getMessage());

                        }
                    });

                }else {
                    Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}