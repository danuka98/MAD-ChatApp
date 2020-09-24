package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateProfileBtn;
    private EditText userName, description;
    private CircleImageView profileImage;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference rootReference;

    private static final int Gallery = 1;
    private StorageReference profileImageReference;
    private ProgressDialog progressDialog;

    private Toolbar settingActionBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();
        profileImageReference = FirebaseStorage.getInstance().getReference().child("Profile Images");

        InitializeFields();//calling InitializeFields method

        userName.setVisibility(View.INVISIBLE);//current username invisible

        updateProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSetting();//calling update setting method
            }
        });

        RetrieveUserInformation();//calling method

        //when the clicked profile image,it's move to the phone gallery
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, Gallery);

            }
        });
    }

    //when the user select the image, here we can crop the image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Gallery && resultCode == RESULT_OK && data!= null){

            Uri imageUri = data.getData();

            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }
        //crop image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK){
                progressDialog.setTitle("Set profile Image");
                progressDialog.setMessage("Your profile image is updating..");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                Uri resultUri = result.getUri();

                final StorageReference filePath = profileImageReference.child(currentUserID + ".jpg");//new profile image created

                //upload image to firebase storage
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(SettingsActivity.this, "Profile Image uploaded...", Toast.LENGTH_SHORT).show();

                            final String downloadUri = filePath.getDownloadUrl().toString();

                            rootReference.child("Users").child(currentUserID).child("image")
                                    .setValue(downloadUri)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                Toast.makeText(SettingsActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();
                                            }
                                            else {
                                                String message = task.getException().toString();
                                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                progressDialog.dismiss();
                                            }
                                        }
                                    });
                        }
                        else {
                            String message = task.getException().toString();
                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    }
                });
            }
        }
    }

    //Initializing all the setting fields
    private void InitializeFields() {
        updateProfileBtn = (Button) findViewById(R.id.update_profile_btn);
        userName = (EditText) findViewById(R.id.set_user_name);
        description = (EditText) findViewById(R.id.set_profile_description);
        profileImage = (CircleImageView) findViewById(R.id.profile_image);
        progressDialog = new ProgressDialog(this);

        settingActionBar = (Toolbar) findViewById(R.id.setting_actionbar);
        setSupportActionBar(settingActionBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Profile Settings");
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
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", currentUserID);
            profileMap.put("name", setUserName);
            profileMap.put("description", setDescription);

            //checking database and updating settings and alert viewing
            rootReference.child("Users").child(currentUserID).updateChildren(profileMap)
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
                            Picasso.get().load(retrieveProfileImage).into(profileImage);
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