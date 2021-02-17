package com.uisrael.recadots.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.uisrael.recadots.activities.driver.MapDriverActivity;
import com.uisrael.recadots.activities.driver.MapDriverBookingActivity;
import com.uisrael.recadots.providers.ClientBookingProvider;

public class CancelReceiver extends BroadcastReceiver {

        private ClientBookingProvider mClientBookingProvider;

        @Override
        public void onReceive(Context context, Intent intent) {

            String idClient = intent.getExtras().getString("idClient");
            mClientBookingProvider = new ClientBookingProvider();
            mClientBookingProvider.updateStatus(idClient, "cancel");

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(2 );

            Intent intent1 = new Intent(context, MapDriverActivity.class); //
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent1.setAction(Intent.ACTION_RUN);
            context.startActivity(intent1);

        }
}
