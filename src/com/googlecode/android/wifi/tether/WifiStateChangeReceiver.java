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
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WifiStateChangeReceiver extends BroadcastReceiver {
    final static String TAG = "WifiStateChangeReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
		if (TetherService.singleton != null 
				&& TetherService.singleton.getState() == TetherService.STATE_RUNNING) { 
			Log.d(TAG, "Phone takes control back over the wifi-interface!");
			((TetherApplication)TetherService.singleton.getApplication()).displayToastMessage("What the hell!\nYour phone takes back control over the wifi-interface.\nImmediate tethering-shutdown NOW!");
			TetherService.singleton.stop();  
		}
    }
}
