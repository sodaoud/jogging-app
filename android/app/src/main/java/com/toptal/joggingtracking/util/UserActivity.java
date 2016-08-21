package com.toptal.joggingtracking.util;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.toptal.joggingtracking.ProfileActivity;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.datatype.ErrorUtil;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.datatype.UserDTO;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserActivity extends AppCompatActivity {

    public static final String USER = "USER";

    private User user;
    private ImageView editUsername;
    private ImageView editPassword;
    private View usernameContainer;
    private TextView showUsername;
    private EditText mUsername;
    private EditText mPassword;
    private CheckBox mManager;
    private CheckBox mAdmin;
    private OkHttpClient client;
    private UserTask mUserTask;
    private UserDTO userDTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        showUsername = (TextView) findViewById(R.id.show_username);
        usernameContainer = findViewById(R.id.username_container);
        editUsername = (ImageView) findViewById(R.id.edit_username_img);
        editUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUsername.setVisibility(View.VISIBLE);
                usernameContainer.setVisibility(View.GONE);
            }
        });
        mUsername = (EditText) findViewById(R.id.edit_username);
        editPassword = (ImageView) findViewById(R.id.edit_password);
        editPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPassword.setVisibility(View.VISIBLE);
                editPassword.setVisibility(View.GONE);
            }
        });
        mPassword = (EditText) findViewById(R.id.password);
        mManager = (CheckBox) findViewById(R.id.manager_role);
        mAdmin = (CheckBox) findViewById(R.id.admin_role);

        user = (User) getIntent().getSerializableExtra(USER);
        client = new OkHttpClient();
    }

    private void setValues() {
        if (user != null) {
            showUsername.setText(user.getUsername());
            mUsername.setText(user.getUsername());
            mManager.setChecked(false);
            mAdmin.setChecked(false);
            for (String role : user.getRoles()) {
                switch (role) {
                    case Util.USER:
                        break;
                    case Util.MANAGER:
                        mManager.setChecked(true);
                        break;
                    case Util.ADMIN:
                        mAdmin.setChecked(true);
                        break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureView();
        setValues();
    }

    private void configureView() {
        mUsername.setVisibility(user == null ? View.VISIBLE : View.GONE);
        usernameContainer.setVisibility(user == null ? View.GONE : View.VISIBLE);
        mPassword.setVisibility(user == null ? View.VISIBLE : View.GONE);
        editPassword.setVisibility(user == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                saveUser();
                return true;
            case R.id.delete:
                deleteUser();
                return true;
            case R.id.profile:
                Intent i = new Intent(this, ProfileActivity.class);
                i.putExtra(ProfileActivity.USER, user);
                startActivityForResult(i, 9123);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 9123) {
            user = (User) data.getSerializableExtra(USER);
        }
    }

    private void saveUser() {
        if (!validate()) return;
        userDTO = new UserDTO();
        if (mUsername.getVisibility() == View.VISIBLE)
            userDTO.setUsername(mUsername.getText().toString());
        if (mPassword.getVisibility() == View.VISIBLE)
            userDTO.setPassword(mPassword.getText().toString());
        ArrayList<String> roles = new ArrayList<>();

        roles.add(Util.USER);
        if (mManager.isChecked())
            roles.add(Util.MANAGER);
        if (mAdmin.isChecked())
            roles.add(Util.ADMIN);
        String[] roleArray = new String[roles.size()];
        roles.toArray(roleArray);
        userDTO.setRoles(roleArray);

        mUserTask = new UserTask();
        mUserTask.execute();
    }

    private boolean validate() {
        if (mPassword.getVisibility() == View.VISIBLE && mPassword.getText().toString().length() < 6) {
            mPassword.setError("Password must contain at least 6 characters");
            return false;
        }
        return true;
    }

    class UserTask extends AsyncTask<Void, Void, Response> {

        UserTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            String token = Util.getAuthToken(UserActivity.this);
            try {
                HttpUrl.Builder builder = new HttpUrl.Builder()
                        .scheme("http")
                        .host(Util.HOST)
                        .port(Util.PORT)
                        .addPathSegment(Util.SEGMENT_USER);
                if (user != null)
                    builder.addPathSegment(user.getId());
                Gson gson = new Gson();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(userDTO));
                Request.Builder requestBuilder = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(builder.build());

                if (user != null)
                    requestBuilder.put(body);
                else
                    requestBuilder.post(body);
                Request request = requestBuilder.build();
                return client.newCall(request).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mUserTask = null;
            if (response != null) {
                if (response.code() == 200 || response.code() == 201) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    ErrorUtil err = null;
                    try {
                        err = ErrorUtil.getFromString(response.body().string());
                        switch (err.getError()) {
                            case "USERNAME_ERROR":
                                mUsername.setError(err.getMessage());
                                break;
                            case "PASSWORD_ERROR":
                                mPassword.setError(err.getMessage());
                                break;
                            default:
                                Toast.makeText(UserActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(UserActivity.this, "An Error happened, please try again later", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(UserActivity.this, "Can't reach server", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mUserTask = null;
        }
    }

    private void deleteUser() {
        new AlertDialog.Builder(UserActivity.this)
                .setTitle("Delete")
                .setMessage("Are you sure you want to delete this entry?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HttpUrl.Builder builder = new HttpUrl.Builder()
                                .scheme("http")
                                .host(Util.HOST)
                                .port(Util.PORT)
                                .addPathSegment(Util.SEGMENT_USER)
                                .addPathSegment(user.getId());
                        Request request = new Request.Builder()
                                .addHeader("Authorization", Util.getAuthToken(UserActivity.this))
                                .url(builder.build())
                                .delete()
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Toast.makeText(UserActivity.this, "Can not delete the entry", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (response.code() == 200) {
                                    finish();
                                } else {
                                    Toast.makeText(UserActivity.this, "Can not delete the entry", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_menu, menu);
        menu.findItem(R.id.delete).setVisible(user != null);
        return true;
    }
}
