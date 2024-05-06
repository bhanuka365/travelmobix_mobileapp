package com.example.public_transport_tracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Register extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout signupEmail, signupPassword ,signupname,signupnic,signuptel;
    private Button signupButton;
    private TextView loginRedirectText;
    FirebaseDatabase database;

    DatabaseReference reference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        signupname=findViewById(R.id.txt_name);
        signupnic=findViewById(R.id.txt_nic);
        signuptel=findViewById(R.id.txt_tel);
        signupEmail = findViewById(R.id.txt_email);
        signupPassword = findViewById(R.id.txt_psw);
        signupButton = findViewById(R.id.btn_signUp);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = signupEmail.getEditText().getText().toString().trim();
                String pass = signupPassword.getEditText().getText().toString().trim();
                String name = signupname.getEditText().getText().toString().trim();
                String nic = signupnic.getEditText().getText().toString().trim();
                String tel = signuptel.getEditText().getText().toString().trim();

                database = FirebaseDatabase.getInstance( "https://travelmobix-7d56c-default-rtdb.asia-southeast1.firebasedatabase.app");


                if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || nic.isEmpty() || tel.isEmpty()) {
                    // Handle empty fields
                    Toast.makeText(Register.this, "All fields are required", Toast.LENGTH_SHORT).show();
                }
                if (tel.isEmpty() || tel.length() != 10) {
                    signuptel.setError("Enter a valid 10-digit telephone number");
                    signuptel.requestFocus();
                    return;
                }else {
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // User creation successful, now store additional information in Firebase
                                String userId = auth.getCurrentUser().getUid();
                                reference = database.getReference().child("users").child(userId);

                                DBHelper helperClass = new DBHelper(name, nic, tel, email, pass);
                                reference.setValue(helperClass);

                                Toast.makeText(Register.this, "SignUp Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Register.this, Login.class));
                            } else {
                                Toast.makeText(Register.this, "SignUp Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Register.this, Login.class));
            }
        });
    }
}