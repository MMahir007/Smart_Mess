package edu.ewubd.smartmessMN;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SignUp extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private EditText etMessCode;
    private CheckBox cbManager;
    private LinearLayout btnGoogleSignUp;
    private TextView txtLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference(); // Realtime DB

        etMessCode = findViewById(R.id.et_mess_code);
        cbManager = findViewById(R.id.cb_manager);
        btnGoogleSignUp = findViewById(R.id.btn_google_signup);
        txtLogin = findViewById(R.id.txtLogin);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 1. Initial UI State
        btnGoogleSignUp.setVisibility(View.VISIBLE);

        // 2. Checkbox Listener
        cbManager.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etMessCode.setVisibility(View.GONE);
                etMessCode.setText("");
            } else {
                etMessCode.setVisibility(View.VISIBLE);
            }
        });

        // 3. Button Click Listener
        btnGoogleSignUp.setOnClickListener(v -> {
            if (cbManager.isChecked()) {
                signIn();
            } else {
                String code = etMessCode.getText().toString().trim();
                if (code.isEmpty()) {
                    etMessCode.setError("Please enter the Mess Code");
                    etMessCode.requestFocus();
                } else if (code.length() < 6) {
                    etMessCode.setError("Mess ID must be 6 digits");
                    etMessCode.requestFocus();
                } else {
                    signIn();
                }
            }
        });

        txtLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUp.this, Login.class));
            finish();
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign In Failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        processUserRegistration(mAuth.getCurrentUser());
                    } else {
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------------------
    //         REALTIME DATABASE LOGIC (UPDATED)
    // ---------------------------------------------------

    private void processUserRegistration(FirebaseUser user) {
        boolean isManager = cbManager.isChecked();
        String uid = user.getUid();
        String email = user.getEmail();

        // 1. Get Name from Google Account
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = "No Name";
        }

        // 2. Generate Current Date
        String joinDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        if (isManager) {
            // --- MANAGER FLOW ---
            String newMessId = generateNumericCode();
            String finalName = name; // Need final for inner class usage

            mDatabase.child("mess").child(newMessId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        processUserRegistration(user); // ID Collision retry
                    } else {
                        // Pass name and joinDate to helper method
                        createMessAndManager(uid, email, newMessId, finalName, joinDate);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    failRegistration("DB Error: " + error.getMessage());
                }
            });

        } else {
            // --- MEMBER FLOW ---
            String inputMessId = etMessCode.getText().toString().trim();
            if (inputMessId.isEmpty()) {
                failRegistration("Mess Code missing. Please try again.");
                return;
            }
            String finalName = name;

            mDatabase.child("mess").child(inputMessId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Pass name and joinDate to helper method
                        joinMessAndCreateMember(uid, email, inputMessId, finalName, joinDate);
                    } else {
                        failRegistration("Mess Code not found! Ask your manager.");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    failRegistration("DB Error: " + error.getMessage());
                }
            });
        }
    }

    private String generateNumericCode() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }

    // --- MANAGER HELPER (UPDATED) ---
    private void createMessAndManager(String uid, String email, String messId, String name, String joinDate) {
        Map<String, Object> messData = new HashMap<>();
        messData.put("managerUid", uid);
        messData.put("createdAt", System.currentTimeMillis());

        Map<String, String> members = new HashMap<>();
        members.put(uid, "Manager");
        messData.put("members", members);

        mDatabase.child("mess").child(messId).setValue(messData)
                .addOnSuccessListener(aVoid -> saveUserProfile(uid, email, messId, "Manager", name, joinDate))
                .addOnFailureListener(e -> failRegistration("Failed to create mess."));
    }

    // --- MEMBER HELPER (UPDATED) ---
    private void joinMessAndCreateMember(String uid, String email, String messId, String name, String joinDate) {
        mDatabase.child("mess").child(messId).child("members").child(uid).setValue("Member")
                .addOnSuccessListener(aVoid -> saveUserProfile(uid, email, messId, "Member", name, joinDate))
                .addOnFailureListener(e -> failRegistration("Failed to join mess."));
    }

    // --- SAVE USER PROFILE (UPDATED) ---
    // Now accepts name and joinDate
    private void saveUserProfile(String uid, String email, String messId, String role, String name, String joinDate) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("messCode", messId);
        userData.put("name", name);         // Added Name
        userData.put("joinDate", joinDate); // Added Join Date
        userData.put("phone", "");          // Placeholder for phone (optional)

        mDatabase.child("users").child(uid).setValue(userData)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Success! Welcome " + name, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignUp.this, MainActivity.class);
                    intent.putExtra("role", role);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> failRegistration("Failed to save profile."));
    }

    private void failRegistration(String errorMsg) {
        mAuth.signOut();
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
    }
}