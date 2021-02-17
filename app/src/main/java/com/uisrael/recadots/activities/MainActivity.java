package com.uisrael.recadots.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.uisrael.recadots.R;
import com.uisrael.recadots.activities.client.MapClientActivity;
import com.uisrael.recadots.activities.driver.MapDriverActivity;

public class MainActivity extends AppCompatActivity {

    Button mButtonCliente;
    Button mButtonConductor;
    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPref = getApplicationContext().getSharedPreferences(  "typeUser", MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPref.edit();

        mButtonCliente = findViewById(R.id.btnCliente);
        mButtonConductor = findViewById(R.id.btnConductor);

        mButtonCliente.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("user", "client");
                editor.apply();
                goToSelectAuth();
            }
        });

        mButtonConductor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putString("user", "driver");
                editor.apply();
                goToSelectAuth();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        if(FirebaseAuth.getInstance().getCurrentUser() !=null){

            String user = mPref.getString("user" , " ");
            if(user.equals("client")){

                Intent intent = new Intent(MainActivity.this, MapClientActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }else{
                Intent intent = new Intent(MainActivity.this, MapDriverActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

            }

        }

    }

    private void goToSelectAuth() {

        Intent intent = new Intent(MainActivity.this, SelectOptionAuthActivity.class);
        startActivity(intent);

    }
}