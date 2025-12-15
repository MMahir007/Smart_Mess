package edu.ewubd.smartmessMN;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PreSignUp extends AppCompatActivity {

    Button btnEmail;
    TextView txtLogin, tvGoogle;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    GoogleSignInClient googleSignInClient;

    private static final int RC_GOOGLE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_sign_up);

        txtLogin = findViewById(R.id.txtLogin);
        tvGoogle = findViewById(R.id.tv_google);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        setupGoogleLogin();

        // 1) Email sign up → SignUp form


        // 2) Old user → normal Login screen
        txtLogin.setOnClickListener(v ->
                startActivity(new Intent(this, Login.class))
        );

        // 3) Google → choose email, then Firestore diye new/old check
        tvGoogle.setOnClickListener(v -> {
            // force chooser every time
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent intent = googleSignInClient.getSignInIntent();
                startActivityForResult(intent, RC_GOOGLE);
            });
        });
    }

    private void setupGoogleLogin() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()             // sudhu email chai, token na
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE) {
            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String email = account.getEmail();
                    String name  = account.getDisplayName();
                    handleGoogleEmail(email, name);
                }
            } catch (ApiException e) {
                Toast.makeText(this,
                        "Google Sign In Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // email diye Firestore check: ache → Login, na thakle → SignUp
    private void handleGoogleEmail(String email, String name) {
        if (email == null) {
            Toast.makeText(this,
                    "Failed to get email from Google",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        // ⭐ OLD USER → Login, email prefill
                        Intent i = new Intent(this, Login.class);
                        i.putExtra("prefill_email", email);
                        startActivity(i);
                    } else {
                        // ⭐ NEW USER → SignUp, email + name prefill
                        Intent i = new Intent(this, SignUp.class);
                        i.putExtra("email", email);
                        i.putExtra("name", name);
                        startActivity(i);
                    }
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to check user: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}


