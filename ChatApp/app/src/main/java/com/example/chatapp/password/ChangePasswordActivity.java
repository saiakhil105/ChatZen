package com.example.chatapp.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText changepassword, confirmpassword;
    String txt_one, txt_two;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        progressBar = findViewById(R.id.progressBar);
        changepassword = findViewById(R.id.change_password);
        confirmpassword = findViewById(R.id.confirm_password);
        Button ch = findViewById(R.id.cp_save);
        ch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txt_one = changepassword.getText().toString();
                txt_two = confirmpassword.getText().toString();
                if (txt_one.equals("")) {
                    changepassword.setError(getString(R.string.enter_password));
                } else if (txt_two.equals("")) {
                    confirmpassword.setError(getString(R.string.enter_password));
                } else if (!txt_one.equals(txt_two)) {
                    confirmpassword.setError(getString(R.string.mismatch));
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.updatePassword(txt_two).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Password Failed To Update Password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }
        });

    }
}