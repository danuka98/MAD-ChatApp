package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.SinchClient;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String receiverID , userName, userImage ,senderID;

    private TextView chatProfileName,chatProfileLastSeen;
    private CircleImageView chatProfileImage;

    private Toolbar chatActionBar;
    private FirebaseAuth mAuth;
    private DatabaseReference rootReference;

    private ImageButton sendMessageBtn, sendFileBtn;
    private EditText textMessage;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessageList;

    private String currentTime, currentDate;
    private String check = "", url = "";
    private Uri fileUri;
    private StorageTask uploadFileTask;

    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        senderID = mAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();

        receiverID = getIntent().getExtras().get("user_id").toString();
        userName = getIntent().getExtras().get("user_name").toString();
        userImage = getIntent().getExtras().get("user_image").toString();

        InitializeFields();

        chatProfileName.setText(userName);
        Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(chatProfileImage);

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        displayLastSeen();

        sendFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence sequence[] = new CharSequence[]
                        {
                                "Images",
                                "PDF Files",
                                "Word Files(.doc)"
                        };
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select the File");

                builder.setItems(sequence, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (which == 0){
                            check = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select Image File"),438);
                        }
                        if (which == 1){
                            check = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent, "Select PDF File"),438);
                        }
                        if (which == 2){
                            check = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent, "Select Word(.docx) File"),438);
                        }
                    }
                });

                builder.show();
            }
        });


    }


    private void InitializeFields() {

        chatActionBar = (Toolbar) findViewById(R.id.chat_actionbar);
        setSupportActionBar(chatActionBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.chatbar_layout , null);
        actionBar.setCustomView(actionBarView);


        chatProfileImage = (CircleImageView) findViewById(R.id.chat_profile_image);
        chatProfileName = (TextView) findViewById(R.id.chat_profile_name);
        chatProfileLastSeen = (TextView) findViewById(R.id.chat_profile_last_seen);

        sendMessageBtn = (ImageButton) findViewById(R.id.send_message_btn);
        sendFileBtn = (ImageButton) findViewById(R.id.send_file_btn);
        textMessage = (EditText) findViewById(R.id.type_message);

        messageAdapter = new MessageAdapter(messagesList);
        userMessageList = (RecyclerView) findViewById(R.id.message_list);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessageList.setLayoutManager(linearLayoutManager);
        userMessageList.setAdapter(messageAdapter);

        progressDialog = new ProgressDialog(this);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy");
        currentDate = dateFormat.format(calendar.getTime());

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm");
        currentTime = timeFormat.format(calendar.getTime());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null){

            progressDialog.setTitle("Sending Image Files");
            progressDialog.setMessage("Please wait, Image is sending..");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            fileUri = data.getData();

            if (!check.equals("image")){

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Documents");

                final String senderReference = "Messages/" + senderID + "/" + receiverID;
                final String receiverReference = "Messages/" + receiverID + "/" + senderID;

                //creating a key
                DatabaseReference messageReference = rootReference.child("Messages").child(senderID).child(receiverID).push();

                final String messageID = messageReference.getKey();//store the key

                final StorageReference filePath = storageReference.child(messageID + "." + check);

                filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if (task.isSuccessful()){

                            Map messageImageBody = new HashMap();
                            messageImageBody.put("message",task.getResult().getStorage().getDownloadUrl().toString());
                            messageImageBody.put("name",fileUri.getLastPathSegment());
                            messageImageBody.put("type",check);
                            messageImageBody.put("from",senderID);
                            messageImageBody.put("to",receiverID);
                            messageImageBody.put("messageId",messageID);
                            messageImageBody.put("time",currentTime);
                            messageImageBody.put("date",currentDate);

                            Map messageDetails = new HashMap();
                            messageDetails.put(senderReference + "/" +messageID,messageImageBody);
                            messageDetails.put(receiverReference + "/" +messageID,messageImageBody);

                            rootReference.updateChildren(messageDetails);
                            progressDialog.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        
                        progressDialog.dismiss();
                        Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {

                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        progressDialog.setMessage((int) progress + " % Uploading....");
                    }
                });
            }
            else if (check.equals("image")){

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Images");

                final String senderReference = "Messages/" + senderID + "/" + receiverID;
                final String receiverReference = "Messages/" + receiverID + "/" + senderID;

                //creating a key
                DatabaseReference messageReference = rootReference.child("Messages").child(senderID).child(receiverID).push();

                final String messageID = messageReference.getKey();//store the key

                final StorageReference filePath = storageReference.child(messageID + "." + "jpg");

                uploadFileTask = filePath.putFile(fileUri);

                uploadFileTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {

                        if (!task.isSuccessful()){
                            throw task.getException();
                        }

                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        if (task.isSuccessful()){
                            Uri downloadUrl = task.getResult();
                            url = downloadUrl.toString();

                            Map messageImageBody = new HashMap();
                            messageImageBody.put("message",url);
                            messageImageBody.put("name",fileUri.getLastPathSegment());
                            messageImageBody.put("type",check);
                            messageImageBody.put("from",senderID);
                            messageImageBody.put("to",receiverID);
                            messageImageBody.put("messageId",messageID);
                            messageImageBody.put("time",currentTime);
                            messageImageBody.put("date",currentDate);

                            Map messageDetails = new HashMap();
                            messageDetails.put(senderReference + "/" +messageID,messageImageBody);
                            messageDetails.put(receiverReference + "/" +messageID,messageImageBody);

                            rootReference.updateChildren(messageDetails).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {

                                    if (task.isSuccessful()){
                                        progressDialog.dismiss();
                                        Toast.makeText(ChatActivity.this, "Message Send", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        progressDialog.dismiss();
                                        String exception = task.getException().toString();
                                        Toast.makeText(ChatActivity.this, "Error: "+exception, Toast.LENGTH_SHORT).show();
                                    }
                                    textMessage.setText("");
                                }
                            });
                        }
                    }
                });

            }
            else{
                progressDialog.dismiss();
                String message = uploadFileTask.getException().toString();
                Toast.makeText(this, "Error : "+ message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //displaying user in online or offline
    private void displayLastSeen(){
        rootReference.child("Users").child(receiverID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.child("onlineStatus").hasChild("status")){

                    String status = snapshot.child("onlineStatus").child("status").getValue().toString();
                    String date = snapshot.child("onlineStatus").child("date").getValue().toString();
                    String time = snapshot.child("onlineStatus").child("time").getValue().toString();

                    if (status.equals("online")){
                        chatProfileLastSeen.setText("online");
                    }
                    else if (status.equals("offline")){
                        chatProfileLastSeen.setText("Last Seen : " + date +" " +time);
                    }
                }
                else {
                    chatProfileLastSeen.setText("offline");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        rootReference.child("Messages").child(senderID).child(receiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot,String previousChildName) {

                        Messages m = snapshot.getValue(Messages.class);
                        messagesList.add(m);

                        messageAdapter.notifyDataSetChanged();

                        //scrolling smoothly
                        userMessageList.smoothScrollToPosition(userMessageList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot,String previousChildName) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });
    }

    private void sendMessage() {

        String inputMessage = textMessage.getText().toString();

        if (TextUtils.isEmpty(inputMessage)){
            Toast.makeText(this, "Type your message..", Toast.LENGTH_SHORT).show();
        }
        else {
            String senderReference = "Messages/" + senderID + "/" + receiverID;
            String receiverReference = "Messages/" + receiverID + "/" + senderID;

            //creating a key
            DatabaseReference messageReference = rootReference.child("Messages").child(senderID).child(receiverID).push();

            String messageID = messageReference.getKey();//store the key

            Map messageBody = new HashMap();
            messageBody.put("message",inputMessage);
            messageBody.put("type","text");
            messageBody.put("from",senderID);
            messageBody.put("to",receiverID);
            messageBody.put("messageId",messageID);
            messageBody.put("time",currentTime);
            messageBody.put("date",currentDate);

            Map messageDetails = new HashMap();
            messageDetails.put(senderReference + "/" +messageID,messageBody);
            messageDetails.put(receiverReference + "/" +messageID,messageBody);

            rootReference.updateChildren(messageDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if (task.isSuccessful()){
                        Toast.makeText(ChatActivity.this, "Message Send", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        String exception = task.getException().toString();
                        Toast.makeText(ChatActivity.this, "Error: "+exception, Toast.LENGTH_SHORT).show();
                    }
                    textMessage.setText("");
                }
            });
        }
    }

}