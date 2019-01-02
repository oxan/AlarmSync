package nl.oxanvanleeuwen.android.alarmsync;

import android.app.AlarmManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmSync/AlarmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
            return;

        Log.d(TAG, "Received next alarm clock broadcast");
        ComponentName serviceComponent = new ComponentName(context, AlarmJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setOverrideDeadline(5000);
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }
}
