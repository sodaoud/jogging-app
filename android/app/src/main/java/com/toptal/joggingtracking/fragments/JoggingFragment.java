package com.toptal.joggingtracking.fragments;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toptal.joggingtracking.MainActivity;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.datatype.Track;
import com.toptal.joggingtracking.util.ConstantUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by sofiane on 8/16/16.
 */
public class JoggingFragment extends Fragment {
    private RecyclerView list;
    private Adapter adapter;
    private OkHttpClient client;
    //    private View mainView;
    private View noServerView;
    private View mProgressView;
    private TracksTask mTracksTask;
    private List<Track> tracks;
    private AccountManager am;
    private LinearLayoutManager mLayoutManager;

    public static Fragment newInstance() {

        JoggingFragment fragment = new JoggingFragment();

        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        client = new OkHttpClient();
        tracks = new ArrayList<>();
        am = AccountManager.get(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_jogging, container, false);
        list = (RecyclerView) v.findViewById(R.id.list);
        list.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(mLayoutManager);
//        mainView = v.findViewById(R.id.list_view);
        noServerView = v.findViewById(R.id.no_data);
        mProgressView = v.findViewById(R.id.progress_view);

        adapter = new Adapter();
        list.setAdapter(adapter);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mTracksTask = new TracksTask();
        showProgress(true);
        mTracksTask.execute((Void) null);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        list.setVisibility(show ? View.GONE : View.VISIBLE);
        list.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                list.setVisibility(show ? View.GONE : View.VISIBLE);
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

    class TracksTask extends AsyncTask<Void, Void, Response> {

        TracksTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            String token = ((MainActivity) getActivity()).getAuthToken();
            try {
                Request request = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(ConstantUtil.URL_TRACK)
                        .get()
                        .build();
                return client.newCall(request).execute();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Response response) {
            mTracksTask = null;
            showProgress(false);
            Gson gson = new Gson();
            if (response != null) {
                String stringBody = null;
                try {
                    stringBody = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (response.code() == 200) {
                    tracks.clear();
                    Type type = new TypeToken<List<Track>>() {
                    }.getType();
                    List<Track> tmp = gson.fromJson(stringBody, type);
                    tracks.addAll(tmp);
                    list.getAdapter().notifyDataSetChanged();
//                    adapter.notifyDataSetChanged();

                } else {
                    // TODO show error to the user
                }
            } else {
                // TODO show error to the user
            }
        }

        @Override
        protected void onCancelled() {
            mTracksTask = null;
            showProgress(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
        menu.add("Filter").setTitle("Filter")
                .setIcon(R.drawable.ic_filter_list)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Toast.makeText(getActivity(), "Khra", Toast.LENGTH_LONG).show();
                        return true;
                    }
                });
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

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mDateView;
            TextView mDurationView;
            TextView mDistanceView;
            TextView mSpeedView;

            public ViewHolder(View v) {
                super(v);
                mDateView = (TextView) v.findViewById(R.id.date_field);
                mDurationView = (TextView) v.findViewById(R.id.duration_field);
                mDistanceView = (TextView) v.findViewById(R.id.distance_field);
                mSpeedView = (TextView) v.findViewById(R.id.speed_field);
            }

        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_card, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Track track = tracks.get(position);
            holder.mDateView.setText(track.getDate().toString());
            holder.mDurationView.setText(String.valueOf(track.getDuration()));
            holder.mDistanceView.setText(String.valueOf(track.getDistance()));
            holder.mSpeedView.setText(String.valueOf(track.getSpeed()));
        }

        @Override
        public int getItemCount() {
            return tracks.size();
        }
    }
}
