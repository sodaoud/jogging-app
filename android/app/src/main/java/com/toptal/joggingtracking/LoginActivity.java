package com.toptal.joggingtracking;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.toptal.joggingtracking.datatype.ErrorUtil;
import com.toptal.joggingtracking.datatype.TokenUtil;
import com.toptal.joggingtracking.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    public static final String ACCOUNT_TYPE = "ACCOUNT_TYPE";
    //    public static final String ADDING_NEW_ACCOUNT = "ADDING_NEW_ACCOUNT";
    public static final String AUTH_TYPE = "AUTH_TYPE";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private OkHttpClient client;
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button signInButton = (Button) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        client = new OkHttpClient();

    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Response> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Response doInBackground(Void... params) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("username", mUsername);
                obj.put("password", mPassword);
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), obj.toString());
                Request request = new Request.Builder()
                        .url(Util.URL_LOGIN)
                        .post(body)
                        .build();
                return client.newCall(request).execute();

            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mAuthTask = null;
            showProgress(false);

            if (response != null) {
                String bodyString = null;
                try {
                    bodyString = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
                }
                if (response.code() == 200) {
                    Gson gson = new Gson();
                    TokenUtil tu = gson.fromJson(bodyString, TokenUtil.class);
                    Bundle mResultBundle = Util.finishLogin(LoginActivity.this, tu, mUsername, mPassword);
                    if (mAccountAuthenticatorResponse != null) {
                        mAccountAuthenticatorResponse.onResult(mResultBundle);
                    }
                    Intent i = new Intent();
                    i.putExtras(mResultBundle);
                    setResult(RESULT_OK, i);
                    finish();

                } else {
                    ErrorUtil err = ErrorUtil.getFromString(bodyString);
                    switch (err.getError()) {
                        case "USERNAME_ERROR":
                            mUsernameView.setError(err.getMessage());
                            break;
                        case "PASSWORD_ERROR":
                            mPasswordView.setError(err.getMessage());
                            break;
                        default:
                            Toast.makeText(LoginActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(LoginActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

