package edu.ewubd.smartmess;

import android.content.Intent;
import android.os.Bundle;
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

public class Login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in); // Ensure matches your XML name

        // 1. Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 2. Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. UI Bindings
        LinearLayout btnGoogleLogin = findViewById(R.id.btn_google_login);
        TextView txtGoToSignup = findViewById(R.id.txtGoToSignup);
        TextView txtExitApp = findViewById(R.id.txtExitApp);

        // 4. Click Listeners
        btnGoogleLogin.setOnClickListener(v -> signIn());

        txtGoToSignup.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, SignUp.class));
            finish();
        });

        txtExitApp.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });
    }

    // --- Auto Login Check on Start ---
    @Override
    protected void onStart() {
        super.onStart();
        // If user is already logged in from a previous session, check DB immediately
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserDatabase(currentUser.getUid());
        }
    }

    // --- Google Auth Flow ---
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
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Google Auth Success -> Now check Realtime Database for Role/MessID
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserDatabase(user.getUid());
                    } else {
                        Toast.makeText(Login.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- Database Verification Logic ---
    private void checkUserDatabase(String uid) {
        System.out.println("Check");
        // Look up the user in the "users" node
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User exists! Retrieve their data.
                    String role = snapshot.child("role").getValue(String.class);
                    String messCode = snapshot.child("messCode").getValue(String.class);

                    // Redirect to Main Activity with this info
                    updateUI(role, messCode);
                    System.out.println(uid);
                    System.out.println(role);
                    System.out.println(messCode);
                    Intent i = new Intent(Login.this, MainActivity.class);
                    i.putExtra("uid", uid);
                    i.putExtra("role", role);
                    i.putExtra("messCode", messCode);


                } else {
                    // Critical Case: User logged in with Google, but has NO data in DB.
                    // This means they skipped the Sign-Up process.
                    Toast.makeText(Login.this, "Account not found. Please Sign Up first.", Toast.LENGTH_LONG).show();

                    mAuth.signOut(); // Log them out immediately
                    mGoogleSignInClient.signOut(); // Ensure Google selector appears next time

                    // Send to Sign Up Page
                    Intent i = new Intent(Login.this, SignUp.class);
                    startActivity(i);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Login.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    private void updateUI(String role, String messCode) {
        Intent intent = new Intent(Login.this, MainActivity.class);
        // Pass the crucial info to the next activity
        intent.putExtra("role", role);
        intent.putExtra("messCode", messCode);
        startActivity(intent);
        finish();
    }
}