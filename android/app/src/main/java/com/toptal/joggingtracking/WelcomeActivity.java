package com.toptal.joggingtracking;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.toptal.joggingtracking.auth.AccountGeneral;

public class WelcomeActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            Account[] accounts = AccountManager.get(getBaseContext()).getAccountsByType(AccountGeneral.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }
    }

    public void onLoginBtn(View v) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, 123);
    }

    public void onSignUpBtn(View v) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivityForResult(intent, 123);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && resultCode == RESULT_OK) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
