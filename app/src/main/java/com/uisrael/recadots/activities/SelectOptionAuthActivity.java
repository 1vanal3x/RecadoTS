package com.uisrael.recadots.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.uisrael.recadots.R;
import com.uisrael.recadots.activities.client.RegisterActivity;
import com.uisrael.recadots.activities.driver.RegisterDriverActivity;
import com.uisrael.recadots.includes.MyToolbar;

public class SelectOptionAuthActivity extends AppCompatActivity {


    Toolbar mToolbar;
    Button mButtonGoToLogin;
    Button mButtonGoToRegister;
    SharedPreferences mPref;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_auth);

        MyToolbar.show(this,"Seleccionar Opcion",true);


        mPref = getApplicationContext().getSharedPreferences("typeUser",MODE_PRIVATE);

        mButtonGoToLogin = findViewById(R.id.btnGoToLogin);
        mButtonGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin();
            }
        });

        mButtonGoToRegister = findViewById(R.id.btnGoToRegister);
        mButtonGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRegister();
            }
        });

    }

    public void goToLogin(){

        Intent intent = new Intent(SelectOptionAuthActivity.this, LoginActivity.class);
        startActivity(intent);


    }

    public void goToRegister(){

        String typeUser = mPref.getString("user", " ");

        if(typeUser.equals("client")){
            Intent intent = new Intent( SelectOptionAuthActivity.this, RegisterActivity.class);
            startActivity(intent);

        }else{

            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterDriverActivity.class);
            startActivity(intent);

        }


    }
}