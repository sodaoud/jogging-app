package com.toptal.joggingtracking.fragments;

import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.toptal.joggingtracking.R;
import com.toptal.joggingtracking.TrackActivity;
import com.toptal.joggingtracking.datatype.Track;
import com.toptal.joggingtracking.util.Util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import io.gsonfire.util.RFC3339DateFormat;
import okhttp3.HttpUrl;
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
    private View noServerView;
    private View mProgressView;
    private TracksTask mTracksTask;
    private List<Track> tracks;
    private AccountManager am;
    private LinearLayoutManager mLayoutManager;
    private Filter filter;
    private FloatingActionButton fab;

    class Filter {

        Filter() {
            order = ORDER_DESC;
        }

        final static String ORDER_DESC = "-";
        final static String ORDER_ASC = "";

        String begin;
        String end;
        String order;
        String field = "date";
    }

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
        filter = new Filter();
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
        noServerView = v.findViewById(R.id.no_data);
        noServerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTracks();
            }
        });
        mProgressView = v.findViewById(R.id.progress_view);

        adapter = new Adapter();
        list.setAdapter(adapter);

        fab = (FloatingActionButton) v.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewTrack();
            }
        });
        return v;
    }

    private void createNewTrack() {
        Intent i = new Intent(getActivity(), TrackActivity.class);
        Bundle b = new Bundle();
        i.putExtras(b);
        startActivityForResult(i, 432);
    }

    @Override
    public void onResume() {
        super.onResume();
        getTracks();
    }

    private void getTracks() {
        showNoServer(false);
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

    private void showNoServer(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        list.setVisibility(show ? View.GONE : View.VISIBLE);
        list.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                list.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        noServerView.setVisibility(show ? View.VISIBLE : View.GONE);
        noServerView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                noServerView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    class TracksTask extends AsyncTask<Void, Void, Response> {

        TracksTask() {
        }

        @Override
        protected Response doInBackground(Void... params) {
            HttpUrl url = new HttpUrl.Builder()
                    .scheme("http")
                    .host(Util.HOST)
                    .port(Util.PORT)
                    .addPathSegment(Util.SEGMENT_TRACK)
                    .addQueryParameter("sort", filter.order + filter.field)
                    .addQueryParameter("begin", filter.begin)
                    .addQueryParameter("end", filter.end)
                    .build();
            String token = Util.getAuthToken(getActivity());
            try {
                Request request = new Request.Builder()
                        .addHeader("Authorization", token)
                        .url(url)
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
                            } else {
                                showNoServer(true);
                            }
                        }
                    });
                } else {
                    if (tracks.isEmpty())
                        showNoServer(true);
                }
            } else {
                if (tracks.isEmpty())
                    showNoServer(true);
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
        inflater.inflate(R.menu.filter_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.filter) {
            DialogFragment newFragment = new FilterDialogFragment();
            newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
            return true;
        }
        return false;
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_card,
                    parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Track track = tracks.get(position);
            holder.mDateView.setText(track.getFormatedDate());
            holder.mDurationView.setText(track.getFormatedDuration());
            holder.mDistanceView.setText(track.getFormatedDistance());
            holder.mSpeedView.setText(track.getFormatedSpeed());
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getActivity(), TrackActivity.class);
                    i.putExtra(TrackActivity.EDIT, false);
                    i.putExtra(TrackActivity.TRACK, track);
                    startActivity(i);
                }
            });
        }

        @Override
        public int getItemCount() {
            return tracks.size();
        }
    }

    @SuppressLint({"SimpleDateFormat", "ValidFragment"})
    class FilterDialogFragment extends DialogFragment {


        private Calendar beginCal;
        private Calendar endCal;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        private RFC3339DateFormat dateFormatws = new RFC3339DateFormat();
        private AppCompatSpinner order;
        private TextView beginDate;
        private TextView endDate;
        private View view;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.filter_dialog, null);
            beginDate = (TextView) view.findViewById(R.id.show_begin);
            endDate = (TextView) view.findViewById(R.id.show_end);
            order = (AppCompatSpinner) view.findViewById(R.id.order);
            if (filter != null) {
                order.setSelection(filter.order.equals(Filter.ORDER_DESC) ? 0 : 1);
                if (filter.begin != null) {
                    beginCal = new GregorianCalendar();
                    try {
                        beginCal.setTime(dateFormatws.parse(filter.begin));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    beginDate.setText(dateFormat.format(beginCal.getTime()));
                }
                if (filter.end != null) {
                    endCal = new GregorianCalendar();
                    try {
                        endCal.setTime(dateFormatws.parse(filter.end));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    endDate.setText(dateFormat.format(endCal.getTime()));
                }
                order.setSelection(filter.order.equals(Filter.ORDER_DESC) ? 0 : 1);
            }


            initListeners();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            return builder
                    .setTitle("Filter")
                    .setView(view)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Filter",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    if (filter == null) filter = new Filter();
                                    filter.order = order.getSelectedItemPosition() == 0 ? Filter.ORDER_DESC : Filter.ORDER_ASC;
                                    if (beginCal != null)
                                        filter.begin = dateFormatws.format(beginCal.getTime());
                                    if (endCal != null)
                                        filter.end = dateFormatws.format(endCal.getTime());
                                    getTracks();
                                }
                            }).create();
        }

        private void initListeners() {
            beginDate.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Calendar tmp = new GregorianCalendar();
                    if (beginCal != null) {
                        tmp = beginCal;
                    }
                    new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear,
                                              int dayOfMonth) {
                            if (beginCal == null) {
                                beginCal = GregorianCalendar.getInstance();
                                beginCal.set(Calendar.HOUR_OF_DAY, 0);
                                beginCal.set(Calendar.MINUTE, 0);
                                beginCal.set(Calendar.SECOND, 0);
                                beginCal.set(Calendar.MILLISECOND, 0);
                            }
                            beginCal.set(year, monthOfYear, dayOfMonth);
                            beginDate.setText(dateFormat.format(beginCal.getTime()));
                            if (endCal != null && (endCal.get(Calendar.DAY_OF_MONTH) < dayOfMonth
                                    || endCal.get(Calendar.MONTH) < monthOfYear
                                    || endCal.get(Calendar.YEAR) < year)) {
                                endCal.set(year, monthOfYear, dayOfMonth);
                                endDate.setText(dateFormat.format(endCal.getTime()));
                            }
                        }
                    }, tmp.get(Calendar.YEAR), tmp.get(Calendar.MONTH), tmp
                            .get(Calendar.DAY_OF_MONTH)).show();
                }
            });
            endDate.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    Calendar tmp = new GregorianCalendar();
                    if (endCal != null) {
                        tmp = endCal;
                    }
                    new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear,
                                              int dayOfMonth) {
                            if (endCal == null) {
                                endCal = GregorianCalendar.getInstance();
                                endCal.set(Calendar.HOUR_OF_DAY, 0);
                                endCal.set(Calendar.MINUTE, 0);
                                endCal.set(Calendar.SECOND, 0);
                                endCal.set(Calendar.MILLISECOND, 0);
                            }
                            endCal.set(year, monthOfYear, dayOfMonth);
                            endDate.setText(dateFormat.format(endCal.getTime()));
                            if (beginCal != null && (beginCal.get(Calendar.DAY_OF_MONTH) > dayOfMonth
                                    || beginCal.get(Calendar.MONTH) > monthOfYear
                                    || beginCal.get(Calendar.YEAR) > year)) {
                                beginCal.set(year, monthOfYear, dayOfMonth);
                                beginDate.setText(dateFormat.format(beginCal.getTime()));
                            }
                        }
                    }, tmp.get(Calendar.YEAR), tmp.get(Calendar.MONTH), tmp
                            .get(Calendar.DAY_OF_MONTH)).show();
                }
            });
        }


    }
}
