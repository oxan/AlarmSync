package nl.oxanvanleeuwen.android.alarmsync;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmSync/AlarmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
            return;

        Log.d(TAG, "Received next alarm clock broadcast");
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            Log.wtf(TAG, "Alarm manager is NULL");
            return;
        }

        AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();
        Instant instant = nextAlarm != null ? Instant.ofEpochMilli(nextAlarm.getTriggerTime()) : null;
        String date = instant != null ? DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(instant) : "1970-01-01";
        String time = instant != null ? DateTimeFormatter.ofPattern("HH:mm:ss")  .withZone(ZoneId.systemDefault()).format(instant) : "00:00:00";
        Log.d(TAG, "Submitting next alarm at " + date + " " + time + " to API");

        JSONObject body;
        try {
            body = new JSONObject();
            body.put("entity_id", SyncConfiguration.ENTITY_ID);
            body.put("time", time);
            body.put("date", date);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize JSON body", e);
            return;
        }

        final byte[] json = body.toString().getBytes();
        final PendingResult pendingResult = goAsync();

        String url = String.format("%s/api/services/input_datetime/set_datetime", SyncConfiguration.HOST);
        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Job finished, got response from API: " + response);
                pendingResult.finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Failed to submit to API", error);
                pendingResult.finish();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", String.format("Bearer %s", SyncConfiguration.TOKEN));
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                return json;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000, 3, 1));
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }
}
