package com.uisrael.recadots.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.uisrael.recadots.R;
import com.uisrael.recadots.models.HistoryBooking;
import com.uisrael.recadots.providers.ClientProvider;
import com.uisrael.recadots.providers.DriverProvider;

public class HistoryBookingDriverAdapter extends FirebaseRecyclerAdapter<HistoryBooking, HistoryBookingDriverAdapter.ViewHolder> {

    private ClientProvider mClientProvider;
    private Context mContext;

    public HistoryBookingDriverAdapter(FirebaseRecyclerOptions<HistoryBooking> options, Context context){
        super(options);
        mClientProvider = new ClientProvider();
        mContext = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull final HistoryBookingDriverAdapter.ViewHolder holder, int position, @NonNull HistoryBooking model) {

        holder.textViewOrigin.setText(model.getOrigin());
        holder.textViewDestination.setText(model.getDestination());
        holder.textViewCalification.setText(String.valueOf(model.getCalificationDriver()));
        mClientProvider.getClient(model.getIdClient()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String name = snapshot.child("name").getValue().toString();
                    holder.textViewName.setText(name);
                    if (snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.with(mContext).load(image).into(holder.imageViewHistoryBooking);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @NonNull
    @Override
    public HistoryBookingDriverAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_history_booking, parent, false);
        return new HistoryBookingDriverAdapter.ViewHolder(view);
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView textViewName;
        private TextView textViewOrigin;
        private TextView textViewDestination;
        private TextView textViewCalification;
        private ImageView imageViewHistoryBooking;


        public ViewHolder(View view){
            super(view);
            textViewName = view.findViewById(R.id.textViewName);
            textViewOrigin = view.findViewById(R.id.textViewOrigin);
            textViewDestination = view.findViewById(R.id.textViewDestination);
            textViewCalification = view.findViewById(R.id.textViewCalification);
            imageViewHistoryBooking = view.findViewById(R.id.imageViewHistoryBooking);

        }
    }

}
