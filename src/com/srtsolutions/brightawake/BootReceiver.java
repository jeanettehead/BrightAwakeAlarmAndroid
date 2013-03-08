package com.srtsolutions.brightawake;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;


public class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent){
		//schedule alarm
		SharedPreferences prefs = context.getSharedPreferences("com.srtsolutions.brightawake", Context.MODE_PRIVATE);
		long alarmMillis = prefs.getLong(Constants.Alarm_Time, -1);
		Calendar alarmTime = Calendar.getInstance();
		alarmTime.setTimeInMillis(alarmMillis);
		Calendar current = Calendar.getInstance();
		current.setTimeInMillis(System.currentTimeMillis());
		if(alarmTime.after(current)){ //only if alarm is set sometime in the future from where we are do we care
			Intent alarmIntent = new Intent().setClass(context, AlarmReceiver.class);
			PendingIntent alarmAction = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
			
			// Schedule the alarm!
			AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), alarmAction);
			
			Log.v("BrightAwakeAlarm", "Set alarm on reboot");

		}
	}
}
