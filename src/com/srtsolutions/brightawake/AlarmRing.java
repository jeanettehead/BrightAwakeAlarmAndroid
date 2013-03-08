package com.srtsolutions.brightawake;

import java.io.IOException;
import java.util.Calendar;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

public class AlarmRing extends Activity implements SurfaceHolder.Callback, Constants {

	protected Boolean thisIsAlarmInstance = false;
	AsyncRing ringTask;
	public byte generatedSnd[];
	AlarmRing activity;
	protected int pitchPercent;
	final int Min_Pitch_Frequency = 350;
	final int Max_Pitch_Frequency = 800;
	protected int calculatedPitch;
	protected int alarmFrequency;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	protected android.hardware.Camera camera;
	protected SharedPreferences prefs;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm_ring);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		camera = Camera.open();

		prefs = this.getSharedPreferences("com.srtsolutions.brightawake", Context.MODE_PRIVATE);
		alarmFrequency = prefs.getInt(Alarm_Frequency, 500);

		surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		setUpSound();

		Button stopButton = (Button) findViewById(R.id.stop_alarm);
		stopButton.setOnClickListener(stopAlarmListener);
		
		Button snoozeButton = (Button) findViewById(R.id.snooze_alarm);
		snoozeButton.setOnClickListener(snoozeAlarmListener);
		activity = this;

		if(!prefs.getBoolean(Constants.Alarm_In_Progress, false))
		{
			thisIsAlarmInstance = true;
			prefs.edit().putBoolean(Constants.Alarm_In_Progress, true).apply();
			ringTask = new AsyncRing();
			ringTask.execute(this);
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

	}

	@Override
	public void onDestroy(){
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		if(ringTask!= null){
			ringTask.cancel(true);
		}
		super.onDestroy();
		try{
			camera.release();
		}
		catch(RuntimeException e){
			e.printStackTrace();
		}
		
		prefs.edit().putBoolean(Constants.Alarm_In_Progress, false).apply();
	}

	@Override
	public void onStart(){
		super.onStart();
		if(camera == null)
			try{
				camera = Camera.open();
			}
		catch(RuntimeException e){
			e.printStackTrace();
		}
	}

	private void setUpSound(){
		pitchPercent = prefs.getInt(Pitch_Percent, 50);
		calculatedPitch = (int) ((Max_Pitch_Frequency - Min_Pitch_Frequency) * (pitchPercent/100.0)) + Min_Pitch_Frequency;
		generatedSnd = this.createSound(calculatedPitch, alarmFrequency);
	}

	private class AsyncRing extends AsyncTask<Activity, Integer, String>
	{
		private RelativeLayout background;
		private Boolean bgIsWhite = false;
		private Boolean isFirstTime = true;
		private Activity activity;
		private AudioTrack audio = new AudioTrack(AudioManager.STREAM_MUSIC,
				8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
				AudioTrack.MODE_STATIC);

		void playSound(){

			if(!isFirstTime)
				audio.stop();
			else
				isFirstTime = false;

			audio.reloadStaticData();
			audio.play();
		}

		@Override
		protected void onProgressUpdate(Integer... values)
		{
			if(background == null)
				background = (RelativeLayout) activity.findViewById(R.id.background);

			if(bgIsWhite){
				background.setBackgroundColor(Color.BLACK);
				bgIsWhite = false;
			}
			else{
				background.setBackgroundColor(Color.WHITE);
				bgIsWhite = true;
			}
		}

		@Override
		protected String doInBackground(Activity... values) {
			activity = values[0];
			audio.write(generatedSnd, 0, generatedSnd.length);

			if(camera == null)
				try{
					camera = Camera.open();
				}
			catch(RuntimeException e){
				e.printStackTrace();
			}
			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			if(camera != null)
				camera.startPreview();

			while(!this.isCancelled()) {
				Parameters camParams;
				try{
					if(camera != null){
						camParams = camera.getParameters();
						camParams.setFlashMode(Parameters.FLASH_MODE_TORCH);
						camera.setParameters(camParams);
					}
				}
				catch(Exception e)
				{e.printStackTrace();

				}
				vibrator.vibrate(alarmFrequency);
				publishProgress();
				playSound();
				try {
					Thread.sleep(alarmFrequency);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if(!this.isCancelled()){
					try{
						if(camera !=null)
						{
							camParams = camera.getParameters();
							camParams.setFlashMode(Parameters.FLASH_MODE_OFF);
							camera.setParameters(camParams);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
					vibrator.cancel();
					publishProgress();

					try {
						Thread.sleep(alarmFrequency);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
					this.onCancelled();

			}
			camera.release();
			this.onCancelled();

			return null;
		}

		@Override
		protected  void onCancelled(){
			super.onCancelled();

			audio.stop();
			if(camera != null)
				camera.release();

			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.cancel();
		}
	}

	protected byte[] createSound(int frequency, int length){
		double duration = length/1000.0;          // seconds
		double freqOfTone = frequency;           // hz
		int sampleRate = 8000;              // a number

		double dnumSamples = duration * sampleRate;
		dnumSamples = Math.ceil(dnumSamples);
		int numSamples = (int) dnumSamples;
		double sample[] = new double[numSamples];
		generatedSnd = new byte[4 * numSamples];  // 2 * 2 * numsamples.  added a second *2 to generate white space in the tone

		for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
			sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
		}

		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalized.
		// convert to 16 bit pcm sound array
		// assumes the sample buffer is normalised.
		int idx = 0;
		int i = 0 ;

		int ramp = numSamples / 10 ;                                    // Amplitude ramp as a percent of sample count


		for (i = 0; i< ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
			double dVal = sample[i];
			// Ramp up to maximum
			final short val = (short) ((dVal * 32767 * i/ramp));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
		}


		for (i = i; i< numSamples - ramp; ++i) {                        // Max amplitude for most of the samples
			double dVal = sample[i];
			// scale to maximum amplitude
			final short val = (short) ((dVal * 32767));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
		}

		for (i = i; i< numSamples; ++i) {                               // Ramp amplitude down
			double dVal = sample[i];
			// Ramp down to zero
			final short val = (short) ((dVal * 32767 * (numSamples-i)/ramp ));
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
		}
		return generatedSnd;
	}

	private OnClickListener stopAlarmListener = new OnClickListener() {

		public void onClick(View v) {
		//	Toast.makeText(AlarmRing.this, "exit",
		//			Toast.LENGTH_LONG).show();
			activity.finish();
		}
	};

	private OnClickListener snoozeAlarmListener = new OnClickListener() {

		public void onClick(View v) {
			Intent intent = new Intent(AlarmRing.this, AlarmReceiver.class);
			PendingIntent alarmAction = PendingIntent.getBroadcast(AlarmRing.this, 0, intent, 0);

			Calendar snoozeTime = Calendar.getInstance();
			snoozeTime.setTimeInMillis(System.currentTimeMillis());
			snoozeTime.add(Calendar.SECOND, 5);	//TODO: minute
			
			// Schedule the alarm!
			AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime.getTimeInMillis(), alarmAction);
			
			activity.finish();

		}
	};
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		try {
			camera.setPreviewDisplay(holder);
		}
		catch(IOException e){
			e.printStackTrace();
		}

	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

}
