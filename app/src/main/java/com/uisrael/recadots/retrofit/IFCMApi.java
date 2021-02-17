package com.uisrael.recadots.retrofit;

import com.uisrael.recadots.models.FCMBody;
import com.uisrael.recadots.models.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMApi {



    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAM8oO5UM:APA91bGV3sb76PUWjaV8p4xCQqFTpMoagL4vRa2y6hiEDd-aQSEfWczN1wEbgq2Plfyg9YjtQTO7sV1ntz2nXGKC8KGWRXbO7Iq5bH1xI5m-JhMdl3V62PbjBp76W1LAPYC3DOLC927-"
    })


    @POST("fcm/send")
    Call<FCMResponse> send(@Body FCMBody body);


}
