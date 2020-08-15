package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneNumberLoginActivity extends AppCompatActivity {

    private Button generateCodeBtn, verifyNumberBtn;
    private EditText inputPhoneNumber, inputOTP_code;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number_login);

        mAuth = FirebaseAuth.getInstance();

        //initializing fields
        generateCodeBtn = (Button) findViewById(R.id.generate_code);
        verifyNumberBtn = (Button) findViewById(R.id.verify_number);
        inputPhoneNumber = (EditText) findViewById(R.id.phone_number);
        inputOTP_code = (EditText) findViewById(R.id.OTP_code);
        loadingBar = new ProgressDialog(this);

        //when clicked send verification code button
        generateCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phoneNumber = inputPhoneNumber.getText().toString();
                if(TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(PhoneNumberLoginActivity.this, "Please, Enter your phone number..", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("Please wait,we are authenticating your phone number...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneNumberLoginActivity.this,               // Activity (for callback binding)
                            callbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });

        verifyNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String OTPcode = inputOTP_code.getText().toString();

                if (TextUtils.isEmpty(OTPcode)){
                    Toast.makeText(PhoneNumberLoginActivity.this, "Please enter verification code..", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingBar.setTitle("Code Verification");
                    loadingBar.setMessage("Please wait,we are verifying your phone number...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, OTPcode);
                    signInWithPhoneAuthCredential(credential);//check code is wrong or correct
                }
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                signInWithPhoneAuthCredential(phoneAuthCredential);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                loadingBar.dismiss();
                Toast.makeText(PhoneNumberLoginActivity.this, "Invalid Phone Number, Please enter correct phone number with country code...", Toast.LENGTH_SHORT).show();

                //Visible two fields
                verifyNumberBtn.setVisibility(View.VISIBLE);
                inputOTP_code.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();
                Toast.makeText(PhoneNumberLoginActivity.this, "Code has been send.Please check your phone...", Toast.LENGTH_SHORT).show();

                //Visible two fields
                verifyNumberBtn.setVisibility(View.VISIBLE);
                inputOTP_code.setVisibility(View.VISIBLE);

            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            loadingBar.dismiss();
                            Toast.makeText(PhoneNumberLoginActivity.this, "Logged in successful..", Toast.LENGTH_SHORT).show();
                            SendUserToMainActivity();

                        } else {
                            // Sign in failed, display a message and update the UI
                            String message = task.getException().toString();
                            Toast.makeText(PhoneNumberLoginActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //Moving to the Main page
    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(PhoneNumberLoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//can't go back. when clicking back button
        startActivity(mainIntent);
        finish();
    }
}