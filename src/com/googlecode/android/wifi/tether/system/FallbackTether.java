package com.googlecode.android.wifi.tether.system;

import java.lang.reflect.Method;
import android.app.Application;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public class FallbackTether extends Application {
	public static final String TAG = "TETHER -> FallbackTether";
	
	 public static void controlStockTether(WifiManager wifimanager, boolean enabled, boolean encryptionEnabled, String passphrase) throws Exception {
		 
    	Log.d(TAG, "fallback tether mode starting: " + enabled);
    	//Config setup
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "TetherFallback"; 
        
        
        if(!encryptionEnabled){
        	Log.d(TAG, "open AP mode ");
        	config.allowedKeyManagement.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else
        {
        	Log.d(TAG, "Shared AP mode ");
        	config.allowedKeyManagement.set(WifiConfiguration.AuthAlgorithm.SHARED);
        	config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        	config.preSharedKey = passphrase;
        }
        
        //TODO: tkip
        //config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        //config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        
        //start AP
        Method method1 = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        method1.invoke(wifimanager, config, enabled); 
        
        
        
        //checkstate
        Method getWifiApState = wifimanager.getClass().getMethod("getWifiApState");
        Integer state = (Integer) getWifiApState.invoke(wifimanager);
    	Log.d(TAG, "fallback tether mode state is: " + state);
	}

}