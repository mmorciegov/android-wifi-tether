/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2011 by Harald Mueller
 */

package com.googlecode.android.wifi.tether;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TetherServiceReceiver extends BroadcastReceiver {
    final static String TAG = "ServiceStartupReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {

		Intent serviceIntent = new Intent();
		serviceIntent.setComponent(
				new ComponentName("com.googlecode.android.wifi.tether", 
						"com.googlecode.android.wifi.tether.TetherService"));

		Log.d(TAG, "onReceive " + intent.getAction());
        if (intent.getAction().equals(TetherService.SERVICEMANAGE_INTENT)) {
        	switch (intent.getIntExtra("state", TetherService.SERVICE_START)) {
        	case TetherService.SERVICE_START :
        		if (TetherService.singleton == null) {
        			context.startService(serviceIntent);
        		}
        		else {
        			sendServiceBroadcast(context, TetherService.SERVICE_STARTED);
        		}
        		break;
        	case TetherService.SERVICE_STARTED :
        		if (TetherService.singleton != null) 
 	   				TetherService.singleton.start();
 	   			break;
        	case TetherService.SERVICE_STOP :
        		if (TetherService.singleton != null) 
 	   				TetherService.singleton.stop();      		
        		break;
        	case TetherService.SERVICE_STOPPED :
        		if (TetherService.singleton != null) 
 	   				TetherService.singleton.stopSelf();
 	   			break;
        	}
        }
    }
    
	private void sendServiceBroadcast(Context context, int state) {
    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
    	intent.putExtra("state", state);
    	context.sendBroadcast(intent);
	}
}
