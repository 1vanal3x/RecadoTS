package com.uisrael.recadots.activities.client;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.uisrael.recadots.R;
import com.uisrael.recadots.models.Client;
import com.uisrael.recadots.models.ClientBooking;
import com.uisrael.recadots.models.FCMBody;
import com.uisrael.recadots.models.FCMResponse;
import com.uisrael.recadots.providers.AuthProvider;
import com.uisrael.recadots.providers.ClientBookingProvider;
import com.uisrael.recadots.providers.GeofireProvider;
import com.uisrael.recadots.providers.GoogleApiProvider;
import com.uisrael.recadots.providers.NotificationProvider;
import com.uisrael.recadots.providers.TokenProvider;
import com.uisrael.recadots.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDriverActivity extends AppCompatActivity {

    private LottieAnimationView mAnimation;
    private TextView mTextViewLookingFor;
    private Button mButtonCancelRequest;
    private GeofireProvider mGeofireProvider;


    private String mExtraOrigin;
    private String mExtraDestination;
    private double mExtraOriginLat;
    private double mExtraOriginLng;
    private double mExtraDestinationLat;
    private double mExtraDestinationlng;
    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private  double mRadius = 0.1;
    private  boolean mDriverFound = false;
    private  String mIdDriverFound = "";
    private LatLng mDriverFoundLantLng;
    private NotificationProvider mNoticationProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;
    private GoogleApiProvider mGoogleApiProvider;

    private ValueEventListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        mAnimation = findViewById(R.id.animation);
        mTextViewLookingFor = findViewById(R.id.textViewLookingFor);
        mButtonCancelRequest = findViewById(R.id.btnCancelRequest);

        mAnimation.playAnimation();

        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraOriginLat = getIntent().getDoubleExtra("origin_lat",0);
        mExtraOriginLng = getIntent().getDoubleExtra("origin_lng",0);
        mExtraDestinationLat = getIntent().getDoubleExtra("destination_lat",0);
        mExtraDestinationlng = getIntent().getDoubleExtra("destination_lng",0);

        mOriginLatLng = new LatLng(mExtraOriginLat, mExtraOriginLng);
        mDestinationLatLng = new LatLng(mExtraDestinationLat, mExtraDestinationlng);

        mGeofireProvider = new GeofireProvider("active_drivers");
        mNoticationProvider = new NotificationProvider();
        mTokenProvider = new TokenProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mAuthProvider = new AuthProvider();
        mGoogleApiProvider = new GoogleApiProvider(RequestDriverActivity.this);

        mButtonCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRequest();
            }
        });

        getClosestDriver();

    }

    private void cancelRequest() {
        mClientBookingProvider.delete(mAuthProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendNotificationCancel();
            }
        });
    }

    private void getClosestDriver(){
         mGeofireProvider.getActiveDrivers(mOriginLatLng, mRadius).addGeoQueryEventListener(new GeoQueryEventListener() {
             @Override
             public void onKeyEntered(String key, GeoLocation location) {

                 if(!mDriverFound){
                     mDriverFound = true;
                     mIdDriverFound = key;
                     mDriverFoundLantLng = new LatLng(location.latitude, location.longitude);
                     mTextViewLookingFor.setText("Conductor encontrado|\n Esperando respuesta");
                     createClientBooking();
                     Log.d("DRIVER","ID" + mIdDriverFound);

                 }

             }

             @Override
             public void onKeyExited(String key) {

             }

             @Override
             public void onKeyMoved(String key, GeoLocation location) {

             }

             @Override
             public void onGeoQueryReady() {
                 //ingresa cuando termina la busqueda del conductor del radio de 0.1km
                 if (!mDriverFound){
                     mRadius = mRadius + 0.1f;
                     //no encontro ningun conductor

                     if (mRadius > 10){
                         mTextViewLookingFor.setText("no se encontro conductor");
                         Toast.makeText(RequestDriverActivity.this, "no se encontro ningun conductor", Toast.LENGTH_SHORT).show();
                         return;
                     }
                     else{
                         getClosestDriver();
                     }
                 }

             }

             @Override
             public void onGeoQueryError(DatabaseError error) {

             }
         });

    }
    private void createClientBooking(){


        mGoogleApiProvider.getDirections(mOriginLatLng, mDriverFoundLantLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                try {

                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray =  jsonObject.getJSONArray("routes");
                    JSONObject router = jsonArray.getJSONObject(0);
                    JSONObject polylines = router.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    JSONArray legs = router.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");
                    sendNotification(durationText,distanceText);


                }catch (Exception e){
                    Log.d("Error", "Error encontrado" + e.getMessage());

                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });


    }

    private void sendNotificationCancel(){
        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    String token = snapshot.child("token").getValue().toString();
                    final Map<String, String> map = new HashMap<>();
                    map.put("title", "VIAJE CANCELADO" ); // madar que tipo de encomienda aki
                    map.put("body",
                            "El cliente cancelo la solicitud"
                    );
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    mNoticationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() == 1){
                                    Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RequestDriverActivity.this,MapClientActivity.class);
                                    startActivity(intent);
                                    finish();
                                    //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion ", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("Error","Error" + t.getMessage());

                        }
                    });

                }else {
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(final String time, final String km) {

        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){

                    String token = snapshot.child("token").getValue().toString();
                    final Map<String, String> map = new HashMap<>();
                    map.put("title", "SOLICITUD DE SERVICIO" + time + " de tu posicion " ); // madar que tipo de encomienda aki
                    map.put("body"," un cliente esta solicitando un servicio a una distacia de " + km + "\n" +
                            " Recoger en : " + mExtraOrigin + "\n" +
                            " Destino: " + mExtraDestination
                    );
                    map.put("idClient", mAuthProvider.getId());
                    map.put("origin", mExtraOrigin);
                    map.put("destination", mExtraDestination);
                    map.put("min", time);
                    map.put("distance", km);
                    FCMBody fcmBody = new FCMBody(token, "high", "4500s", map);
                    mNoticationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body() != null){
                                if (response.body().getSuccess() == 1){
                                    ClientBooking clientBooking = new ClientBooking(
                                            mAuthProvider.getId(),
                                            mIdDriverFound,
                                            mExtraDestination,
                                            mExtraOrigin,
                                            time,
                                            km,
                                            "create",
                                            mExtraOriginLat,
                                            mExtraOriginLng,
                                            mExtraDestinationLat,
                                            mExtraDestinationlng
                                    );
                                    mClientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            checkStatusClientBooking();

                                        }
                                    });

                                    //Toast.makeText(RequestDriverActivity.this, "La notificacion se ha enviado correctamente", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion", Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion ", Toast.LENGTH_SHORT).show();
                            }

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("Error","Error" + t.getMessage());

                        }
                    });

                }else {
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void checkStatusClientBooking() {
        mListener = mClientBookingProvider.getStatus(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()){
                    String status = snapshot.getValue().toString();
                    if (status.equals("accept")){
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientBookingActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    else if (status.equals("cancel")){
                        Toast.makeText(RequestDriverActivity.this, "El conductor no acepto el viaje", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null){
            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListener);
        }

    }
}