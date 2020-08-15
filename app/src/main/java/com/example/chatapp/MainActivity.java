package com.example.chatapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private TabsAccessorAdapter tabsAccessorAdapter;

    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private DatabaseReference rootReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        rootReference = FirebaseDatabase.getInstance().getReference();

        //Toolbar
        mToolbar = (Toolbar) findViewById(R.id.main_page_actionbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("ChatApp");

        viewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        tabsAccessorAdapter = new TabsAccessorAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabsAccessorAdapter);

        tabLayout = (TabLayout) findViewById(R.id.main_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }


    @Override
    protected void onStart() {
        super.onStart();

        //if current users are null,then moving to the login page
        if(currentUser == null){
            SendUserToLoginActivity();
        }
        //current users are already existence,
        else{
            UserExistence();
        }
    }


    //Moving to the login page
    private void SendUserToLoginActivity() {

        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//can't go back. when clicking back button
        startActivity(loginIntent);
        finish();
    }

    //Moving to the setting page
    private void SendUserToSettingActivity() {

        Intent settingIntent = new Intent(MainActivity.this,SettingsActivity.class);
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//can't go back. when clicking back button
        startActivity(settingIntent);
        finish();
    }

    //calling menu option
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu, menu);

        return true;
    }

    //setting up menu option
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.logout){
            mAuth.signOut();
            SendUserToLoginActivity();//method call
        }
        if (item.getItemId() == R.id.find_friends){

        }
        if (item.getItemId() == R.id.create_group){
            CreateGroup();//method call
        }
        if (item.getItemId() == R.id.settings){
            SendUserToSettingActivity();//method call
        }
        return true;
    }

    private void UserExistence() {

        String currentUserID = mAuth.getCurrentUser().getUid();//retrieving user data

        //checking database of current users
        rootReference.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if ((snapshot.child("name").exists())){

                    Toast.makeText(MainActivity.this, "Welcome To  ChatApp..", Toast.LENGTH_SHORT).show();
                }
                else{
                    SendUserToSettingActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //create group
    private void CreateGroup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
        builder.setTitle("Enter Group Name:");

        final EditText groupName = new EditText(MainActivity.this);
        groupName.setHint("Group name");
        builder.setView(groupName);

        //creating a button for create group
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String grpName = groupName.getText().toString();

                if(TextUtils.isEmpty(grpName)){
                    Toast.makeText(MainActivity.this, "Please, enter group name..", Toast.LENGTH_SHORT).show();
                }
                else {
                    createNewGroup(grpName);//create group method calling
                }
            }
        });

        //creating a button for cancel group
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    //when create a group, showing group is created
    private void createNewGroup(final String grpName) {
        rootReference.child("Groups").child(grpName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(MainActivity.this, grpName + " group is Created", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}