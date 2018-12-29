package nl.oxanvanleeuwen.android.alarmsync;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;

public class AlarmJobService extends JobService {
    private static final String TAG = "AlarmSync/AlarmJobService";

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        try {
            Context context = getApplicationContext();
            AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
            AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();

            String time = "";
            String date = "";
            if (nextAlarm != null) {
                Instant instant = Instant.ofEpochMilli(nextAlarm.getTriggerTime());
                time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant);
                date = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(instant);
            }

            JSONObject attributes = new JSONObject();
            attributes.put("date", date);
            JSONObject body = new JSONObject();
            body.put("state", time);
            body.put("attributes", attributes);

            String url = String.format("%s/api/states/%s", SyncConfiguration.HOST, SyncConfiguration.SENSOR);
            JsonObjectRequest request = new JsonObjectRequest(url, body, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    jobFinished(jobParameters, false);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Failed to submit to API", error);
                    jobFinished(jobParameters, false);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", String.format("Bearer %s", SyncConfiguration.TOKEN));
                    return params;
                }
            };

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Failed to process job", e);
            jobFinished(jobParameters, false);
        }

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}