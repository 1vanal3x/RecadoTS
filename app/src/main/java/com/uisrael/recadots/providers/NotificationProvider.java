package com.uisrael.recadots.providers;

import com.uisrael.recadots.models.FCMBody;
import com.uisrael.recadots.models.FCMResponse;
import com.uisrael.recadots.retrofit.IFCMApi;
import com.uisrael.recadots.retrofit.RetrofitClient;

import retrofit2.Call;

public class NotificationProvider {


    private  String url = "https://fcm.googleapis.com";


    public NotificationProvider(){

    }

    public Call<FCMResponse> sendNotification(FCMBody body){
            return RetrofitClient.getClienteObject(url).create(IFCMApi.class).send(body);
    }


}
