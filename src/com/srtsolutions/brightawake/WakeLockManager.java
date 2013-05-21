package com.srtsolutions.brightawake;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockManager {

	private static WakeLock wakeLock;

	public WakeLockManager(){
	}


	public Boolean acquireLock(Context context){
		if(wakeLock == null){
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Alarm is Started");
			wakeLock.setReferenceCounted(false);
		}
		if(!wakeLock.isHeld()){

			wakeLock.acquire();
			return true;
		}
		return false;
	}

	public void releaseLock(){
		if(wakeLock.isHeld())
			wakeLock.release();
	}
}
