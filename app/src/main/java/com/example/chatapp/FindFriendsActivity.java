package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView findFriendsList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        //initialize fields
        findFriendsList = (RecyclerView) findViewById(R.id.find_friends_list);
        findFriendsList.setLayoutManager(new LinearLayoutManager(this));

        //action bar with back button
        toolbar = (Toolbar) findViewById(R.id.find_friends_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(databaseReference,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,FindFriends> adapter = new FirebaseRecyclerAdapter<Contacts, FindFriends>(options) {
            @Override //data retrieving
            protected void onBindViewHolder(@NonNull FindFriends holder, final int position, @NonNull Contacts model) {
                holder.userName.setText(model.getName());
                holder.description.setText(model.getDescription());
                Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profileImage);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String user_id = getRef(position).getKey(); // get user id and store into variable

                        Intent userProfileIntent = new Intent(FindFriendsActivity.this,UserProfileActivity.class);
                        userProfileIntent.putExtra("user_id",user_id);
                        startActivity(userProfileIntent);
                    }
                });
            }

            @NonNull
            @Override //user display layout
            public FindFriends onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.display_user_layout,parent,false);
                FindFriends findFriends = new FindFriends(view);
                return findFriends;
            }
        };

        findFriendsList.setAdapter(adapter);
        adapter.startListening();
    }

    //
    public static class FindFriends extends RecyclerView.ViewHolder{
        TextView userName, description;
        CircleImageView profileImage;
        public FindFriends(@NonNull View itemView) {
            super(itemView);

            //initializing id's
            userName = itemView.findViewById(R.id.userName);
            description = itemView.findViewById(R.id.userDescription);
            profileImage = itemView.findViewById(R.id.user_profile_image);
        }
    }
}