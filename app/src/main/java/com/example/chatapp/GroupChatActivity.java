package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class GroupChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageButton sendBtn;
    private EditText inputMessage;
    private ScrollView scrollView;
    private TextView displayMessage;

    private FirebaseAuth mAuth;
    private DatabaseReference userReference, groupReference , groupMessageKeyReference;

    private String currentGroupName, currentUserName, currentUserID , currentDate, currentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        currentGroupName = getIntent().getExtras().get("groupName").toString();//getting group name
        Toast.makeText(GroupChatActivity.this, currentGroupName, Toast.LENGTH_SHORT).show();//display

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();//getting user id
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");//getting reference of users
        groupReference = FirebaseDatabase.getInstance().getReference().child("Groups").child(currentGroupName);//getting reference of groups


        InitializeFields();//method call

        RetrieveUserInformation();//method call

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMessage();//method call

                inputMessage.setText("");//user input message

                scrollView.fullScroll(ScrollView.FOCUS_DOWN);//scrolling message
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        groupReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                if (snapshot.exists()){
                    DisplayMessage(snapshot);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                
                if (snapshot.exists()){
                    DisplayMessage(snapshot);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }



    //initializing all the fields
    private void InitializeFields() {
        toolbar = (Toolbar) findViewById(R.id.group_chat_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(currentGroupName);

        sendBtn = (ImageButton) findViewById(R.id.groupMessageBtn);
        inputMessage = (EditText) findViewById(R.id.groupMessage);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        displayMessage = (TextView) findViewById(R.id.chat_display);

    }

    //checking user info and dta retrieve
    private void RetrieveUserInformation() {
        userReference.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check user online
                if (snapshot.exists()){
                    currentUserName = snapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //save message details to database
    private void saveMessage() {
        String message = inputMessage.getText().toString();
        String messageKey = groupReference.push().getKey();//get message key

        if (TextUtils.isEmpty(message)){
            Toast.makeText(this, "please, write your message..", Toast.LENGTH_SHORT).show();
        }
        else {
            //setting date
            Calendar calendarDate = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            currentDate = dateFormat.format(calendarDate.getTime());

            //setting time
            Calendar calendarTime = Calendar.getInstance();
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");
            currentTime = timeFormat.format(calendarTime.getTime());

            HashMap<String, Object> groupMessageKey = new HashMap<>();
            groupReference.updateChildren(groupMessageKey);

            groupMessageKeyReference = groupReference.child(messageKey);//getting message key and store

            HashMap<String, Object> messageInformation = new HashMap<>();

            //adding to database
            messageInformation.put("name", currentUserName);
            messageInformation.put("message", message);
            messageInformation.put("date", currentDate);
            messageInformation.put("time", currentTime);

            groupMessageKeyReference.updateChildren(messageInformation);
        }
    }

    //displaying all message
    private void DisplayMessage(DataSnapshot snapshot) {

        Iterator iterator = snapshot.getChildren().iterator();//getting all messages

        //getting messages line by line
        while (iterator.hasNext()){

            String chatDate = (String) ((DataSnapshot)iterator.next()).getValue();
            String chatMessage = (String) ((DataSnapshot)iterator.next()).getValue();
            String chatName = (String) ((DataSnapshot)iterator.next()).getValue();
            String chatTime = (String) ((DataSnapshot)iterator.next()).getValue();

            displayMessage.append(chatName + ":\n" + chatMessage + "\n" + chatTime + "    " + chatDate + "\n\n\n");//displaying messages

            scrollView.fullScroll(ScrollView.FOCUS_DOWN);//scrolling message
        }
    }

}