package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;

    private Button LoginButton, PhoneLoginButton;
    private EditText UserEmail, UserPassword;
    private TextView CreateNewAccount , ForgetPassword;

    private DatabaseReference userReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");

        InitializeFields();

        CreateNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegisterActivity(); //method calling (Moving to the register page)
            }
        });

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginToApp();
            }
        });

        //when clicked phone login btn moving to the verification page
        PhoneLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneNumberLoginIntent = new Intent(LoginActivity.this,PhoneNumberLoginActivity.class);
                startActivity(phoneNumberLoginIntent);
            }
        });
    }

    //Initializing all the login fields with casting
    private void InitializeFields() {
        LoginButton = (Button) findViewById(R.id.login_button);
        PhoneLoginButton = (Button) findViewById(R.id.phone_login_button);
        UserEmail = (EditText) findViewById(R.id.login_email);
        UserPassword = (EditText) findViewById(R.id.login_password);
        CreateNewAccount = (TextView) findViewById(R.id.create_new_account);
        ForgetPassword = (TextView) findViewById(R.id.forget_password_link);

        loadingBar = new ProgressDialog(this);
    }


    //Moving to the Main page
    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//can't go back. when clicking back button
        startActivity(mainIntent);
        finish();
    }

    //Moving to the register page
    private void SendUserToRegisterActivity() {
        Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);
    }

    //Allow user to login
    private void LoginToApp() {
        String email = UserEmail.getText().toString();
        String password = UserPassword.getText().toString();

        //Alerts
        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Please enter your email..", Toast.LENGTH_SHORT).show();
        }

        //Alerts
        if (TextUtils.isEmpty(password)){
            Toast.makeText(this, "Please enter your password..", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Sign In");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){

                                String currentUserId = mAuth.getCurrentUser().getUid();
                                String deviceTokenId = FirebaseInstanceId.getInstance().getToken();

                                userReference.child(currentUserId).child("deviceTokenId")
                                        .setValue(deviceTokenId)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if (task.isSuccessful()){
                                                    SendUserToMainActivity();
                                                    Toast.makeText(LoginActivity.this, "Login Successful..", Toast.LENGTH_SHORT).show();//Alerts
                                                    loadingBar.dismiss();
                                                }
                                            }
                                        });
                            }
                            else {
                                String message = task.getException().toString();
                                Toast.makeText(LoginActivity.this, "Error: "+ message, Toast.LENGTH_SHORT).show();//Alerts
                                loadingBar.dismiss();
                            }
                        }
                    });
        }

    }

}