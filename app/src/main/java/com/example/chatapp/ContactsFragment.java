package com.example.chatapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class ContactsFragment extends Fragment {

    private View contactView;
    private RecyclerView viewContactList;

    private DatabaseReference contactReference , userReference;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactView = inflater.inflate(R.layout.fragment_contacts, container, false);

        //initializing fields
        viewContactList = (RecyclerView) contactView.findViewById(R.id.contactList);
        viewContactList.setLayoutManager(new LinearLayoutManager(getContext()));

        //Firebase connection
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        contactReference = FirebaseDatabase.getInstance().getReference().child("Contact").child(currentUserId);
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");

        return contactView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions recyclerOptions = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(contactReference,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,contactListHolder> recyclerAdapter = new FirebaseRecyclerAdapter<Contacts, contactListHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final contactListHolder holder, int position, @NonNull Contacts model) {

                final String userId = getRef(position).getKey();

                userReference.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){

                            if (snapshot.child("onlineStatus").hasChild("status")){

                                String status = snapshot.child("onlineStatus").child("status").getValue().toString();
                                String date = snapshot.child("onlineStatus").child("date").getValue().toString();
                                String time = snapshot.child("onlineStatus").child("time").getValue().toString();

                                if (status.equals("online")){
                                    holder.onlineIcon.setVisibility(View.VISIBLE);
                                }
                                else if (status.equals("offline")){
                                    holder.onlineIcon.setVisibility(View.INVISIBLE);
                                }
                            }
                            else {
                                holder.onlineIcon.setVisibility(View.INVISIBLE);
                            }

                            if (snapshot.hasChild("image")){
                                String userProfileImage = snapshot.child("image").getValue().toString();
                                String userProfileDescription = snapshot.child("description").getValue().toString();
                                String userProfileName = snapshot.child("name").getValue().toString();

                                holder.username.setText(userProfileName);
                                holder.userDescription.setText(userProfileDescription);
                                Picasso.get().load(userProfileImage).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            }
                            else{
                                String userProfileDescription = snapshot.child("description").getValue().toString();
                                String userProfileName = snapshot.child("name").getValue().toString();

                                holder.username.setText(userProfileName);
                                holder.userDescription.setText(userProfileDescription);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }


            //display the user profile
            @NonNull
            @Override
            public contactListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.display_user_layout, parent, false);

                contactListHolder viewHolder = new contactListHolder(view);
                return viewHolder;
            }
        };

        viewContactList.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    public static class contactListHolder extends RecyclerView.ViewHolder{

            TextView username, userDescription;
            CircleImageView profileImage;
            ImageView onlineIcon;

            public contactListHolder(@NonNull View itemView) {
                super(itemView);

                //initializing fields
                username = itemView.findViewById(R.id.userName);
                userDescription = itemView.findViewById(R.id.userDescription);
                profileImage = itemView.findViewById(R.id.user_profile_image);
                onlineIcon = (ImageView) itemView.findViewById(R.id.online);
            }
    }
}