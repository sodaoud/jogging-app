package com.toptal.joggingtracking.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.datatype.HttpUtil;
import com.toptal.joggingtracking.datatype.Track;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import io.gsonfire.util.RFC3339DateFormat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;


/**
 * Created by sofiane on 8/21/16.
 */

public class ReportViewFragment extends Fragment {

    public static final String BEGIN = "BEGIN";
    public static final String END = "END";
    public static final String TITLE = "TITLE";

    private RFC3339DateFormat dateFormatws = new RFC3339DateFormat();
    Date begin;
    Date end;
    String title;
    private TextView mDuration;
    private TextView mMaxSpeed;
    private TextView mMaxDuration;
    private TextView mMaxDistance;
    private TextView mDistance;
    private TextView mSpeed;
    private TracksTask mTracksTask;
    private OkHttpClient client;
    private Track[] tracks;
    private TextView mTotalRuns;


    public static ReportViewFragment newInstance(Date begin, Date end) {

        ReportViewFragment fragment = new ReportViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(BEGIN, begin);
        args.putSerializable(END, end);
        args.putString(TITLE, "From " + Util.getSimpleDateFormat().format(begin) + " to " + Util.getSimpleDateFormat().format(end));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        begin = (Date) getArguments().getSerializable(BEGIN);
        end = (Date) getArguments().getSerializable(END);
        title = getArguments().getString(TITLE);
        client = new OkHttpClient();
    }

    @Override
    public void onResume() {
        super.onResume();
        getTracks();
    }

    private void getTracks() {

        mTracksTask = new TracksTask();
        mTracksTask.execute((Void) null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.report_view, container, false);
        ((TextView) v.findViewById(R.id.title)).setText(title);
        mDuration = (TextView) v.findViewById(R.id.duration);
        mSpeed = (TextView) v.findViewById(R.id.speed);
        mDistance = (TextView) v.findViewById(R.id.distance);
        mMaxDistance = (TextView) v.findViewById(R.id.max_distance);
        mMaxDuration = (TextView) v.findViewById(R.id.max_duration);
        mTotalRuns = (TextView) v.findViewById(R.id.total_runs);
        mMaxSpeed = (TextView) v.findViewById(R.id.max_speed);
        return v;
    }

    class TracksTask extends AsyncTask<Void, Void, HttpUtil> {

        TracksTask() {
        }

        @Override
        protected HttpUtil doInBackground(Void... params) {
            HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(Util.HOST)
                    .port(Util.PORT)
                    .addPathSegment(Util.SEGMENT_TRACK)
                    .addQueryParameter("begin", dateFormatws.format(begin))
                    .addQueryParameter("end", dateFormatws.format(end));
            String token = Util.getAuthToken(getActivity());
            try {
                Request request = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(urlBuilder.build())
                        .get()
                        .build();
                return new HttpUtil(client.newCall(request).execute());

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final HttpUtil response) {
            mTracksTask = null;
            Gson gson = new Gson();
            if (response != null) {
                String stringBody = null;
                stringBody = response.body().string();
                if (response.code() == 200) {
                    Type type = new TypeToken<Track[]>() {
                    }.getType();
                    tracks = gson.fromJson(stringBody, type);
                    populateView();
                    return;
                } else if (response.code() == 401) {
                    Util.getNewAuthToken(new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            if (msg.what == Util.SUCCES) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getTracks();
                                    }
                                });
                            }
                        }
                    });
                    return;
                }
            }
            Toast.makeText(getActivity(), "An error has occured", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onCancelled() {
            mTracksTask = null;
        }
    }

    private void populateView() {

        int duration = 0;
        double maxSpeed = 0d;
        int maxDuration = 0;
        int maxDistance = 0;
        int distance = 0;
        for (Track t : tracks) {
            duration += t.getDuration();
            distance += t.getDistance();
            double cSpeed = (double) t.getDistance() / t.getDuration();
            if (t.getDistance() > maxDistance) maxDistance = t.getDistance();
            if (t.getDuration() > maxDuration) maxDuration = t.getDuration();
            if (cSpeed > maxSpeed) maxSpeed = cSpeed;
        }

        mSpeed.setText(Util.formatSpeed(distance, duration));
        mMaxSpeed.setText(Util.formatSpeed(maxSpeed));
        mDuration.setText(Util.formatDuration(duration));
        mMaxDuration.setText(Util.formatDuration(maxDuration));
        mDistance.setText(Util.formatDistance(distance));
        mMaxDistance.setText(Util.formatDistance(maxDistance));
        mTotalRuns.setText("" + tracks.length);
    }
}
