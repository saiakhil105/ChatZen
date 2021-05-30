package com.example.chatapp.chats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.Extras;
import com.example.chatapp.common.NodeNames;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RECORD_PER_PAGE = 30;
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_PICK_VIDEO = 103;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;


    private ImageView ivProfile;
    TextView tvUserName, tvUserStatus;
    private EditText etMessage;
    private DatabaseReference mRootRef;
    private String currentUserId, chatUserId;

    private RelativeLayout chatActivity;
    private RecyclerView rvMessage;
    private SwipeRefreshLayout srlMessages;
    private MessagesAdapter messagesAdapter;
    private List<MessageModel> messagesList;
    private int currentPage = 1;
    private BottomSheetDialog bottomSheetDialog;

    private String userName, photoName;

    private ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setElevation(0);
            actionBar.setCustomView(actionBarLayout);
            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        }

        ivProfile = findViewById(R.id.ivProfile);
        ImageView ivAttach = findViewById(R.id.ivAttachment);
        tvUserName = findViewById(R.id.tvUsername);
        tvUserStatus = findViewById(R.id.tvUserStatus);
        chatActivity = findViewById(R.id.chatactivity);

        ImageView ivSend = findViewById(R.id.ivSend);
        etMessage = findViewById(R.id.etMessage);


        ivSend.setOnClickListener(this);
        ivAttach.setOnClickListener(this);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        if (getIntent().hasExtra(Extras.USER_KEY)) {
            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
        }
        if (getIntent().hasExtra(Extras.USER_NAME)) {
            userName = getIntent().getStringExtra(Extras.USER_NAME);
        }
        if (getIntent().hasExtra(Extras.PHOTO_NAME)) {
            photoName = getIntent().getStringExtra(Extras.PHOTO_NAME);
        }

        tvUserName.setText(userName);
        if (!TextUtils.isEmpty(photoName)) {
            final StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER).child(photoName);
            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(ChatActivity.this)
                            .load(uri)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .into(ivProfile);
                }
            });
        }


        rvMessage = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);

        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(this, messagesList);

        rvMessage.setLayoutManager(new LinearLayoutManager(this));
        rvMessage.setAdapter(messagesAdapter);

        loadMessages();
        rvMessage.scrollToPosition(messagesList.size() - 1);

        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                currentPage++;
                loadMessages();
            }
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
        view.findViewById(R.id.llCamera).setOnClickListener(this);
        view.findViewById(R.id.llGallery).setOnClickListener(this);
        view.findViewById(R.id.llVideo).setOnClickListener(this);
        view.findViewById(R.id.ivClose).setOnClickListener(this);
        bottomSheetDialog.setContentView(view);

        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = "";
                if (dataSnapshot.child(NodeNames.ONLINE).getValue() != null)
                    status = dataSnapshot.child(NodeNames.ONLINE).getValue().toString();
                if (status.equals("true")) {
                    tvUserStatus.setVisibility(View.VISIBLE);
                    tvUserStatus.setText(Constants.STATUS_ONLINE);
                } else
                    tvUserStatus.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
                if (editable.toString().matches("")) {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
                } else {
                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
                }

            }
        });

        DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(NodeNames.TYPING).getValue() != null) {
                    String typingStatus = dataSnapshot.child(NodeNames.TYPING).getValue().toString();
                    if (typingStatus.equals(Constants.TYPING_STARTED)) {
                        tvUserStatus.setVisibility(View.VISIBLE);
                        tvUserStatus.setText(Constants.STATUS_TYPING);
                    } else {
                        tvUserStatus.setText(Constants.STATUS_ONLINE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void sendMessage(String msg, String msgType, String pushId) {
        try {
            if (!msg.equals("")) {
                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.MESSAGE_ID, pushId);
                messageMap.put(NodeNames.MESSAGE, msg);
                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");
                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                        {

                        }
                    }
                });
            }
        } catch (Exception ex) {
            Toast.makeText(ChatActivity.this, "Failed to send Message", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        messagesList.clear();
        DatabaseReference databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);

        Query messagesQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);
        if (childEventListener != null)
            messagesQuery.removeEventListener(childEventListener);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                MessageModel message = dataSnapshot.getValue(MessageModel.class);
                messagesList.add(message);
                messagesAdapter.notifyDataSetChanged();
                rvMessage.scrollToPosition(messagesList.size() - 1);
                srlMessages.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                loadMessages();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                srlMessages.setRefreshing(false);
            }
        };

        messagesQuery.addChildEventListener(childEventListener);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ivSend:

                DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
                String pushId = userMessagePush.getKey();
                sendMessage(etMessage.getText().toString().trim(), Constants.MESSAGE_TYPE_TEXT, pushId);
                break;

            case R.id.ivAttachment:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    if (bottomSheetDialog != null)
                        bottomSheetDialog.show();


                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                break;

            case R.id.llCamera:
                bottomSheetDialog.dismiss();
                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera, REQUEST_CODE_CAPTURE_IMAGE);
                break;

            case R.id.llGallery:
                bottomSheetDialog.dismiss();
                Intent intentImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentImage, REQUEST_CODE_PICK_IMAGE);
                break;

            case R.id.llVideo:
                bottomSheetDialog.dismiss();
                Intent intentVideo = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentVideo, REQUEST_CODE_PICK_VIDEO);
                break;

            case R.id.ivClose:
                bottomSheetDialog.dismiss();


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE_IMAGE)//Camera
            {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                uploadBytes(bytes);

            } else if (requestCode == REQUEST_CODE_PICK_IMAGE) {
                Uri uri = data.getData();
                uploadFile(uri, Constants.MESSAGE_TYPE_IMAGE);
            } else if (requestCode == REQUEST_CODE_PICK_VIDEO) {
                Uri uri = data.getData();
                uploadFile(uri, Constants.MESSAGE_TYPE_VIDEO);
            }
        }

    }

    private void uploadFile(Uri uri, String messageType) {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putFile(uri);

        uploadProgress(uploadTask, fileRef, pushId, messageType);
    }


    private void uploadBytes(ByteArrayOutputStream bytes) {
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
        String pushId = databaseReference.getKey();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        String folderName = Constants.MESSAGE_TYPE_IMAGE.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
        String fileName = Constants.MESSAGE_TYPE_IMAGE.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";

        StorageReference fileRef = storageReference.child(folderName).child(fileName);
        UploadTask uploadTask = fileRef.putBytes(bytes.toByteArray());
        uploadProgress(uploadTask, fileRef, pushId, Constants.MESSAGE_TYPE_IMAGE);
    }


    private void uploadProgress(final UploadTask task, final StorageReference filePath, final String pushId, final String messageType) {


        final AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
        View aview = getLayoutInflater().inflate(R.layout.progress, null);
        final ImageView play = aview.findViewById(R.id.play);
        final TextView tv = aview.findViewById(R.id.tvuploading);
        final ImageView pause = aview.findViewById(R.id.pause);
        final ProgressBar pro = aview.findViewById(R.id.progressBar22);
        ImageView cancel = aview.findViewById(R.id.cancel);
        play.setVisibility(View.GONE);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.resume();
                tv.setText("Uploading...");
                play.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.pause();
                tv.setText("Uploading Paused");
                pause.setVisibility(View.GONE);
                play.setVisibility(View.VISIBLE);
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                task.cancel();
            }
        });

        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                pro.setProgress((int) progress);
                tv.setText(getString((R.string.upload_progress),messageType,String.valueOf(pro.getProgress())));
            }
        });

        alert.setView(aview);
        final AlertDialog alertDialog = alert.create();
        alertDialog.show();


        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                alertDialog.dismiss();
                if (task.isSuccessful()) {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            sendMessage(downloadUrl, messageType, pushId);
                        }
                    });
                }
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                alertDialog.dismiss();
                Toast.makeText(ChatActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bottomSheetDialog != null)
                    bottomSheetDialog.show();
            } else {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();

        switch (itemId) {
            case android.R.id.home:
                finish();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(final String messageId, final String messageType){
        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES)
                .child(currentUserId).child(chatUserId).child(messageId);
        databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.MESSAGES)
                            .child(chatUserId).child(currentUserId).child(messageId);
                    databaseReferenceChatUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(getApplicationContext(),"Message deleted successfully",Toast.LENGTH_SHORT).show();
                                if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT)){
                                    StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                                    String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
                                    String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId+".mp4":messageId+".jpg";
                                    StorageReference fileRef = rootRef.child(folder).child(fileName);
                                    fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(!task.isSuccessful()){
                                                Toast.makeText(getApplicationContext(),"Failed to delete file",Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });


                                }
                            }else {
                                Toast.makeText(getApplicationContext(),"Failed to delete message",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(getApplicationContext(),"Failed to delete message",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void downloadFile(final String messageId, final String messageType){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
        }else {
            String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
            String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";

            StorageReference fileRef = FirebaseStorage.getInstance().getReference().child(folderName).child(fileName);
            final String localFilePath = getExternalFilesDir(null).getAbsolutePath()+"/"+fileName;

            File localFile = new File(localFilePath);

            try{
                if(localFile.exists()|| localFile.createNewFile()){
                    final FileDownloadTask downloadTask = fileRef.getFile(localFile);
                    final AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                    final View aview = getLayoutInflater().inflate(R.layout.progress, null);
                    final ImageView play = aview.findViewById(R.id.play);
                    final TextView tv = aview.findViewById(R.id.tvuploading);
                    final ImageView pause = aview.findViewById(R.id.pause);
                    final ProgressBar pro = aview.findViewById(R.id.progressBar22);
                    ImageView cancel = aview.findViewById(R.id.cancel);
                    play.setVisibility(View.GONE);
                    play.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.resume();
                            play.setVisibility(View.GONE);
                            pause.setVisibility(View.VISIBLE);
                        }
                    });
                    pause.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.pause();
                            pause.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    });
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadTask.cancel();
                        }
                    });

                    alert.setView(aview);
                    final AlertDialog alertDialog = alert.create();
                    alertDialog.show();

                    downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            pro.setProgress((int) progress);
                            tv.setText(getString((R.string.download_progress),messageType,String.valueOf(pro.getProgress())));
                        }
                    });

                    downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
                            alertDialog.dismiss();
                            if(task.isSuccessful()){
                                final Snackbar snackbar = Snackbar.make(chatActivity,"File Downloaded Successfully",Snackbar.LENGTH_INDEFINITE);
                                snackbar.setAction(R.string.view, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Uri uri = Uri.parse(localFilePath);
                                        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                                        if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO)){
                                            intent.setDataAndType(uri,"video/mp4");
                                        }else if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE)){
                                            intent.setDataAndType(uri,"image/jpg");
                                        }
                                        startActivity(intent);
                                    }
                                });
                                snackbar.show();
                            }
                        }
                    });
                    downloadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),"Failed to download file",Toast.LENGTH_SHORT).show();
                        }
                    });

                }else{
                    Toast.makeText(ChatActivity.this, "Failed to store file", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),"Failed to download file",Toast.LENGTH_SHORT).show();
            }
        }
    }
}