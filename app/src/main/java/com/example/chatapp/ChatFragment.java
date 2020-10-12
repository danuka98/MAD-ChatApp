package com.example.chatapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFragment extends Fragment {

    private View chatView;
    private RecyclerView chatList;

    private DatabaseReference chatsReference ,userReference;
    private FirebaseAuth mAuth;
    private static String currentUserId;

    private SinchClient sinchClient;
    private Call call;

    public ChatFragment() {
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
        chatView = inflater.inflate(R.layout.fragment_chat, container, false);

        //Firebase connection
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        chatsReference = FirebaseDatabase.getInstance().getReference().child("Contact").child(currentUserId);
        userReference = FirebaseDatabase.getInstance().getReference().child("Users");

        //Retrieve all chats
        chatList = (RecyclerView) chatView.findViewById(R.id.list_of_chats);
        chatList.setLayoutManager(new LinearLayoutManager(getContext()));


        sinchClient = Sinch.getSinchClientBuilder()
                .context(getContext().getApplicationContext())
                .applicationKey("6bf780d1-3ac7-4a1f-a79f-439c69ad76d5")
                .applicationSecret("HpFAWLQP3EmdXU8E4N2YNg==")
                .environmentHost("clientapi.sinch.com")
                .userId(currentUserId)
                .build();

        sinchClient.setSupportCalling(true);
        sinchClient.setSupportActiveConnectionInBackground(true);

        sinchClient.getCallClient().addCallClientListener(new CallClientListener(){

            @Override
            public void onIncomingCall(CallClient callClient, Call incomingCall) {

                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("CALLING");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Answer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        CallClient callClient = sinchClient.getCallClient();
                        Call call = callClient.callUser(currentUserId);

                        call.addCallListener(new SinchCallListener());
                        call = incomingCall;
                        call.answer();

                        Toast.makeText(getActivity(), "Call is Started", Toast.LENGTH_SHORT).show();
                    }
                });

                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        call.hangup();
                    }
                });

                alertDialog.show();
            }
        });

        sinchClient.start();

        return chatView;


    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallProgressing(Call call) {
            Toast.makeText(getActivity(), "Ringing...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEstablished(Call call) {
            Toast.makeText(getActivity(), "Call established", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCallEnded(Call endedCall) {
            Toast.makeText(getActivity(), "Call ended", Toast.LENGTH_SHORT).show();
            call = null;
            endedCall.hangup();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> list) {

        }
    }

//    private class SinchCallClientListener implements CallClientListener {
//
//        @Override
//        public void onIncomingCall(CallClient callClient, Call incomingCall) {
//
//            AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
//            alertDialog.setTitle("CALLING");
//            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Answer", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    call = incomingCall;
//                    call.answer();
//                    call.addCallListener(new SinchCallListener());
//                    Toast.makeText(getActivity(), "Call is Started", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Reject", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                    call.hangup();
//                }
//            });
//
//            alertDialog.show();
//        }
//    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> recyclerOptions = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatsReference, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts ,chatViewHolder> recyclerAdapter = new FirebaseRecyclerAdapter<Contacts, chatViewHolder>(recyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final chatViewHolder holder, int position, @NonNull Contacts model) {

                //get each key value
                final String userId = getRef(position).getKey();
                final String[] retrieveImage = {"defaultImage"};

                userReference.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()){
                            if (snapshot.hasChild("image")){
                                retrieveImage[0] = snapshot.child("image").getValue().toString();
                                Picasso.get().load(retrieveImage[0]).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            }

                            final String retrieveName = snapshot.child("name").getValue().toString();
                            final String retrieveDescription = snapshot.child("description").getValue().toString();

                            holder.userName.setText(retrieveName);

                            if (snapshot.child("onlineStatus").hasChild("status")){

                                String status = snapshot.child("onlineStatus").child("status").getValue().toString();
                                String date = snapshot.child("onlineStatus").child("date").getValue().toString();
                                String time = snapshot.child("onlineStatus").child("time").getValue().toString();

                                if (status.equals("online")){
                                    holder.userDescription.setText("online");
                                }
                                else if (status.equals("offline")){
                                    holder.userDescription.setText("Last Seen : " + date +" " +time);
                                }
                            }
                            else {
                                holder.userDescription.setText("offline");
                            }

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("user_id",userId);
                                    chatIntent.putExtra("user_name",retrieveName);
                                    chatIntent.putExtra("user_image", retrieveImage[0]);
                                    startActivity(chatIntent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @NonNull
            @Override
            public chatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.display_user_layout,parent,false);

                return new chatViewHolder(view);
            }
        };

        chatList.setAdapter(recyclerAdapter);
        recyclerAdapter.startListening();
    }

    public class chatViewHolder extends RecyclerView.ViewHolder{

        CircleImageView profileImage;
        TextView userName,userDescription;
        ImageButton callBtn;

        public chatViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.user_profile_image);
            userName = itemView.findViewById(R.id.userName);
            userDescription = itemView.findViewById(R.id.userDescription);
            callBtn = itemView.findViewById(R.id.call_Btn);

            callBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callUser();
                }
            });

        }

    }

    private void callUser() {

        if (call == null){
            call = sinchClient.getCallClient().callUser(currentUserId);
            call.addCallListener(new SinchCallListener());

            openCallerDialog(call);
        }
    }

    private void openCallerDialog(final Call call) {

        AlertDialog alertDialogCall = new AlertDialog.Builder(getContext()).create();
        alertDialogCall.setTitle("ALERT");
        alertDialogCall.setMessage("CALLING");
        alertDialogCall.setButton(AlertDialog.BUTTON_NEUTRAL, "Hang up", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                call.hangup();
            }
        });

        alertDialogCall.show();
    }

}