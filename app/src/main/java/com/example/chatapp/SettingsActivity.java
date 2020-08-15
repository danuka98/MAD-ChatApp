package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateProfileBtn;
    private EditText userName, description;
    private CircleImageView profileImage;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference rootReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();

        InitializeFields();//calling InitializeFields method

        userName.setVisibility(View.INVISIBLE);//current username invisible

        updateProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSetting();//calling update setting method
            }
        });

        RetrieveUserInformation();//calling method
    }


    //Initializing all the setting fields
    private void InitializeFields() {
        updateProfileBtn = (Button) findViewById(R.id.update_profile_btn);
        userName = (EditText) findViewById(R.id.set_user_name);
        description = (EditText) findViewById(R.id.set_profile_description);
        profileImage = (CircleImageView) findViewById(R.id.profile_image);
    }

    //updating all the setting
    private void UpdateSetting() {

        String setUserName = userName.getText().toString();
        String setDescription = description.getText().toString();

        if (TextUtils.isEmpty(setUserName)){
            Toast.makeText(this, "Please, Enter your Username..", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(setDescription)){
            Toast.makeText(this, "Please, Enter your Description..", Toast.LENGTH_SHORT).show();
        }
        else{
            HashMap<String,String> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("description", setDescription);

            //checking database and updating settings and alert viewing
            rootReference.child("Users").child(currentUserID).setValue(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                SendUserToMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile Updated..", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                //Exception Error
                                String message = task.getException().toString();
                                Toast.makeText(SettingsActivity.this, "Error:"+ message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    //Moving to the Main page
    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//can't go back. when clicking back button
        startActivity(mainIntent);
        finish();
    }

    //Retrieving user information
    private void RetrieveUserInformation() {
        rootReference.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if ((snapshot.exists()) && (snapshot.hasChild("name") && (snapshot.hasChild("image")))){
                            String retrieveUserName = snapshot.child("name").getValue().toString();
                            String retrieveDescription = snapshot.child("description").getValue().toString();
                            String retrieveProfileImage = snapshot.child("image").getValue().toString();

                            userName.setText(retrieveUserName);
                            description.setText(retrieveDescription);
                        }
                        else if ((snapshot.exists()) && (snapshot.hasChild("name"))){
                            String retrieveUserName = snapshot.child("name").getValue().toString();
                            String retrieveDescription = snapshot.child("description").getValue().toString();

                            userName.setText(retrieveUserName);
                            description.setText(retrieveDescription);
                        }
                        else{
                            userName.setVisibility(View.VISIBLE);//visible username
                            Toast.makeText(SettingsActivity.this, "Please, Update your information...", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}