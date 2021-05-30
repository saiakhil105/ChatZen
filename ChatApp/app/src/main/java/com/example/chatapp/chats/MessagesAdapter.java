package com.example.chatapp.chats;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private final Context context;
    private final List<MessageModel> messageList;
    private ActionMode actionMode;
    private RelativeLayout selectedView;

    public MessagesAdapter(Context context, List<MessageModel> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessagesAdapter.MessageViewHolder holder, int position) {
        final MessageModel message = messageList.get(position);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String fromUserId = message.getMessageFrom();
        SimpleDateFormat sfd = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String dateTime = sfd.format(new Date(message.getMessageTime()));
        String[] splitString = dateTime.split(" ");
        final String messageTime = splitString[1];


        if (fromUserId.equals(currentUserId)) {

            if (message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                holder.llSent.setVisibility(View.VISIBLE);
                holder.llSentImage.setVisibility(View.GONE);
            } else {
                holder.llSent.setVisibility(View.GONE);
                holder.llSentImage.setVisibility(View.VISIBLE);
            }

            holder.llReceived.setVisibility(View.GONE);
            holder.llReceivedImage.setVisibility(View.GONE);

            holder.tvSentMessage.setText(message.getMessage());
            holder.tvSentMessageTime.setText(messageTime);
            holder.tvImageSentTime.setText(messageTime);
            Glide.with(context)
                    .load(message.getMessage())
                    .placeholder(R.drawable.loadingcircle)
                    .error(R.drawable.ic_baseline_image_24)
                    .into(holder.ivSent);
        } else {

            if (message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                holder.llReceived.setVisibility(View.VISIBLE);
                holder.llReceivedImage.setVisibility(View.GONE);
            } else {
                holder.llReceived.setVisibility(View.GONE);
                holder.llReceivedImage.setVisibility(View.VISIBLE);
            }

            holder.llSent.setVisibility(View.GONE);
            holder.llSentImage.setVisibility(View.GONE);

            holder.tvReceiveMessage.setText(message.getMessage());
            holder.tvReceiveMessageTime.setText(messageTime);
            holder.tvImageReceiveTime.setText(messageTime);
            Glide.with(context)
                    .load(message.getMessage())
                    .placeholder(R.drawable.loadingcircle)
                    .error(R.drawable.ic_baseline_image_24)
                    .into(holder.ivReceived);
        }

        holder.rlMessage.setTag(R.id.TAG_MESSAGE, message.getMessage());
        holder.rlMessage.setTag(R.id.TAG_MESSAGE_ID, message.getMessageId());
        holder.rlMessage.setTag(R.id.TAG_MESSAGE_TYPE, message.getMessageType());


        holder.rlMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageType = view.getTag(R.id.TAG_MESSAGE_TYPE).toString();
                Uri uri = Uri.parse(view.getTag(R.id.TAG_MESSAGE).toString());
                if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "video/mp4");
                    context.startActivity(intent);
                } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "image/mp4");
                    context.startActivity(intent);
                }
            }
        });

        holder.rlMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(actionMode!=null)
                    return false;

                selectedView = holder.rlMessage;

                actionMode = ((AppCompatActivity)context).startSupportActionMode(actionModeCallBack);

                holder.rlMessage.setBackgroundColor(context.getResources().getColor(R.color.colorAccent));

                return  true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout llSent;
        private final LinearLayout llReceived;
        private final LinearLayout llSentImage;
        private final LinearLayout llReceivedImage;
        private final TextView tvSentMessage;
        private final TextView tvSentMessageTime;
        private final TextView tvReceiveMessage;
        private final TextView tvReceiveMessageTime;
        private final ImageView ivSent;
        private final ImageView ivReceived;
        private final TextView tvImageSentTime;
        private final TextView tvImageReceiveTime;
        private final RelativeLayout rlMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            llSent = itemView.findViewById(R.id.llSent);
            llReceived = itemView.findViewById(R.id.llReceived);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);
            tvReceiveMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceiveMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);
            rlMessage = itemView.findViewById(R.id.rlMessage);
            llSentImage = itemView.findViewById(R.id.llsentIamge);
            llReceivedImage = itemView.findViewById(R.id.llReceivedImage);
            ivSent = itemView.findViewById(R.id.ivSent);
            ivReceived = itemView.findViewById(R.id.ivReceive);
            tvImageReceiveTime = itemView.findViewById(R.id.tvReceivedImageTime);
            tvImageSentTime = itemView.findViewById(R.id.tvSentImageTime);

        }
    }

    public ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_chat_option, menu);

            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            if(selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)){
                MenuItem itemDownload = menu.findItem(R.id.mnu_download);
                itemDownload.setVisible(false);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

            String selectedMessageId = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_ID));
            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            String selectedMessage = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE));

            int itemId= menuItem.getItemId();
            switch (itemId){
                case R.id.mnu_delete:
                    if(context instanceof ChatActivity)
                    {
                        ((ChatActivity)context).deleteMessage(selectedMessageId,selectedMessageType);
                    }
                    actionMode.finish();
                    break;
                case R.id.mnu_download:
                    if(context instanceof ChatActivity)
                    {
                        ((ChatActivity)context).downloadFile(selectedMessageId,selectedMessageType);
                    }
                    actionMode.finish();
                    break;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            selectedView.setBackgroundColor(context.getResources().getColor(R.color.colorBackground));


        }
    };
}
