package com.example.chatapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class RequestsFragment extends Fragment {

    private View requestFragmentView;
    private RecyclerView requestList;

    private DatabaseReference requestReference,userReference,contactReference;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public RequestsFragment() {
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
        requestFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        //firebase connection
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        requestReference = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        contactReference = FirebaseDatabase.getInstance().getReference().child("Contact");


        //initializing fields
        requestList = (RecyclerView) requestFragmentView.findViewById(R.id.chatRequest);
        requestList.setLayoutManager(new LinearLayoutManager(getContext()));
        return requestFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> recyclerOptions = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(requestReference.child(currentUserId),Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, RequestViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder holder, int position, @NonNull Contacts model) {

                //initializing buttons
                holder.itemView.findViewById(R.id.accept_Request_Btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.cancel_Request_Btn).setVisibility(View.VISIBLE);

                //getting the user id's in database
                final String userList = getRef(position).getKey();

                DatabaseReference requestTypeReference = getRef(position).child("requestType").getRef();

                requestTypeReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            String requestType = snapshot.getValue().toString();

                            if (requestType.equals("received")){
                                userReference.child(userList).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.hasChild("image")){
                                            final String requestUserImage = snapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserImage).into(holder.profileImage);//load profile photo
                                        }

                                        final String requestUserName = snapshot.child("name").getValue().toString();
                                        final String requestUserDescription = snapshot.child("description").getValue().toString();

                                        holder.userProfileName.setText(requestUserName);
                                        holder.userProfileDescription.setText("Hi, I want to connect with you");


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence sequence[] = new CharSequence[]{
                                                        "Accept",
                                                        "Cancel"
                                                };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle( requestUserName+ " Request");

                                                builder.setItems(sequence, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                        if (which == 0){
                                                            contactReference.child(currentUserId).child(userList).child("Contacts")
                                                                    .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()){
                                                                        contactReference.child(userList).child(currentUserId).child("Contacts")
                                                                                .setValue("Saved")
                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()){
                                                                                    requestReference.child(currentUserId).child(userList)
                                                                                            .removeValue()
                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                                    if (task.isSuccessful()){

                                                                                                        requestReference.child(userList).child(currentUserId)
                                                                                                                .removeValue()
                                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                    @Override
                                                                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                                                                        if (task.isSuccessful()){
                                                                                                                            Toast.makeText(getContext(), "Contact Saved", Toast.LENGTH_SHORT).show();
                                                                                                                        }
                                                                                                                    }
                                                                                                                });
                                                                                                    }
                                                                                                }
                                                                                            });
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            });
                                                        }

                                                        if (which == 1){

                                                            requestReference.child(currentUserId).child(userList)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if (task.isSuccessful()){

                                                                                requestReference.child(userList).child(currentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                if (task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "Request Removed", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });

                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                            else if (requestType.equals("sent")){

                                Button requestSendBtn = holder.itemView.findViewById(R.id.accept_Request_Btn);
                                requestSendBtn.setText("Request Send");

                                holder.itemView.findViewById(R.id.cancel_Request_Btn).setVisibility(View.INVISIBLE);

                                userReference.child(userList).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.hasChild("image")){
                                            final String requestUserImage = snapshot.child("image").getValue().toString();

                                            Picasso.get().load(requestUserImage).into(holder.profileImage);//load profile photo
                                        }

                                        final String requestUserName = snapshot.child("name").getValue().toString();
                                        final String requestUserDescription = snapshot.child("description").getValue().toString();

                                        holder.userProfileName.setText(requestUserName);
                                        holder.userProfileDescription.setText("You are send a request to "+ requestUserName);


                                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence sequence[] = new CharSequence[]{
                                                        "Cancel Request"
                                                };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Already Send a Request");

                                                builder.setItems(sequence, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        if (which == 0){

                                                            requestReference.child(currentUserId).child(userList)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if (task.isSuccessful()){

                                                                                requestReference.child(userList).child(currentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                if (task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "Cancel the chat request", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });

                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
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
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.display_user_layout,parent,false);
                RequestViewHolder viewHolder = new RequestViewHolder(view);
                return viewHolder;
            }
        };

        requestList.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder{

        TextView userProfileName,userProfileDescription;
        CircleImageView profileImage;
        Button acceptBtn, cancelBtn;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            //initializing fields
            userProfileName = itemView.findViewById(R.id.userName);
            userProfileDescription = itemView.findViewById(R.id.userDescription);
            profileImage = itemView.findViewById(R.id.user_profile_image);
            acceptBtn = itemView.findViewById(R.id.accept_Request_Btn);
            cancelBtn = itemView.findViewById(R.id.cancel_Request_Btn);


        }
    }
}