package com.example.chatapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.data.DataBufferIterator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> userMessagesList;

    private FirebaseAuth mAuth;
    private DatabaseReference userReference;

    public MessageAdapter (List<Messages> userMessagesList){
        this.userMessagesList = userMessagesList;
    }

    //initializing fields
    public class MessageViewHolder extends RecyclerView.ViewHolder{

        public TextView receiverMessage , senderMessage;
        public CircleImageView receiverImage;
        public ImageView senderImageFile,receiverImageFile;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            receiverMessage = (TextView) itemView.findViewById(R.id.receiver_message);
            senderMessage = (TextView) itemView.findViewById(R.id.sender_message);
            receiverImage = (CircleImageView) itemView.findViewById(R.id.message_profile_image);
            senderImageFile = (ImageView) itemView.findViewById(R.id.sender_image_file);
            receiverImageFile = (ImageView) itemView.findViewById(R.id.receiver_image_file);

        }
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout,parent,false);

        mAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, final int position) {

        String senderID = mAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String userFromID = messages.getFrom();
        String messageType = messages.getType();

        userReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userFromID);

        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.hasChild("image")){
                    String receiverProfileImage = snapshot.child("image").getValue().toString();

                    Picasso.get().load(receiverProfileImage).placeholder(R.drawable.profile_image).into(holder.receiverImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //whole layout will be disappear
        holder.receiverMessage.setVisibility(View.GONE);
        holder.receiverImage.setVisibility(View.GONE);
        holder.senderMessage.setVisibility(View.GONE);
        holder.senderImageFile.setVisibility(View.GONE);
        holder.receiverImageFile.setVisibility(View.GONE);

        if (messageType.equals("text")){

            if (userFromID.equals(senderID)){
                holder.senderMessage.setVisibility(View.VISIBLE);
                holder.senderMessage.setBackgroundResource(R.drawable.sender_message_layout);
                holder.senderMessage.setText(messages.getMessage() + "   \t" + messages.getTime());
            }
            else {
                holder.receiverImage.setVisibility(View.VISIBLE);
                holder.receiverMessage.setVisibility(View.VISIBLE);

                holder.receiverMessage.setBackgroundResource(R.drawable.receiver_message_layout);
                holder.receiverMessage.setText(messages.getMessage() + "   \t" + messages.getTime());
            }
        }

        else if (messageType.equals("image")){

            if (userFromID.equals(senderID)){

                holder.senderImageFile.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.senderImageFile);
            }
            else {
                holder.receiverImage.setVisibility(View.VISIBLE);
                holder.receiverImageFile.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.receiverImageFile);
            }
        }
        else if ((messageType.equals("pdf")) || (messageType.equals("docx"))){

            if (userFromID.equals(senderID)){
                holder.senderImageFile.setVisibility(View.VISIBLE);

                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/chatapp-61773.appspot.com/o/Images%2Ffile.png?alt=media&token=ba3e5492-2eb3-4f0c-8f31-953311821a42")
                        .into(holder.senderImageFile);
            }
            else{
                holder.receiverImage.setVisibility(View.VISIBLE);
                holder.receiverImageFile.setVisibility(View.VISIBLE);

                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/chatapp-61773.appspot.com/o/Images%2Ffile.png?alt=media&token=ba3e5492-2eb3-4f0c-8f31-953311821a42")
                        .into(holder.receiverImageFile);
            }
        }

        if (userFromID.equals(senderID)){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (userMessagesList.get(position).getType().equals("docx") || userMessagesList.get(position).getType().equals("pdf")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "DELETE FOR EVERYONE",
                                        "DOWNLOAD AND VIEW THIS DOCUMENT",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteSenderMessage(position, holder);
                                }
                                else if (which == 1){
                                    deleteMessageForEveryone(position, holder);
                                }
                                else if (which == 2){

                                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });

                        builder.show();
                    }

                    else if (userMessagesList.get(position).getType().equals("text")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "DELETE FOR EVERYONE",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteSenderMessage(position, holder);
                                }
                                else if (which == 1){
                                    deleteMessageForEveryone(position, holder);
                                }
                            }
                        });

                        builder.show();
                    }

                    else if (userMessagesList.get(position).getType().equals("image")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "DELETE FOR EVERYONE",
                                        "VIEW THIS IMAGE",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteSenderMessage(position, holder);
                                }
                                else if (which == 1){
                                    deleteMessageForEveryone(position, holder);
                                }
                                else if (which == 2){

                                    Intent intent = new Intent(holder.itemView.getContext(), ViewImageActivity.class);
                                    intent.putExtra("url",userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });

                        builder.show();
                    }
                }
            });
        }

        else{

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (userMessagesList.get(position).getType().equals("docx") || userMessagesList.get(position).getType().equals("pdf")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "DOWNLOAD AND VIEW THIS DOCUMENT",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteReceiverMessage(position, holder);
                                }
                                else if (which == 1){
                                    Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });

                        builder.show();
                    }

                    else if (userMessagesList.get(position).getType().equals("text")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteReceiverMessage(position, holder);
                                }
                            }
                        });

                        builder.show();
                    }

                    else if (userMessagesList.get(position).getType().equals("image")){

                        CharSequence sequence[] = new CharSequence[]
                                {
                                        "DELETE FOR ME",
                                        "VIEW THIS IMAGE",
                                        "CANCEL"
                                };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Do You Want To Delete?");

                        builder.setItems(sequence, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (which == 0){
                                    deleteReceiverMessage(position, holder);
                                }
                                else if (which == 1){

                                    Intent intent = new Intent(holder.itemView.getContext(), ViewImageActivity.class);
                                    intent.putExtra("url",userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });

                        builder.show();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {

        return  userMessagesList.size();
    }

    private void deleteSenderMessage(final int position , final MessageViewHolder holder){

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("Messages")
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()){

                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred..", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteReceiverMessage(final int position , final MessageViewHolder holder){

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()){

                    Toast.makeText(holder.itemView.getContext(), "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred..", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void deleteMessageForEveryone(final int position , final MessageViewHolder holder){

        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()){

                    databaseReference.child("Messages")
                            .child(userMessagesList.get(position).getFrom())
                            .child(userMessagesList.get(position).getTo())
                            .child(userMessagesList.get(position).getMessageId())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()){

                                Toast.makeText(holder.itemView.getContext(), "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
                else {
                    Toast.makeText(holder.itemView.getContext(), "Error Occurred..", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
