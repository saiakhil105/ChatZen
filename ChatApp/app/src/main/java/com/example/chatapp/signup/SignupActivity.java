package com.example.chatapp.signup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.chatapp.R;
import com.example.chatapp.common.NodeNames;
import com.example.chatapp.login.LoginActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashMap;

public class SignupActivity extends AppCompatActivity {

    private EditText et_email, et_password, et_confirm_password, et_name;
    Button bt_signup;
    ImageView imageView;
    String txt_email, txt_password, txt_name, txt_confirm_password;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        getSupportActionBar().hide();
        et_name = findViewById(R.id.s_name);
        et_email = findViewById(R.id.s_email);
        et_password = findViewById(R.id.s_password);
        et_confirm_password = findViewById(R.id.s_confirm_password);
        bt_signup = findViewById(R.id.s_signup);
        imageView = findViewById(R.id.imageview);
        fileStorage = FirebaseStorage.getInstance().getReference();
        progressBar = findViewById(R.id.progressBar);


    }


    public void pickImage(View v) {

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
                imageView.setImageURI(localFileUri);
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
                                        hashMap.put(NodeNames.EMAIL, et_email.getText().toString().trim());
                                        hashMap.put(NodeNames.ONLINE, "true");
                                        hashMap.put(NodeNames.PHOTO, strFileName);

                                        databaseReference.child(userID).setValue(hashMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        Toast.makeText(SignupActivity.this, "User Created Successfully", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                                    }
                                                });


                                    } else {
                                        Toast.makeText(SignupActivity.this, "Failed", Toast.LENGTH_SHORT).show();
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
                    hashMap.put(NodeNames.PHOTO, "");
                    progressBar.setVisibility(View.VISIBLE);
                    databaseReference.child(userID).setValue(hashMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getApplicationContext(), "Signed Up Successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                    }
                                }
                            });
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to Upload Profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void btnSignUpClick(View v) {
        txt_email = et_email.getText().toString();
        txt_password = et_password.getText().toString();
        txt_confirm_password = et_confirm_password.getText().toString();
        txt_name = et_name.getText().toString();

        if (txt_name.equals("")) {
            et_name.setError(getString(R.string.enter_name));
        } else if (txt_email.equals("")) {
            et_email.setError(getString(R.string.enter_email));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(txt_email).matches()) {
            et_email.setError(getString(R.string.enter_email_correctly));
        } else if (txt_password.equals("")) {
            et_password.setError(getString(R.string.enter_password));
        } else if (!txt_password.equals(txt_confirm_password)) {
            et_confirm_password.setError(getString(R.string.mismatch));
        } else {
            progressBar.setVisibility(View.VISIBLE);
            final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.createUserWithEmailAndPassword(txt_email, txt_password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                firebaseUser = firebaseAuth.getCurrentUser();
                                if (localFileUri != null) {
                                    updateNameAndPhoto();
                                } else {
                                    updateOnlyName();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Signing Up Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


}