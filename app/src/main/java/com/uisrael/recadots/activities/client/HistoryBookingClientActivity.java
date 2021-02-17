package com.uisrael.recadots.activities.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DownloadManager;
import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.uisrael.recadots.R;
import com.uisrael.recadots.adapters.HistoryBookingClientAdapter;
import com.uisrael.recadots.includes.MyToolbar;
import com.uisrael.recadots.models.HistoryBooking;
import com.uisrael.recadots.providers.AuthProvider;

public class HistoryBookingClientActivity extends AppCompatActivity {

    private RecyclerView mReciclerView;
    private HistoryBookingClientAdapter mAdapter;
    private AuthProvider mAuthProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_booking_client);
        MyToolbar.show(this,"Historial de viajes", true);

        mReciclerView = findViewById(R.id.recyclerViewHistoryBooking);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mReciclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuthProvider = new AuthProvider();
        Query query = FirebaseDatabase.getInstance().getReference().child("HistoryBooking").orderByChild("idClient").equalTo(mAuthProvider.getId());
        FirebaseRecyclerOptions<HistoryBooking> options = new FirebaseRecyclerOptions.Builder<HistoryBooking>()
                                                            .setQuery(query, HistoryBooking.class)
                                                            .build();
        mAdapter = new HistoryBookingClientAdapter(options, HistoryBookingClientActivity.this);

        mReciclerView.setAdapter(mAdapter);
        mAdapter.startListening();

    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }
}