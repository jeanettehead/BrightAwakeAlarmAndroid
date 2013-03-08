package com.srtsolutions.brightawake;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.content.Context;

public class BrightAlarmMain extends Activity implements Constants{

	protected Context context;
	protected Boolean isAlarmStopped = true;
	protected AlarmManager alarmManager;
	protected TimePicker timePicker;
	protected SeekBar pitchSeeker;
	protected TextView storedAlarmText;
	protected Calendar alarmTime;
	protected SharedPreferences prefs;
	protected PendingIntent alarmAction;
	protected Button cancelButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		setContentView(R.layout.activity_bright_alarm_main);
		prefs = getSharedPreferences("com.srtsolutions.brightawake", Context.MODE_PRIVATE);

		Button setAlarmButton = (Button)findViewById(R.id.set_alarm);
		setAlarmButton.setOnClickListener(setAlarmListener);

		cancelButton = (Button)findViewById(R.id.cancel_alarm);
		cancelButton.setOnClickListener(cancelAlarmListener);

		timePicker = (TimePicker) findViewById(R.id.timePicker1);
		//set time to midnight
		timePicker.setCurrentHour(0);
		timePicker.setCurrentMinute(0);
		//set pitch to 50%
		pitchSeeker = (SeekBar) findViewById(R.id.pitch_control);
		pitchSeeker.setProgress(50);

		storedAlarmText = (TextView) findViewById(R.id.stored_alarm);

		Button testAlarm = (Button) findViewById(R.id.test_alarm);
		testAlarm.setOnClickListener(testAlarmListener);
		
		Button aboutAlarm = (Button) findViewById(R.id.about);
		aboutAlarm.setOnClickListener(aboutAlarmListener);
	}

	@Override
	public void onResume(){
		super.onResume();
		setStoredAlarmText();
	}

	private void storeAlarmTime(){
		prefs.edit().putLong(Alarm_Time, alarmTime.getTimeInMillis()).apply();
		setStoredAlarmText();
	}

	private void setStoredAlarmText(){
		long storedAlarmTime = prefs.getLong(Alarm_Time, -1);
		Calendar storedAlarm = Calendar.getInstance();
		storedAlarm.setTimeInMillis(storedAlarmTime);
		Calendar now = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis());
		if(storedAlarm.after(now)){
			cancelButton.setVisibility(View.VISIBLE);
			SimpleDateFormat format = new SimpleDateFormat("h:mm a  \nMMMM dd, yyyy");
			storedAlarmText.setText("\nalarm set for " + format.format(storedAlarm.getTime()) + "\n" );
		}
		else{
			cancelButton.setVisibility(View.GONE);
			storedAlarmText.setText("\n");
		}
	}

	private OnClickListener setAlarmListener = new OnClickListener() {
		public void onClick(View v) {
			// When the alarm goes off, we want to broadcast an Intent to our
			// BroadcastReceiver.  Here we make an Intent with an explicit class
			// name to have our own receiver (which has been published in
			// AndroidManifest.xml) instantiated and called, and then create an
			// IntentSender to have the intent executed as a broadcast.

			prefs.edit().putInt(Pitch_Percent, pitchSeeker.getProgress()).apply();

			Intent intent = new Intent(BrightAlarmMain.this, AlarmReceiver.class);
			alarmAction = PendingIntent.getBroadcast(BrightAlarmMain.this, 0, intent, 0);


			int hour = timePicker.getCurrentHour();
			int minute = timePicker.getCurrentMinute();

			Calendar current = Calendar.getInstance();
			current.setTimeInMillis(System.currentTimeMillis());

			alarmTime = Calendar.getInstance();
			alarmTime.setTimeInMillis(System.currentTimeMillis());

			alarmTime.set(Calendar.HOUR_OF_DAY, hour);
			alarmTime.set(Calendar.MINUTE, minute);
			alarmTime.set(Calendar.SECOND, 0);
			alarmTime.set(Calendar.MILLISECOND, 0);

			if(alarmTime.before(current))
				alarmTime.add(Calendar.DATE, 1);
			
			storeAlarmTime();


			// Schedule the alarm!
			alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), alarmAction);

		}
	};

	private OnClickListener testAlarmListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			prefs.edit().putInt(Pitch_Percent, pitchSeeker.getProgress()).apply();

			Intent testIntent = new Intent().setClass(context, AlarmRing.class);
			testIntent.putExtra("isTest", true);
			testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(testIntent);
		}
	};

	private OnClickListener cancelAlarmListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent alarmIntent = new Intent(BrightAlarmMain.this, AlarmReceiver.class);
			alarmAction = PendingIntent.getBroadcast(BrightAlarmMain.this, 0, alarmIntent, 0);
			alarmTime = Calendar.getInstance();
			alarmTime.setTimeInMillis(0); //clear out the time
			storeAlarmTime();

			alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			alarmManager.cancel(alarmAction);

			Toast.makeText(context, "alarm cancelled", Toast.LENGTH_SHORT).show();
		}
	};
	
	private OnClickListener aboutAlarmListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(R.string.about_description);
			builder.create().show();
			
		}
	};
}
