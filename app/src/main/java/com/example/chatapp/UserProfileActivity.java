package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserProfileActivity extends AppCompatActivity {

    private String userID, currentState, currentUserID;

    private CircleImageView userProfileImage;
    private TextView userProfileName,userProfileDescription;
    private Button sendMessageButton, cancelMessageButton;

    private DatabaseReference userReference,chatRequestReference, contactReference,notificationReference;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //link to firebase reference
        mAuth = FirebaseAuth.getInstance();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestReference = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        contactReference = FirebaseDatabase.getInstance().getReference().child("Contact");
        notificationReference = FirebaseDatabase.getInstance().getReference().child("Notification");

        //getting user id and display a toast message with user id
        userID = getIntent().getExtras().get("user_id").toString();
        currentUserID = mAuth.getCurrentUser().getUid();

        //initializing fields
        userProfileImage = (CircleImageView) findViewById(R.id.friend_profile_image);
        userProfileName = (TextView) findViewById(R.id.friend_user_name);
        userProfileDescription = (TextView) findViewById(R.id.friend_user_description);
        sendMessageButton = (Button) findViewById(R.id.send_message_request_button);
        cancelMessageButton = (Button) findViewById(R.id.cancel_message_request_button);
        currentState = "new";

        RetrieveUserInformation();
    }

    //retrieve all the data in database
    private void RetrieveUserInformation() {

        userReference.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //when the user setting user profile image
                if ((snapshot.exists()) && (snapshot.hasChild("image"))){
                    String userImage = snapshot.child("image").getValue().toString();
                    String userName = snapshot.child("name").getValue().toString();
                    String userDescription = snapshot.child("description").getValue().toString();

                    Picasso.get().load(userImage).into(userProfileImage);//load profile photo
                    userProfileName.setText(userName);
                    userProfileDescription.setText(userDescription);

                    MessageRequest();
                }
                //user not setting a profile image
                else{
                    String userName = snapshot.child("name").getValue().toString();
                    String userDescription = snapshot.child("description").getValue().toString();

                    userProfileName.setText(userName);
                    userProfileDescription.setText(userDescription);

                    MessageRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //user can send message request to another user
    private void MessageRequest() {

        chatRequestReference.child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.hasChild(userID)){
                            String requestType = snapshot.child(userID).child("requestType").getValue().toString();

                            if (requestType.equals("sent")){
                                currentState = "request_sent";
                                sendMessageButton.setText("Cancel Chat Request");
                            }
                            else if (requestType.equals("received")){
                                currentState = "request_received";
                                sendMessageButton.setText("Accept Chat Request");

                                cancelMessageButton.setVisibility(View.VISIBLE);
                                cancelMessageButton.setEnabled(true);

                                cancelMessageButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        CancelChatRequest();
                                    }
                                });
                            }
                        }
                        else {
                            contactReference.child(currentUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.hasChild(userID)){
                                                currentState = "friends";
                                                sendMessageButton.setText("Remove Contact");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        if (!currentUserID.equals(userID)){
            sendMessageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMessageButton.setEnabled(false);

                    if (currentState.equals("new")){
                        SendChatRequest();//Method calling
                    }

                    if (currentState.equals("request_sent")){
                        CancelChatRequest();//method calling
                    }
                    if (currentState.equals("request_received")){
                        AcceptChatRequest();//method calling
                    }
                    if (currentState.equals("friends")){
                        RemoveContact();//method calling
                    }
                }
            });
        }else {
            sendMessageButton.setVisibility(View.INVISIBLE);//current user profile does not view send message button
        }
    }
    //remove the chat request
    private void RemoveContact() {
        contactReference.child(currentUserID).child(userID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactReference.child(userID).child(currentUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(true);
                                                currentState = "new";
                                                sendMessageButton.setText("Send Message Request");

                                                cancelMessageButton.setVisibility(View.INVISIBLE);
                                                cancelMessageButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    //accept the chat request
    private void AcceptChatRequest() {

        contactReference.child(currentUserID).child(userID)
                .child("Contact").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactReference.child(userID).child(currentUserID)
                                    .child("Contact").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                chatRequestReference.child(currentUserID).child(userID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()){
                                                                    chatRequestReference.child(userID).child(currentUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    sendMessageButton.setEnabled(true);
                                                                                    currentState = "friends";
                                                                                    sendMessageButton.setText("Remove Contact");

                                                                                    cancelMessageButton.setVisibility(View.INVISIBLE);
                                                                                    cancelMessageButton.setEnabled(false);
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

    //cancel the chat request
    private void CancelChatRequest() {

        chatRequestReference.child(currentUserID).child(userID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestReference.child(userID).child(currentUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(true);
                                                currentState = "new";
                                                sendMessageButton.setText("Send Message Request");

                                                cancelMessageButton.setVisibility(View.INVISIBLE);
                                                cancelMessageButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    //send friend requests
    private void SendChatRequest() {
        chatRequestReference.child(currentUserID).child(userID)
                .child("requestType").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()){
                            chatRequestReference.child(userID).child(currentUserID)
                                    .child("requestType").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){

                                                HashMap<String,String> chatNotification = new HashMap<>();
                                                chatNotification.put("from",currentUserID);
                                                chatNotification.put("type","request");

                                                notificationReference.child(userID).push().setValue(chatNotification).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if (task.isSuccessful()){

                                                            sendMessageButton.setEnabled(true);
                                                            currentState = "request_sent";
                                                            sendMessageButton.setText("Cancel Chat Request");
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