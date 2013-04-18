
package com.srtsolutions.brightawake;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver
{
	Context context;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = context.getSharedPreferences("com.srtsolutions.brightawake", Context.MODE_PRIVATE);
		if(prefs.getBoolean(Constants.Alarm_In_Progress, false)){
			return;
		}
		KeyguardManager key = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
	     KeyguardLock lock = key.newKeyguardLock("keyguardlock tag");
	     lock.disableKeyguard();
	    // Log.v(TAG, "alarm: disabled keyguard.");
		
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wake = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Alarm is Started");
		wake.acquire();
		Intent mainIntent = new Intent().setClass(context, AlarmRing.class);
		mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(mainIntent);
	}
} 

