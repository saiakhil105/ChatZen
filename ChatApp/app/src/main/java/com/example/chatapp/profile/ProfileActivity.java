package com.example.chatapp.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.chats.ChatActivity;
import com.example.chatapp.common.Constants;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.login.LoginActivity;
import com.example.chatapp.password.ChangePasswordActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private EditText et_name, et_email;
    private ImageView profile_image;
    private DatabaseReference databaseReference;
    private StorageReference fileStorage;
    private FirebaseUser firebaseUser;
    private Uri localFileUri, serverFileUri;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        et_email = findViewById(R.id.p_email);
        et_name = findViewById(R.id.p_name);
        profile_image = findViewById(R.id.p_imageview);
        fileStorage = FirebaseStorage.getInstance().getReference();
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        TextView change = findViewById(R.id.change);
        progressBar = findViewById(R.id.progressBar);

        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ChangePasswordActivity.class));
            }
        });

        if (firebaseUser != null) {
            et_name.setText(firebaseUser.getDisplayName());
            et_email.setText(firebaseUser.getEmail());
            serverFileUri = firebaseUser.getPhotoUrl();

            if (serverFileUri != null) {
                Glide.with(this).load(serverFileUri)
                        .placeholder(R.drawable.loadingcircle)
                        .error(R.drawable.user)
                        .into(profile_image);
            }
        }

    }

    public void btn_signout(View view) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();
        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        String userID = firebaseUser.getUid();
        databaseReference.child(userID).child(NodeNames.ONLINE).setValue("false");
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();
    }

    public void btnSaveClick(View view) {
        if (et_name.getText().toString().equals("")) {
            et_name.setError(getString(R.string.enter_name));
        } else {
            if (localFileUri != null) {
                updateNameAndPhoto();
            } else {
                updateOnlyName();
            }
        }
    }

    private void removePhoto() {
        progressBar.setVisibility(View.VISIBLE);
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(et_name.getText().toString().trim())
                .setPhotoUri(null)
                .build();
        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    String userID = firebaseUser.getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                    HashMap<String, String> hashMap = new HashMap<>();

                    hashMap.put(NodeNames.NAME, et_name.getText().toString().trim());
                    hashMap.put(NodeNames.EMAIL, et_email.getText().toString());
                    hashMap.put(NodeNames.ONLINE, "true");
                    hashMap.put(NodeNames.PHOTO, "");

                    databaseReference.child(userID).setValue(hashMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    Toast.makeText(ProfileActivity.this, "Profile Picture Removed Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });


                } else {
                    Toast.makeText(ProfileActivity.this, "Failed To Remove Profile Picture", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    public void changeImage(View view) {
        if (serverFileUri == null) {
            pickImage();
        } else {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_picture, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int id = menuItem.getItemId();
                    if (id == R.id.mnuChangePic) {
                        pickImage();
                    } else if (id == R.id.mnuRemovePic) {
                        removePhoto();
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    }

    private void pickImage() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {

                localFileUri = data.getData();
                profile_image.setImageURI(localFileUri);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 102) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 101);
            } else {
                Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void updateNameAndPhoto() {
        final String strFileName = firebaseUser.getUid() + ".jpg";

        final StorageReference fileRef = fileStorage.child("images/" + strFileName);
        progressBar.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            serverFileUri = uri;

                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(et_name.getText().toString().trim())
                                    .setPhotoUri(serverFileUri)
                                    .build();

                            firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        String userID = firebaseUser.getUid();
                                        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                                        HashMap<String, String> hashMap = new HashMap<>();

                                        hashMap.put(NodeNames.NAME, et_name.getText().toString().trim());
                                        hashMap.put(NodeNames.EMAIL, et_email.getText().toString());
                                        hashMap.put(NodeNames.ONLINE, "true");
                                        hashMap.put(NodeNames.PHOTO, strFileName);

                                        databaseReference.child(userID).setValue(hashMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        finish();
                                                    }
                                                });


                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                        }
                    });
                }
            }
        });

    }

    private void updateOnlyName() {
        progressBar.setVisibility(View.VISIBLE);
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(et_name.getText().toString())
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    String userID = firebaseUser.getUid();
                    databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.NAME, et_name.getText().toString());
                    hashMap.put(NodeNames.EMAIL, et_email.getText().toString());
                    hashMap.put(NodeNames.ONLINE, "true");
                    hashMap.put(NodeNames.PHOTO, firebaseUser.getUid() + ".jpg");
                    databaseReference.child(userID).setValue(hashMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        finish();
                                    }
                                }
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to Upload Profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}