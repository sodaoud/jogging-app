package com.toptal.joggingtracking;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.toptal.joggingtracking.datatype.Profile;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;

import io.gsonfire.DateSerializationPolicy;
import io.gsonfire.GsonFireBuilder;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ProfileActivity extends AppCompatActivity {

    public static final String USER = "USER";

    private User user;
    private Profile profile;
    private ProfileTask mProfileTask;
    private OkHttpClient client;
    private EditText mFirstName;
    private EditText mLastName;
    private AppCompatSpinner mUnit;
    private AppCompatSpinner mSex;
    private EditText mAge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Edit Profile");
        mFirstName = (EditText) findViewById(R.id.first_name);
        mLastName = (EditText) findViewById(R.id.last_name);
        mSex = (AppCompatSpinner) findViewById(R.id.sex);
        mUnit = (AppCompatSpinner) findViewById(R.id.unit);
        mAge = (EditText) findViewById(R.id.age);

        user = (User) getIntent().getSerializableExtra(USER);

        client = new OkHttpClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setValues();
    }

    private void setValues() {
        if (user != null) {
            profile = user.getProfile();
            mFirstName.setText(profile.getFirstname());
            mLastName.setText(profile.getLastname());
            mAge.setText(profile.getAge() > 0 ? profile.getAge() + "" : "");
            mSex.setSelection(profile.getSex().equals("Female") ? 1 : 0);
            mUnit.setSelection(profile.getUnit().equals("Mile") ? 1 : 0);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                saveProfile();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveProfile() {
        profile = new Profile();
        profile.setAge(Integer.parseInt(mAge.getText().toString()));
        profile.setFirstname(mFirstName.getText().toString());
        profile.setLastname(mLastName.getText().toString());
        profile.setSex(mSex.getSelectedItem().toString());
        profile.setUnit(mUnit.getSelectedItem().toString());

        mProfileTask = new ProfileTask();
        mProfileTask.execute();
    }


    class ProfileTask extends AsyncTask<Void, Void, Response> {

        ProfileTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            String token = Util.getAuthToken(ProfileActivity.this);
            try {
                HttpUrl.Builder builder;
                builder = new HttpUrl.Builder()
                        .scheme("http")
                        .host(Util.HOST)
                        .port(Util.PORT)
                        .addPathSegment(Util.SEGMENT_USER)
                        .addPathSegment(user.getId())
                        .addPathSegment(Util.SEGMENT_PROFILE);

                Gson gson = new GsonFireBuilder().dateSerializationPolicy(DateSerializationPolicy.rfc3339).createGson();

                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(profile));
                Request.Builder requestBuilder = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(builder.build())
                        .put(body);
                Request request = requestBuilder.build();
                return client.newCall(request).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mProfileTask = null;
            if (response != null) {
                if (response.code() == 200) {
                    Gson gson = new Gson();
                    try {
                        User user = gson.fromJson(response.body().string(), User.class);
                        Intent i = getIntent();
                        i.putExtra(ProfileActivity.USER, user);
                        setResult(RESULT_OK, i);
                        Toast.makeText(ProfileActivity.this, "The profile has been successfully updated", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(ProfileActivity.this, "An error happened please retry later", Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onCancelled() {
            mProfileTask = null;
        }
    }
}
