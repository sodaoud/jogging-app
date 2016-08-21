package com.toptal.joggingtracking;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.toptal.joggingtracking.datatype.Track;
import com.toptal.joggingtracking.datatype.User;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import biz.kasual.materialnumberpicker.MaterialNumberPicker;
import io.gsonfire.DateSerializationPolicy;
import io.gsonfire.GsonFireBuilder;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class TrackActivity extends AppCompatActivity {

    public static final String EDIT = "EDIT";
    public static final String ADMIN = "ADMIN";
    public static final String TRACK = "TRACK";
    public static final String USERS = "USERS";

    private boolean edit = false;
    private Track track;
    private Track tmp;
    private boolean admin;
    private User[] users;
    private TextView showDate;
    private TextView showDistance;
    private TextView showDuration;
    private ImageView editDistance;
    private ImageView editDate;
    private ImageView editDuration;
    private MenuItem doneItem;
    private MenuItem editItem;
    private MenuItem deleteItem;
    private AppCompatSpinner userSpinner;
    private TrackTask mTrackTask;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        edit = getIntent().getBooleanExtra(EDIT, true);
        track = (Track) getIntent().getSerializableExtra(TRACK);
        admin = getIntent().getBooleanExtra(ADMIN, false);
        if (admin)
            users = (User[]) getIntent().getSerializableExtra(USERS);
        tmp = new Track(null, -1, -1);

        showDate = (TextView) findViewById(R.id.show_date);
        showDistance = (TextView) findViewById(R.id.show_distance);
        showDuration = (TextView) findViewById(R.id.show_duration);
        editDistance = (ImageView) findViewById(R.id.edit_distance);
        editDistance.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                View view = getLayoutInflater().inflate(R.layout.dialog_distance_picker, null);
                final MaterialNumberPicker km = (MaterialNumberPicker) view.findViewById(R.id.km);
                final MaterialNumberPicker dm = (MaterialNumberPicker) view.findViewById(R.id.dm);
                dm.setWrapSelectorWheel(true);
                km.setWrapSelectorWheel(true);
                if (tmp.getDistance() >= 0) {
                    km.setValue(tmp.getNumOfKm());
                    dm.setValue(tmp.getNumOfDm());
                }
                AlertDialog.Builder b = new AlertDialog.Builder(TrackActivity.this).setTitle("Set distance").setView(view);
                b.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tmp.setDistance(km.getValue() * 1000 + dm.getValue() * 10);
                        showDistance.setText(tmp.getFormatedDistance());
                    }
                });
                b.setNegativeButton("Cancel", null);
                b.show();
            }
        });
        editDuration = (ImageView) findViewById(R.id.edit_duration);
        editDuration.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                View view = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
                final MaterialNumberPicker hour = (MaterialNumberPicker) view.findViewById(R.id.hour);
                final MaterialNumberPicker minute = (MaterialNumberPicker) view.findViewById(R.id.minute);
                final MaterialNumberPicker second = (MaterialNumberPicker) view.findViewById(R.id.second);

                hour.setWrapSelectorWheel(true);
                minute.setWrapSelectorWheel(true);
                second.setWrapSelectorWheel(true);
                if (tmp.getDuration() >= 0) {
                    hour.setValue(tmp.getNumOfHours());
                    minute.setValue(tmp.getNumOfMinutes());
                    second.setValue(tmp.getNumOfSeconds());
                }
                hour.setDisplayedValues(new String[]{"0h", "1h", "2h", "3h", "4h", "5h", "6h", "7h", "8h", "9h",
                        "10h", "11h", "12h", "13h", "14h", "15h", "16h", "17h", "18h", "19h",
                        "20h", "21h", "22h", "23h"});
                minute.setDisplayedValues(new String[]{"0m", "1m", "2m", "3m", "4m", "5m", "6m", "7m", "8m", "9m",
                        "10m", "11m", "12m", "13m", "14m", "15m", "16m", "17m", "18m", "19m",
                        "20m", "21m", "22m", "23m", "24m", "25m", "26m", "27m", "28m", "29m",
                        "30m", "31m", "32m", "33m", "34m", "35m", "36m", "37m", "38m", "39m",
                        "40m", "41m", "42m", "43m", "44m", "45m", "46m", "47m", "48m", "49m",
                        "50m", "51m", "52m", "53m", "54m", "55m", "56m", "57m", "58m", "59m"});
                second.setDisplayedValues(new String[]{"0s", "1s", "2s", "3s", "4s", "5s", "6s", "7s", "8s", "9s",
                        "10s", "11s", "12s", "13s", "14s", "15s", "16s", "17s", "18s", "19s",
                        "20s", "21s", "22s", "23s", "24s", "25s", "26s", "27s", "28s", "29s",
                        "30s", "31s", "32s", "33s", "34s", "35s", "36s", "37s", "38s", "39s",
                        "40s", "41s", "42s", "43s", "44s", "45s", "46s", "47s", "48s", "49s",
                        "50s", "51s", "52s", "53s", "54s", "55s", "56s", "57s", "58s", "59s"});
                AlertDialog.Builder b = new AlertDialog.Builder(TrackActivity.this).setTitle("Set duration").setView(view);
                b.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tmp.setDuration(hour.getValue() * 3600 + minute.getValue() * 60 + second.getValue());
                        showDuration.setText(tmp.getFormatedDuration());
                    }
                });
                b.setNegativeButton("Cancel", null);
                b.show();
            }
        });
        editDate = (ImageView) findViewById(R.id.edit_date);
        editDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GregorianCalendar calendar;
                calendar = new GregorianCalendar();
                if (tmp.getDate() != null) {
                    calendar.setTime(tmp.getDate());
                }
                DatePickerDialog dialog = new DatePickerDialog(TrackActivity.this, new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        GregorianCalendar cal = new GregorianCalendar(year, monthOfYear,
                                dayOfMonth);

                        tmp.setDate(cal.getTime());
                        showDate.setText(tmp.getFormatedDate());
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMaxDate(new Date().getTime());
                dialog.show();
            }
        });
        if (admin) {
            findViewById(R.id.user_container).setVisibility(View.VISIBLE);
            userSpinner = (AppCompatSpinner) findViewById(R.id.user_spinner);
            userSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, users));
        }

        client = new OkHttpClient();
    }

    @Override
    protected void onResume() {
        super.onResume();

        configureView();
        setValues();
    }

    private void setValues() {
        if (track != null) {
            showDate.setText(track.getFormatedDate());
            showDistance.setText(track.getFormatedDistance());
            showDuration.setText(track.getFormatedDuration());
            tmp.setDate(track.getDate());
            tmp.setDuration(track.getDuration());
            tmp.setDistance(track.getDistance());
        }
    }

    private void configureView() {
        editDistance.setVisibility(edit ? View.VISIBLE : View.GONE);
        editDuration.setVisibility(edit ? View.VISIBLE : View.GONE);
        editDate.setVisibility(edit ? View.VISIBLE : View.GONE);
        if (doneItem != null)
            doneItem.setVisible(edit);
        if (editItem != null)
            editItem.setVisible(!edit);
        if (deleteItem != null)
            deleteItem.setVisible(!edit);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.track_menu, menu);
        doneItem = menu.findItem(R.id.done);
        editItem = menu.findItem(R.id.edit);
        editItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                edit = true;
                configureView();
                return true;
            }
        });

        deleteItem = menu.findItem(R.id.delete);
        deleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new AlertDialog.Builder(TrackActivity.this)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this entry?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DeleteTask task = new DeleteTask();
                                task.execute();

                            }
                        }).show();
                return true;
            }
        });
        doneItem.setVisible(edit);
        editItem.setVisible(!edit);
        deleteItem.setVisible(!edit);
        return true;
    }

    class DeleteTask extends AsyncTask<Void, Void, Response> {

        DeleteTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {

            HttpUrl.Builder builder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(Util.HOST)
                    .port(Util.PORT)
                    .addPathSegment(Util.SEGMENT_TRACK)
                    .addPathSegment(track.getId());
            Request request = new Request.Builder()
                    .addHeader("Authorization", Util.getAuthToken(TrackActivity.this))
                    .url(builder.build())
                    .delete()
                    .build();
            try {
                return client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            if (response != null) {
                if (response.code() == 200) {
                    finish();
                } else {
                    Toast.makeText(TrackActivity.this, "Can not delete the entry", Toast.LENGTH_LONG).show();
                }
            }
            Toast.makeText(TrackActivity.this, "Can not delete the entry", Toast.LENGTH_LONG).show();

        }

        @Override
        protected void onCancelled() {
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                saveTrack();
                return true;
            case R.id.edit:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTrack() {
        if (!validate()) return;
        if (track == null)
            track = new Track();
        track.setDuration(tmp.getDuration());
        track.setDistance(tmp.getDistance());
        track.setDate(tmp.getDate());
        if (admin)
            track.setUserid(((User) userSpinner.getSelectedItem()).getId());
        mTrackTask = new TrackTask();
        mTrackTask.execute();
    }

    private boolean validate() {
        if (tmp.getDate() == null) {
            Snackbar.make(showDate, "Date should not be empty", Snackbar.LENGTH_LONG).show();
            return false;
        }
        if (tmp.getDuration() < 1) {
            Snackbar.make(showDuration, "Duration should not be empty", Snackbar.LENGTH_LONG).show();
            return false;
        }
        if (tmp.getDistance() < 1) {
            Snackbar.make(editDistance, "Distance should not be empty", Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    class TrackTask extends AsyncTask<Void, Void, Response> {

        TrackTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            String token = Util.getAuthToken(TrackActivity.this);
            try {
                HttpUrl.Builder builder;
                if (admin)
                    builder = new HttpUrl.Builder()
                            .scheme("http")
                            .host(Util.HOST)
                            .port(Util.PORT)
                            .addPathSegment(Util.SEGMENT_USER)
                            .addPathSegment(track.getUserid())
                            .addPathSegment(Util.SEGMENT_TRACK);
                else {
                    builder = new HttpUrl.Builder()
                            .scheme("http")
                            .host(Util.HOST)
                            .port(Util.PORT)
                            .addPathSegment(Util.SEGMENT_TRACK);
                    if (track.getId() != null && !track.getId().equals(""))
                        builder.addPathSegment(track.getId());
                }
                Gson gson = new GsonFireBuilder().dateSerializationPolicy(DateSerializationPolicy.rfc3339).createGson();

                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(track));
                Request.Builder requestBuilder = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(builder.build());

                if (track.getId() != null && !track.getId().equals(""))
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
            mTrackTask = null;
            if (response != null) {
                if (response.code() == 200 || response.code() == 201) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(TrackActivity.this, "Can't reach server", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(TrackActivity.this, "Can't reach server", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mTrackTask = null;
        }
    }
}
