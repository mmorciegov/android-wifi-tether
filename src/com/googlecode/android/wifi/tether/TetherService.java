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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;

import com.googlecode.android.wifi.tether.data.ClientData;
import com.googlecode.android.wifi.tether.system.Configuration;
import com.googlecode.android.wifi.tether.system.WimaxHelper;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class TetherService extends Service {

	public static final String STATECHANGED_INTENT = "com.googlecode.android.wifi.tether.intent.STATE";
	public static final String SERVICEMANAGE_INTENT = "com.googlecode.android.wifi.tether.intent.MANAGE";
	public static final String TRAFFICCOUNT_INTENT = "com.googlecode.android.wifi.tether.intent.TRAFFIC";
	public static final String QUOTACOUNT_INTENT = "com.googlecode.android.wifi.tether.intent.QUOTA";
	public static final String COUNTDOWN_INTENT = "com.googlecode.android.wifi.tether.intent.COUNTDOWN";
	public static final String KEEPALIVE_INTENT = "com.googlecode.android.wifi.tether.intent.KEEPALIVE";
	public static final String COUNTDOWNTIMER_INTENT = "com.googlecode.android.wifi.tether.intent.COUNTDOWNTIMER";
	
	private static final String TAG = "TetherService";
	
	// "Tethering"-states
	public final static int STATE_RUNNING       = 1;
	public final static int STATE_IDLE          = 2;
	public final static int STATE_STARTING      = 3;
	public final static int STATE_STOPPING      = 4;
	public final static int STATE_RESTARTING    = 5;
	public final static int STATE_FAILURE_LOG   = 6;
	public final static int STATE_FAILURE_EXE   = 7;
	
	// "Service"-states
	public final static int SERVICE_STARTED     = 8;
	public final static int SERVICE_START       = 9;
	public final static int SERVICE_STOPPED     = 11;
	public final static int SERVICE_STOP        = 10;

	private final Binder binder = new LocalBinder();
	
	private TetherApplication application = null;
	public static TetherService singleton = null;

	// Data counters
	private Thread trafficCounterThread = null;
	// Client-Connect Thread
	private Thread clientConnectThread = null;
	//
	private Thread shutdownIdleCheckerThread = null;
	private Thread shutdownTimerCheckerThread = null;
	private Thread shutdownQuotaCheckerThread = null;
	private Thread keepAliveCheckerThread = null;
	
	// WifiManager
	private WifiManager wifiManager;
	private boolean origWifiState;
	private WifiManager.WifiLock wifiLock = null;
	
	// Bluetooth
	private BluetoothAdapter btAdapter;
	private boolean origBtState;
		
	// Default state
	private int state = STATE_IDLE;
		
	// timestamp of last counter-update
	long timestampCounterUpdate = 0;
	boolean autoShutdown = false;
	
	// Setforeground
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private Object[] mSetForegroundArgs = new Object[1];
	
    private static final Class<?>[] mSetForegroundSignature = new Class[] {
        boolean.class};
    private static final Class<?>[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {
        boolean.class};
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        TetherService getService() {
            return TetherService.this;
        }
    }

	@Override
	public void onCreate() {
		Log.d(TAG, ">>>>>>>>>>>>> Tethering-Service started! <<<<<<<<<<<<<");
		// Init foreground
		initForeground();
		
		// Initialize TetherApplication
		application = (TetherApplication)getApplication();
		
		// Initialize itself
		singleton = this;
		
		// Initialize WifiManager
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
        
        // Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Init timeStampCounterUpdate
        timestampCounterUpdate = System.currentTimeMillis();
        
        // Check state by getprop-tether value.
		String tetherStatus = application.coretask.getProp("tether.status");
		
		if (tetherStatus.equals("running")){

			clientConnectEnable(true);
    		trafficCounterEnable(true);

    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownidle", false);
    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdowntimer", false);
       		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownquota", false);
    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownkeepalive", false);
       		TetherService.this.application.preferenceEditor.commit();

       		if (TetherService.this.application.settings.getBoolean("shutdownpref", false))
    			shutdownIdleCheckerEnable(true);

       		if (TetherService.this.application.settings.getBoolean("shutdowntimerpref", false))
    			shutdownTimerCheckerEnable(true);

    		if (TetherService.this.application.settings.getBoolean("quotashutdownpref", false))
    			shutdownQuotaCheckerEnable(true);

    		if (TetherService.this.application.settings.getBoolean("keepalivecheckpref", false))
    			keepAliveCheckerEnable(true);

    		state = STATE_RUNNING;
    		sendStateBroadcast(STATE_RUNNING);
     		startForegroundCompat(-1, application.getStartNotification());
		}
        else {
        	// Send a "state"-broadcast - Tethering not running = idle
        	sendStateBroadcast(STATE_IDLE);
        }
		if (tetherStatus.equals("running") == false)
			sendManageBroadcast(SERVICE_STARTED);
	}

	private void initForeground() {
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
            return;
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, ">>>>>>>>>>>>> Tethering-Service stopped! <<<<<<<<<<<<<");
		singleton = null;
		super.onDestroy();
	}

	
    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    /*
	@Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    void handleCommand(Intent intent) {
        // Nothing
    }	
	
	// ===========================================================
	// Public Methods
	// =========================================================== 
	// Public Methods for service-handling
    public void start() {
    	sendStateBroadcast(STATE_STARTING);
    	state = STATE_STARTING;
    	autoShutdown = false;
 		new Thread(new Runnable(){
			public void run(){
 		    	// Check if binaries need to be updated
		    	if (application.binariesExists() == false || application.coretask.filesetOutdated()) {
		        	if (application.coretask.hasRootPermission()) {
		        		application.installFiles();
		        	}
		        }
		    	
		    	origWifiState = wifiManager.isWifiEnabled();
		    	origBtState = btAdapter.isEnabled();

		    	boolean waitForShutdown = false;
		    	
		        // Check if we need to disable wimax
		        application.updateDeviceParameters();
		        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
		        	Log.d(TAG, "Disabling 4G ...");
		        	WimaxHelper.samsungWimax(TetherService.this, false);
		        	waitForShutdown = true;
		        }
		        
			    // Disable Wifi.
		        disableWifiAndBt(waitForShutdown);
		        
		        // Check if "auto"-setup method is selected
		        boolean reloadDriver = application.settings.getBoolean("driverreloadpref", true);
		        String setupMethod = application.settings.getString("setuppref", "auto");
		        boolean active4G = application.settings.getBoolean("enable4gpref", true);
		        if (setupMethod.equals("auto")) {
		        	setupMethod = application.getDeviceParameters().getAutoSetupMethod();
		        }
		        // Don't stop wifi if we want softap or netd
		    	if (setupMethod.startsWith("softap") || setupMethod.equals("netd")) {
		    		if (reloadDriver == false) {
		    			enableAndDisconnectWifi();
		    		}
		    	}
		        
			    // Generate configuration
		    	application.updateConfiguration();
		    	
		    	// Check if tether-service is already-running
		    	if (state != STATE_RUNNING) {
			    	// Starting service
			    	if (application.coretask.runRootCommand(application.coretask.DATA_FILE_PATH+"/bin/tether start")) {
						// Acquire Wakelock
			    		application.acquireWakeLock();
			    		state = STATE_RUNNING;
			    	}
			    	else {
			    		state = STATE_FAILURE_EXE;
			    	}
		    	}
				
		    	// Check if tether.status was set to "running"
		    	String wifiStatus = application.coretask.getProp("tether.status");
				if (wifiStatus.equals("running") == false) {
					state = STATE_FAILURE_LOG;
				}
				else {
					clientConnectEnable(true);
		    		trafficCounterEnable(true);

		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownidle", false);
		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdowntimer", false);
		       		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownquota", false);
		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownkeepalive", false);
		       		TetherService.this.application.preferenceEditor.commit();
		    		if (TetherService.this.application.settings.getBoolean("shutdownpref", false))
		    			shutdownIdleCheckerEnable(true);

		       		if (TetherService.this.application.settings.getBoolean("shutdowntimerpref", false))
		    			shutdownTimerCheckerEnable(true);

		       		if (TetherService.this.application.settings.getBoolean("quotashutdownpref", false))
		    			shutdownQuotaCheckerEnable(true);

		    		if (TetherService.this.application.settings.getBoolean("keepalivecheckpref", false))
		    			keepAliveCheckerEnable(true);
				}
				
				// Enable 4G again
				if (active4G) {
			        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
			        	Log.d(TAG, "Enabling 4G ...");
			        	WimaxHelper.samsungWimax(TetherService.this, true);
			        }				
				}
		    	sendStateBroadcast(state);
			}
		}).start();
 		startForegroundCompat(-1, application.getStartNotification());
    }
    public void stop() {
    	sendStateBroadcast(STATE_STOPPING);
    	state = STATE_STOPPING;
 		new Thread(new Runnable(){
			public void run(){   	

		        // Check if we need to disable wimax
		        application.updateDeviceParameters();
		        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
		        	Log.d(TAG, "Disabling 4G ...");
		        	WimaxHelper.samsungWimax(TetherService.this, false);
		        	try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// nothing
					}
		        }
				
				// Disabling polling-threads
		    	trafficCounterEnable(false);
		    	clientConnectEnable(false);
	    		keepAliveCheckerEnable(false);
		    	shutdownIdleCheckerEnable(false);
		    	shutdownTimerCheckerEnable(false);
		    	shutdownQuotaCheckerEnable(false);

		    	application.releaseWakeLock();
		    	if (application.coretask.runRootCommand(application.coretask.DATA_FILE_PATH+"/bin/tether stop") == false) {
		    		state = STATE_FAILURE_EXE;
		    	}
		    	TetherService.this.application.notificationManager.cancelAll();
		    		
		    	
		    	// Check if "auto"-setup method is selected
		    	boolean reloadDriver = application.settings.getBoolean("driverreloadpref", true);
		        String setupMethod = application.settings.getString("setuppref", "auto");
		        boolean active4G = application.settings.getBoolean("enable4gpref", true);
		        if (setupMethod.equals("auto")) {
		        	setupMethod = application.getDeviceParameters().getAutoSetupMethod();
		        }
		        // Don't stop wifi if we want softap or netd
		    	if (setupMethod.startsWith("softap") || setupMethod.equals("netd")) {
			    	if (reloadDriver == false) {
			    		disableWifiAndBt(false);
			    	}
		    	}	
		    	
		    	// Re-Enable wifi if it was enabled
	    		enableWifiAndBt(false);

				// Enable 4G again
	    		if (active4G) {
			        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
			        	Log.d(TAG, "Enabling 4G ...");
			        	WimaxHelper.samsungWimax(TetherService.this, true);
			        }				
	    		}
		        
		    	// Check for failed-state 
		    	if (state != STATE_FAILURE_EXE) {
		    		state = STATE_IDLE;
		    	}
		    	sendStateBroadcast(state);   
		    	sendManageBroadcast(SERVICE_STOPPED);
		    	
		    	if (TetherService.this.autoShutdown) {
		   			TetherService.this.application.shutdownHandler.sendEmptyMessage(0);
		    	}
			}
		}).start();
 		stopForegroundCompat(-1);
    }

    public void restart() {
    	state = STATE_RESTARTING;
    	autoShutdown = false;
    	sendStateBroadcast(state);
 		new Thread(new Runnable(){
			public void run(){     
		    	// Disabling polling-threads
		    	trafficCounterEnable(false);
		    	clientConnectEnable(false);
	    		keepAliveCheckerEnable(false);
		    	shutdownIdleCheckerEnable(false);
		    	shutdownTimerCheckerEnable(false);
		    	shutdownQuotaCheckerEnable(false);

		        application.updateDeviceParameters();
		        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
		        	Log.d(TAG, "Disabling 4G ...");
		        	WimaxHelper.samsungWimax(TetherService.this, false);
		        	try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// nothing
					}
		        }
		    	
		    	if (application.coretask.runRootCommand(application.coretask.DATA_FILE_PATH+"/bin/tether stop") == false) {
		    		state = STATE_FAILURE_EXE;
		    	}
		    	
		        // Disable Wifi.
		        disableWifiAndBt(false);
		        
		    	// Check if "auto"-setup method is selected
		        boolean reloadDriver = application.settings.getBoolean("driverreloadpref", true);
		        String setupMethod = application.settings.getString("setuppref", "auto");
		        if (setupMethod.equals("auto")) {
		        	setupMethod = application.getDeviceParameters().getAutoSetupMethod();
		        }

		        // Don't stop wifi if we want softap or netd
		    	if (setupMethod.startsWith("softap") || setupMethod.equals("netd")) {
			    	if (reloadDriver == false) {
			    		enableAndDisconnectWifi();
			    	}
		    	}
		        
		    	// Generate configuration
		    	application.updateConfiguration();
		    	
		    	// Check if tether-service is already-running
		    	if (state != STATE_RUNNING) {
			    	// Starting service
			    	if (application.coretask.runRootCommand(application.coretask.DATA_FILE_PATH+"/bin/tether start")) {
			    		state = STATE_RUNNING;
			    	}
			    	else {
			    		state = STATE_FAILURE_EXE;
			    	}
		    	}

				// Enable 4G again
		        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700)) {
		        	Log.d(TAG, "Enabling 4G ...");
		        	WimaxHelper.samsungWimax(TetherService.this, true);
		        }				
		    	
		    	// Check if tether.status was set to "running"
		    	String wifiStatus = application.coretask.getProp("tether.status");
				if (wifiStatus.equals("running") == false) {
					state = STATE_FAILURE_LOG;
				}
				else {

					clientConnectEnable(true);
		    		trafficCounterEnable(true);

		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownidle", false);
		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdowntimer", false);
		       		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownquota", false);
		    		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownkeepalive", false);
		       		TetherService.this.application.preferenceEditor.commit();
		    		if (TetherService.this.application.settings.getBoolean("shutdownpref", false))
		    			shutdownIdleCheckerEnable(true);

		       		if (TetherService.this.application.settings.getBoolean("shutdowntimerpref", false))
		    			shutdownTimerCheckerEnable(true);

		       		if (TetherService.this.application.settings.getBoolean("quotashutdownpref", false))
		    			shutdownQuotaCheckerEnable(true);

		    		if (TetherService.this.application.settings.getBoolean("keepalivecheckpref", false))
		    			keepAliveCheckerEnable(true);
				}
		    	sendStateBroadcast(state);			
			}
		}).start();
    }
    
    public void reloadACRules() {
    	try {
	    	Log.d(TAG, "Restarting iptables for access-control-changes!");
			if (application.coretask.runRootCommand(application.coretask.DATA_FILE_PATH+"/bin/tether restartsecwifi") == false) {
				application.displayToastMessage(getString(R.string.global_application_error_restartsecwifi));
				return;
			}
		} catch (Exception e) {
			// nothing
		}
    }
    
    public int getState() {
		return state;
	}

	public void sendStateBroadcast(int state) {
    	Intent intent = new Intent(STATECHANGED_INTENT);
    	intent.setAction(STATECHANGED_INTENT);
    	intent.putExtra("state", state);
    	sendBroadcast(intent);
    }
    
	public void sendManageBroadcast(int state) {
    	Intent intent = new Intent(SERVICEMANAGE_INTENT);
    	intent.setAction(SERVICEMANAGE_INTENT);
    	intent.putExtra("state", state);
    	sendBroadcast(intent);
	}
	
	public void sendTrafficBroadcast(long[] traffic) {
    	Intent intent = new Intent(TRAFFICCOUNT_INTENT);
    	intent.setAction(TRAFFICCOUNT_INTENT);
    	intent.putExtra("traffic", traffic);
    	sendBroadcast(intent);
	}

	public void sendQuotaBroadcast(long[] quota) {
    	Intent intent = new Intent(QUOTACOUNT_INTENT);
    	intent.setAction(QUOTACOUNT_INTENT);
    	intent.putExtra("quota", quota);
    	sendBroadcast(intent);
	}

	public void sendCountdownBroadcast(long[] countdown) {
    	Intent intent = new Intent(COUNTDOWN_INTENT);
    	intent.setAction(COUNTDOWN_INTENT);
    	intent.putExtra("countdown", countdown);
    	sendBroadcast(intent);
	}

	public void sendTimerBroadcast(long[] countdowntimer) {
    	Intent intent = new Intent(COUNTDOWNTIMER_INTENT);
    	intent.setAction(COUNTDOWNTIMER_INTENT);
    	intent.putExtra("countdowntimer", countdowntimer);
    	sendBroadcast(intent);
	}
	public void sendKeepAliveBroadcast(long[] keepalive) {
    	Intent intent = new Intent(KEEPALIVE_INTENT);
    	intent.setAction(KEEPALIVE_INTENT);
    	intent.putExtra("keepalive", keepalive);
    	sendBroadcast(intent);
	}

	void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke method", e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w(TAG, "Unable to invoke method", e);
            e.printStackTrace();
        }
    }
	
	
	// ===========================================================
	// Private/Protected Methods
	// =========================================================== 
    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    protected void startForegroundCompat(int id, Notification notification) {
    	// If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        application.notificationManager.notify(id, notification);
    	
    	
    	/*
    	// If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke startForeground", e);
            }
            return;
        }

        // Fall back on the old API.
        setForeground(true);
        
        application.notificationManager.notify(id, notification);*/
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    protected void stopForegroundCompat(int id) {
    	// If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        application.notificationManager.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        
    	/*
    	// If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w(TAG, "Unable to invoke stopForeground", e);
            }
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        application.notificationManager.cancel(id);
        
        setForeground(false);*/
    }
	
	private void disableWifiAndBt(boolean enableAction) {
        boolean dontdisablebt = application.settings.getBoolean("dontdisablebtpref", false);
		boolean disableAction = false;
		if (this.wifiManager.isWifiEnabled()) {
			if (this.wifiLock != null && this.wifiLock.isHeld()) {
				this.wifiLock.release();
			}
    		this.wifiManager.setWifiEnabled(false);
    		disableAction = true;
    		Log.d(TAG, "Wifi disabled!");
    	}
		if (this.btAdapter.isEnabled() && dontdisablebt == false) {
			this.btAdapter.disable();
			disableAction = true;
			Log.d(TAG, "BT disabled");
		}
		if (disableAction) {
	    	// Waiting for interface-shutdown
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// nothing
			}
		}
    }
    
    private void enableWifiAndBt(boolean enableAction) {
    	boolean dontdisablebt = application.settings.getBoolean("dontdisablebtpref", false);
    	if (this.origWifiState == true && this.wifiManager.isWifiEnabled() == false) {
        	// Waiting for interface-restart
    		this.wifiManager.setWifiEnabled(true);
    		enableAction = true;
    		Log.d(TAG, "Wifi started!");
    	}
    	if (dontdisablebt == false && this.origBtState == true && this.btAdapter.isEnabled() == false) {
    		this.btAdapter.enable();
    		enableAction = true;
    		Log.d(TAG, "BT enabled");
    	}
    	if (enableAction) {
	    	// Waiting for interface-shutdown
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// nothing
			}
		}
    }
	
    private void enableAndDisconnectWifi() {
    	wifiManager.setWifiEnabled(true);
    	wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "Tether");
    	try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// nothing
		}
    	wifiManager.disconnect();
    	/*
    	try {
    		for (int i=0;i<10;i++) {
		    	wifiManager.disconnect();
		    	List<WifiConfiguration> allWifiConfs = wifiManager.getConfiguredNetworks();
		    	for (WifiConfiguration wifiConfiguration : allWifiConfs) {
		    		Log.d(TAG, "Disabling network - id: "+wifiConfiguration.networkId+" - ssid: "+wifiConfiguration.SSID);
		    		wifiManager.disableNetwork(wifiConfiguration.networkId);
		    	}
				Thread.sleep(500);
			}

		} catch (InterruptedException e) {
			// nothing
		}*/
   }
    
    /*private void enableAndDisconnectWifi() {
    	wifiManager.setWifiEnabled(true);
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "TETHERING_LOCK");
    	wifiLock.acquire();
    	
    	try {
    		for (int i=0;i<10;i++) {
		    	wifiManager.disconnect();
		    	List<WifiConfiguration> allWifiConfs = wifiManager.getConfiguredNetworks();
		    	for (WifiConfiguration wifiConfiguration : allWifiConfs) {
		    		Log.d(TAG, "Disabling network - id: "+wifiConfiguration.networkId+" - ssid: "+wifiConfiguration.SSID);
		    		wifiManager.disableNetwork(wifiConfiguration.networkId);
		    	}
				Thread.sleep(500);
			}

		} catch (InterruptedException e) {
			// nothing
		}
   }*/
    
	/**
	 * Start/stop the client-connect Thread
	 */
   	private void clientConnectEnable(boolean enable) {
   		if (enable == true) {
			if (this.clientConnectThread == null || this.clientConnectThread.isAlive() == false) {
				this.clientConnectThread = new Thread(new ClientConnect());
				this.clientConnectThread.start();
			}
   		} else {
	    	if (this.clientConnectThread != null)
	    		this.clientConnectThread.interrupt();
   		}
   	}  
	       
   	private void keepAliveCheckerEnable(boolean enable) {
   		if (enable == true) {
			if (this.keepAliveCheckerThread == null || this.keepAliveCheckerThread.isAlive() == false) {
				this.keepAliveCheckerThread = new Thread(new KeepAliveChecker());
				this.keepAliveCheckerThread.start();
			}
   		} else {
	    	if (this.keepAliveCheckerThread != null)
	    		this.keepAliveCheckerThread.interrupt();
   		}
   	}

   	private void trafficCounterEnable(boolean enable) {
   		if (enable == true) {
			if (this.trafficCounterThread == null || this.trafficCounterThread.isAlive() == false) {
				this.trafficCounterThread = new Thread(new TrafficCounter());
				this.trafficCounterThread.start();
			}
   		} else {
	    	if (this.trafficCounterThread != null)
	    		this.trafficCounterThread.interrupt();
   		}
   	}
   	
   	private void shutdownIdleCheckerEnable(boolean enable) {
   		if (enable == true) {
			if (this.shutdownIdleCheckerThread == null || this.shutdownIdleCheckerThread.isAlive() == false) {
				this.shutdownIdleCheckerThread = new Thread(new ShutdownIdleChecker());
				this.shutdownIdleCheckerThread.start();
			}
   		} else {
	    	if (this.shutdownIdleCheckerThread != null)
	    		this.shutdownIdleCheckerThread.interrupt();
   		}
   	}

   	private void shutdownTimerCheckerEnable(boolean enable) {
   		if (enable == true) {
			if (this.shutdownTimerCheckerThread == null || this.shutdownTimerCheckerThread.isAlive() == false) {
				this.shutdownTimerCheckerThread = new Thread(new ShutdownTimerChecker());
				this.shutdownTimerCheckerThread.start();
			}
   		} else {
	    	if (this.shutdownTimerCheckerThread != null)
	    		this.shutdownTimerCheckerThread.interrupt();
   		}
   	}

   	private void shutdownQuotaCheckerEnable(boolean enable) {
   		if (enable == true) {
			if (this.shutdownQuotaCheckerThread == null || this.shutdownQuotaCheckerThread.isAlive() == false) {
				this.shutdownQuotaCheckerThread = new Thread(new ShutdownQuotaChecker());
				this.shutdownQuotaCheckerThread.start();
			}
   		} else {
	    	if (this.shutdownQuotaCheckerThread != null)
	    		this.shutdownQuotaCheckerThread.interrupt();
   		}
   	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
   	/**
   	 * Checks the dnsmasq-lease file for new entries.
   	 */
	class ClientConnect implements Runnable {

	    private ArrayList<String> knownWhitelists = new ArrayList<String>();
	    private ArrayList<String> knownLeases = new ArrayList<String>();
	    private Hashtable<String, ClientData> currentLeases = new Hashtable<String, ClientData>();
	    private long timestampLeasefile = -1;
	    private long timestampWhitelistfile = -1;

	    // @Override
	    public void run() {
	        while (!Thread.currentThread().isInterrupted()) {
	        	//Log.d(MSG_TAG, "Checking for new clients ... ");
	        	// Notification-Type
	        	int notificationType = TetherService.this.application.getNotificationType();
	        	// Access-Control activated
	        	boolean accessControlActive = TetherService.this.application.whitelist.exists();
		        // Checking if Access-Control is activated
		        if (accessControlActive) {
	                // Checking whitelistfile
	                long currentTimestampWhitelistFile = TetherService.this.application.coretask.getModifiedDate(TetherService.this.application.coretask.DATA_FILE_PATH + "/conf/whitelist_mac.conf");
	                if (this.timestampWhitelistfile != currentTimestampWhitelistFile) {
	                    knownWhitelists = TetherService.this.application.whitelist.get();
	                    this.timestampWhitelistfile = currentTimestampWhitelistFile;
	                }
		        }

	            // Checking leasefile
	            long currentTimestampLeaseFile = TetherService.this.application.coretask.getModifiedDate(TetherService.this.application.coretask.DATA_FILE_PATH + "/var/dnsmasq.leases");
	            if (this.timestampLeasefile != currentTimestampLeaseFile) {
	                try {
	                	// Getting current dns-leases
	                    this.currentLeases = TetherService.this.application.coretask.getLeases();
	                    
	                    // Cleaning-up knownLeases after a disconnect (dhcp-release)
	                    for (String lease : this.knownLeases) {
	                        if (this.currentLeases.containsKey(lease) == false) {
	                        	Log.d(TAG, "Removing '"+lease+"' from known-leases!");
	                            this.knownLeases.remove(lease);
	                        	
	                            notifyActivity();
	                            TetherService.this.application.removeClientMac(lease);
	                        }
	                    }
	                    
	                    Enumeration<String> leases = this.currentLeases.keys();
	                    while (leases.hasMoreElements()) {
	                        String mac = leases.nextElement();
	                        Log.d(TAG, "Mac-Address: '"+mac+"' - Known Whitelist: "+knownWhitelists.contains(mac)+" - Known Lease: "+knownLeases.contains(mac));
	                        if (knownLeases.contains(mac) == false) {
	                            if (knownWhitelists.contains(mac) == false) {
	                            	// AddClientData to TetherApplication-Class for AccessControlActivity
	                            	TetherService.this.application.addClientData(this.currentLeases.get(mac));
	                            	
	                            	if (accessControlActive) {
	                            		if (notificationType == 1 || notificationType == 2) {
	                            			this.sendClientMessage(this.currentLeases.get(mac),
	                            					TetherApplication.CLIENT_CONNECT_NOTAUTHORIZED);
	                            		}
	                            	}
	                            	else {
	                            		if (notificationType == 2) {
	                            			this.sendClientMessage(this.currentLeases.get(mac),
	                            					TetherApplication.CLIENT_CONNECT_ACDISABLED);
	                            		}
	                            	}
	                                this.knownLeases.add(mac);
	                            } else if (knownWhitelists.contains(mac) == true) {
	                            	// AddClientData to TetherApplication-Class for AccessControlActivity
	                            	ClientData clientData = this.currentLeases.get(mac);
	                            	clientData.setAccessAllowed(true);
	                            	TetherService.this.application.addClientData(clientData);
	                            	
	                                if (notificationType == 2) {
	                                    this.sendClientMessage(this.currentLeases.get(mac),
	                                    		TetherApplication.CLIENT_CONNECT_AUTHORIZED);
	                                    this.knownLeases.add(mac);
	                                }
	                            }
	                            notifyActivity();
	                        }
	                    }
	                    this.timestampLeasefile = currentTimestampLeaseFile;
	                } catch (Exception e) {
	                    Log.d(TAG, "Unexpected error detected - Here is what I know: " + e.getMessage());
	                    e.printStackTrace();
	                }
	            }
	            try {
	                Thread.sleep(3000);
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	            }
	        }
	    }

	    private void notifyActivity(){
	    	if (AccessControlActivity.currentInstance != null){
	    		AccessControlActivity.currentInstance.clientConnectHandler.sendMessage(new Message());
	    	}
	    }
	    
	    private void sendClientMessage(ClientData clientData, int connectType) {
	        Message m = new Message();
	        m.obj = clientData;
	        m.what = connectType;
	        TetherService.this.application.clientConnectHandler.sendMessage(m);
	    }
	}
   	
   	class TrafficCounter implements Runnable {
   		private static final int INTERVAL = 2;  // Sample rate in seconds.
   		long previousDownload;
   		long previousUpload;
   		long lastTimeChecked;
   		public void run() {
   			this.previousDownload = this.previousUpload = 0;
   			this.lastTimeChecked = new Date().getTime();

   			String tetherNetworkDevice = TetherService.this.application.getTetherNetworkDevice();
   			long [] startCount = TetherService.this.application.coretask.getDataTraffic(tetherNetworkDevice);
   			
   			while (!Thread.currentThread().isInterrupted()) {
		        // Check data count
		        long [] trafficCount = TetherService.this.application.coretask.getDataTraffic(tetherNetworkDevice);
		        long currentTime = new Date().getTime();
		        float elapsedTime = (float) ((currentTime - this.lastTimeChecked) / 1000);
		        this.lastTimeChecked = currentTime;
		        
		        /**
		         * [0] - totalUpload
		         * [1] - totalDownload
		         * [2] - uploadRate
		         * [3] - downloadRate
		         */
		        long[] trafficData = new long[4];
		        trafficData[0] = trafficCount[0]-startCount[0];
		        trafficData[1] = trafficCount[1]-startCount[1];
		        trafficData[2] = (long) ((trafficData[0] - this.previousUpload)*8/elapsedTime);
		        trafficData[3] = (long) ((trafficData[1] - this.previousDownload)*8/elapsedTime);
		        
		        // Send traffic-broadcast
		        sendTrafficBroadcast(trafficData);
		        
		        if (trafficData[2] > 0 && trafficData[3] > 0) {
			        TetherService.this.timestampCounterUpdate = System.currentTimeMillis();
		        }
		        
		        this.previousUpload = trafficData[0];
				this.previousDownload = trafficData[1];
                try {
                    Thread.sleep(INTERVAL * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
   			}
   		}
   	}

  	class KeepAliveChecker implements Runnable {
  		private static final int INTERVAL = (5*60);				// Interval in seconds.
  		private static final int INTERVALFAIL = (1*60);			// Interval in seconds if failed.
  		private static final int INTERVALSHUTDOWNFAIL = (10*60);	// Interval in seconds to retry if failed.
		private Socket MySocket = null;
        private DataOutputStream MyOutStream = null;
        private DataInputStream MyInStream = null;
        private String MyTempString = "";
        private String[] MyHostName = new String[100]; // Up to 100 hostnames Max         
        private Integer TotalHostName = 0;        
        private Integer CurrentHostName = 0;

        public void run() {
			long[] keepalive = new long[3];
	        long nextCycle = 0;
   			boolean Updatestring = false;

   			String ShutDownPref =  (TetherService.this.application.settings.getString("keepalivecheckoptionpref", "karetry"));
			String TempHostsString = (TetherService.this.application.settings.getString("keepalivecheckwebsitespref", getString(R.string.setup_layout_keepalive_websites_default)));
	        StringTokenizer st = new StringTokenizer(TempHostsString, ";");

			String failShutdowntimerstring = (TetherService.this.application.settings.getString("keepalivecheckprefcheckintervalshutdownpref", "10"));
			failShutdowntimerstring.replaceAll("[^0-9]","");
   			if (failShutdowntimerstring.length() > 8) {
   				failShutdowntimerstring = failShutdowntimerstring.substring(failShutdowntimerstring.length()-8,failShutdowntimerstring.length());
   				Updatestring = true;
   			}
   			long failShutdowntimer = (Long.parseLong(failShutdowntimerstring) * 60);
   			if (failShutdowntimerstring == "" || failShutdowntimer == 0) {
	        	failShutdowntimer = INTERVALSHUTDOWNFAIL;
				failShutdowntimerstring = "10";
				Updatestring = true;
			}
			if (Updatestring == true) {
				TetherService.this.application.preferenceEditor.putString("keepalivecheckprefcheckintervalshutdownpref", failShutdowntimerstring);
   				TetherService.this.application.preferenceEditor.commit();
   			}

			String intervaltimerstring = (TetherService.this.application.settings.getString("keepalivecheckprefcheckintervalpref", "10"));
			intervaltimerstring.replaceAll("[^0-9]","");
   			if (intervaltimerstring.length() > 8) {
   				intervaltimerstring = intervaltimerstring.substring(intervaltimerstring.length()-8,intervaltimerstring.length());
   				Updatestring = true;
   			}
   			long intervaltimer = (Long.parseLong(intervaltimerstring) * 60);
   			if (intervaltimerstring == "" || intervaltimer == 0) {
   				intervaltimer = INTERVAL;
   				intervaltimerstring = "5";
				Updatestring = true;
			}
			if (Updatestring == true) {
				TetherService.this.application.preferenceEditor.putString("keepalivecheckprefcheckintervalpref", intervaltimerstring);
   				TetherService.this.application.preferenceEditor.commit();
   			}

			
	        while (TotalHostName == 0) {
	        	while (st.hasMoreTokens()) {
					MyTempString = st.nextToken();
					if (TotalHostName < 100) // 0-99 = 100
						if (MyTempString != null)
							if (MyTempString != "") {
								MyHostName[TotalHostName] = MyTempString;         
								TotalHostName++;        
							}
				}
	        	if (TotalHostName == 0) {
	        		st = new StringTokenizer(getString(R.string.setup_layout_keepalive_websites_default), ";");
					TetherService.this.application.preferenceEditor.putString("setup_layout_keepalive_websites_default", getString(R.string.setup_layout_keepalive_websites_default));
	   				TetherService.this.application.preferenceEditor.commit();
	        	}
	        }
	        
        	Log.d(TAG, "KeepAlive: Starting thread");
	    	while (!Thread.currentThread().isInterrupted()) {

	    		if (System.currentTimeMillis() >= nextCycle) {
	    			String HTTPREQUEST = "HEAD / HTTP/1.0\nHost: "+ MyHostName[CurrentHostName] + "\nUser-Agent: Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)\n\n";
	    			if (keepalive[0] == 0) 
	    				keepalive[1] = System.currentTimeMillis();

	    			keepalive[0] = 0;

	    			MySocket = null;  
	    			MyOutStream = null;
   		        	MyInStream = null;
   		        	String MyReceivedData = null;
   		        	Log.d(TAG, "KeepAlive: Connecting to host \"" + MyHostName[CurrentHostName] + "\"");
   		        	try {
   		        		MySocket = new Socket(MyHostName[CurrentHostName], 80);
   		        		MyOutStream = new DataOutputStream(MySocket.getOutputStream());
   		        		MyInStream = new DataInputStream(MySocket.getInputStream());
   		        	} catch (UnknownHostException e) {
   		        		Log.d(TAG, "KeepAlive: Unable to connect to unknown host \"" + MyHostName[CurrentHostName] + "\"");
   		        		keepalive[0] = 1;
   		        	} catch (IOException e) {
   		        		Log.d(TAG, "KeepAlive: An I/O exception occured with connection to host \"" + MyHostName[CurrentHostName] + "\"");
   		        		keepalive[0] = 1;
   		        	}	        
   		        	if (MySocket != null)
   		        		try {
   		        			Log.d(TAG, "KeepAlive: Requesting header from \"" + MyHostName[CurrentHostName] + "\"");
   		        			MyOutStream.writeBytes(HTTPREQUEST);    
   		        			while ((MyReceivedData = MyInStream.readLine()) != null) {
   		        				// Got response, lets boogie!
   		        				if (MyReceivedData.length() != 0) {
   		        					Log.d(TAG, "KeepAlive: Received response from \"" + MyHostName[CurrentHostName] + "\" is \"" + MyReceivedData + "\"");
   		        					break;
   		        				}
   		        				// Times up, I guess they don't want to talk to us so lets get outta here!
   		        				if (((System.currentTimeMillis() - (keepalive[0])/1000)) > 2) {
   		        					Log.d(TAG, "KeepAlive: Host \""+ MyHostName[CurrentHostName] +"\" timed out...");
   		        					keepalive[0] = 2;
   		        					break;
   		        				}
   		        			}
   		        			MyOutStream.close();
   		        			MyInStream.close();
   		        			MySocket.close();   
   		        		} catch (UnknownHostException e) {
   		        			Log.d(TAG, "KeepAlive: Unable to connect to unknown host \"" + MyHostName + "\"");
   		        			keepalive[0] = 1;
   		        		} catch (IOException e) {
   		        			Log.d(TAG, "KeepAlive: An I/O exception occured with connection to host \"" + MyHostName + "\"");
   		        			keepalive[0] = 1;
   		        		}
   		        	CurrentHostName++;
   		        	if (CurrentHostName >= TotalHostName)
   		        		CurrentHostName = 0;
   		        	if (keepalive[0] == 0) {
   		        		nextCycle = System.currentTimeMillis() + (intervaltimer * 1000);
   		        	} else {
   		        		nextCycle = System.currentTimeMillis() + (INTERVALFAIL * 1000);
   		        	}
	    		}
		        
		        if (ShutDownPref.equals("kashutdown")) {
	   	            if (keepalive[0] > 0)
	   	            	keepalive[2] = (failShutdowntimer*1000) - (System.currentTimeMillis() - keepalive[1]);
		        	if ((System.currentTimeMillis() - keepalive[1]) >= (failShutdowntimer * 1000)) {
		        		Log.d(TAG, "Automatic Shutdown: Keep-alive unable to establish connection for " + (failShutdowntimer/1000/60) + " minute(s).");
		        		TetherService.this.application.preferenceEditor.putBoolean("autoshutdownkeepalive", true);
   		 	   			TetherService.this.application.preferenceEditor.commit();
   		 	   			sendShutdownBroadcast();
		        	}
		        } else {
	   	            if (keepalive[0] > 0)
	   	            	keepalive[2] = (System.currentTimeMillis() - keepalive[1]);
		        }
   	            
   				// Send traffic-broadcast
		        sendKeepAliveBroadcast(keepalive);
		        
	        	try {
                	Thread.sleep(1 * 1000);
            	} catch (InterruptedException e) {
                	Thread.currentThread().interrupt();
            	}
	    	}
   		}
	    
	    private void sendShutdownBroadcast() {
   			// Send notification
   			TetherService.this.autoShutdown = true;
   			
			// Sending intent to TetherServiceReceiver that we want to start the service-now
	    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
	    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
	    	intent.putExtra("state", TetherService.SERVICE_STOP);
	    	sendBroadcast(intent);
   		}
   	}

  	class ShutdownIdleChecker implements Runnable {
  		private static final int INTERVAL = 1;  // Interval in seconds.

	    public void run() {
   			long idleMilliseconds = (TetherService.this.application.settings.getInt("shutdownidlepref", 2))*60*1000;
   			while (!Thread.currentThread().isInterrupted()) {

   				long[] countdown = new long[1];

   				countdown[0] = ((TetherService.this.timestampCounterUpdate+idleMilliseconds)-System.currentTimeMillis());

		        // Send traffic-broadcast
		        sendCountdownBroadcast(countdown);

   				if ((TetherService.this.timestampCounterUpdate+idleMilliseconds) < System.currentTimeMillis()) {
		        	Log.d(TAG, "Automatic Shutdown: Idle time reached..." + TetherService.this.application.settings.getInt("shutdownidlepref", 2) + " minute(s).");
   					TetherService.this.application.preferenceEditor.putBoolean("autoshutdownidle", true);
   		 	   		TetherService.this.application.preferenceEditor.commit();
   					sendShutdownBroadcast();
   				}
                try {
                    Thread.sleep(INTERVAL * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
   			}
   		}
   		
   		private void sendShutdownBroadcast() {
   			// Send notification
   			TetherService.this.autoShutdown = true;
   			
			// Sending intent to TetherServiceReceiver that we want to start the service-now
	    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
	    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
	    	intent.putExtra("state", TetherService.SERVICE_STOP);
	    	sendBroadcast(intent);
   		}
   	}


  	class ShutdownTimerChecker implements Runnable {
  		private static final int INTERVAL = 1;  // Interval in seconds.

	    public void run() {
   			long timerMilliseconds = (TetherService.this.application.settings.getInt("shutdowntimerlimitpref", 2))*60*1000;
   			long endTimer = System.currentTimeMillis() + timerMilliseconds; 
   			
   			while (!Thread.currentThread().isInterrupted()) {

   				long[] countdowntimer = new long[1];

   				countdowntimer[0] = (endTimer - System.currentTimeMillis());

		        // Send traffic-broadcast
		        sendTimerBroadcast(countdowntimer);

   				if (System.currentTimeMillis() >= endTimer) {
		        	Log.d(TAG, "Automatic Shutdown: Timer expired at..." + TetherService.this.application.settings.getInt("shutdowntimerlimitpref", 2) + " minute(s).");
   					TetherService.this.application.preferenceEditor.putBoolean("autoshutdowntimer", true);
   		 	   		TetherService.this.application.preferenceEditor.commit();
   					sendShutdownBroadcast();
   				}
                try {
                    Thread.sleep(INTERVAL * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
   			}
   		}
   		
   		private void sendShutdownBroadcast() {
   			// Send notification
   			TetherService.this.autoShutdown = true;
   			
			// Sending intent to TetherServiceReceiver that we want to start the service-now
	    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
	    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
	    	intent.putExtra("state", TetherService.SERVICE_STOP);
	    	sendBroadcast(intent);
   		}
   	}


  	class ShutdownQuotaChecker implements Runnable {
  		private static final int INTERVAL = 1;  // Interval in seconds.
   		public void run() {
   			/*
   			 * From setupview.xml - for Slider instead of manual
   					<com.googlecode.android.wifi.tether.ui.SeekBarPreference android:key="quotashutdownidlepref"
   			        android:title="@string/setup_layout_quotashutdown_limit"
   			        android:summary="@string/setup_layout_quotashutdown_limit_summary"
   			        android:dialogMessage="@string/setup_layout_quotashutdown_limitmax"
   			        android:defaultValue="1"
   			        android:text="@string/setup_layout_quotashutdown_limit_unit_mb"
   			        android:max="5000"
   			        android:dependency="quotashutdownpref" />
			* From strings.xml
					<string name="setup_layout_quotashutdown_limit_unit_mb">MB</string> 
					<string name="setup_layout_quotashutdown_limitmax">Shutdown after ... MB of usage.</string>
					<string name="setup_layout_quotashutdown_limit">Quota Limit Slider</string>
					<string name="setup_layout_quotashutdown_limit_summary">Configure the quota-limit quickly using a slider.</string>

			* From TetherService.java for slider use.
   					long QuotaMaxMB = (TetherService.this.application.settings.getInt("quotashutdownidlepref", 100)) * (1024 * 1024);
   			*/

   			// Long max = 9,223,372,036,854,775,807
   			String QuotaMaxMBstring = TetherService.this.application.settings.getString("quotamanualshutdownidlepref", "100");
   			QuotaMaxMBstring.replaceAll("[^0-9]","");

   			boolean Updatestring = false;
   			if (QuotaMaxMBstring.length() > 8) {
   				QuotaMaxMBstring = QuotaMaxMBstring.substring(QuotaMaxMBstring.length()-8,QuotaMaxMBstring.length()); // Could work with 9, 8 = approx 97000GB
   				Updatestring = true;
   			}
			if (QuotaMaxMBstring == "") {
				QuotaMaxMBstring = "100";
				Updatestring = true;
			}

   			long QuotaMaxMB = (Long.parseLong(QuotaMaxMBstring) * (1024 * 1024));
   			if (QuotaMaxMB < 1) {
   				QuotaMaxMB = 1;
				QuotaMaxMBstring = "1";
				Updatestring = true;
   			}

   			if (Updatestring == true) {
				TetherService.this.application.preferenceEditor.putString("quotamanualshutdownidlepref", QuotaMaxMBstring);
   				TetherService.this.application.preferenceEditor.commit();
   			}
   			
   			String tetherNetworkDevice = TetherService.this.application.getTetherNetworkDevice();
	   		long [] startCount = TetherService.this.application.coretask.getDataTraffic(tetherNetworkDevice);

	   		while (!Thread.currentThread().isInterrupted()) {
		        long [] trafficCount = TetherService.this.application.coretask.getDataTraffic(tetherNetworkDevice);

		        long[] quotaData = new long[2];
		        quotaData[0] = (trafficCount[0]-startCount[0])+(trafficCount[1]-startCount[1]);
		        quotaData[1] = QuotaMaxMB;

		        // Send traffic-broadcast
		        sendQuotaBroadcast(quotaData);

		        if ((trafficCount[0]-startCount[0])+(trafficCount[1]-startCount[1]) >= QuotaMaxMB) {
		        	Log.d(TAG, "Automatic Shutdown: Qouta limit reached..." + ((QuotaMaxMB / 1024) / 1024) + " MB(s).");
   					TetherService.this.application.preferenceEditor.putBoolean("autoshutdownquota", true);
   		 	   		TetherService.this.application.preferenceEditor.commit();
   					sendShutdownBroadcast();
   				}
                try {
                    Thread.sleep(INTERVAL * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
   			}
   		}
   		
   		private void sendShutdownBroadcast() {
   			// Send notification
   			TetherService.this.autoShutdown = true;
   			
			// Sending intent to TetherServiceReceiver that we want to start the service-now
	    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
	    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
	    	intent.putExtra("state", TetherService.SERVICE_STOP);
	    	sendBroadcast(intent);
   		}
   	}
}



