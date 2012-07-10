/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Sofia Lemons.
 */

package com.googlecode.android.wifi.tether;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private TetherApplication application = null;
	private ProgressDialog progressDialog;

	private Button startBtn = null;
	private OnClickListener startBtnListener = null;
	private Button stopBtn = null;
	private OnClickListener stopBtnListener = null;
	private TextView progressTitle = null;
	private TextView progressText = null;
	private ProgressBar progressBar = null;
	private RelativeLayout downloadUpdateLayout = null;
	private RelativeLayout batteryTemperatureLayout = null;
	
	private RelativeLayout trafficRow = null;
	private TextView downloadText = null;
	private TextView uploadText = null;
	private TextView downloadRateText = null;
	private TextView uploadRateText = null;
	private TextView batteryTemperature = null;
	private RelativeLayout quotaRow = null;
	private TextView quotaCurrentText = null;
	private TextView quotaMaxText = null;
	private TextView countDownText = null;
	private TextView timerText = null;
	private TextView keepAliveText = null;
	
	private TableRow startTblRow = null;
	private TableRow stopTblRow = null;
	
	private ScaleAnimation animation = null;
	
	private static int ID_DIALOG_STARTING = 0;
	private static int ID_DIALOG_STOPPING = 1;
	
	public static final int MESSAGE_CANT_START_TETHER = 2;
	public static final int MESSAGE_DOWNLOAD_STARTING = 3;
	public static final int MESSAGE_DOWNLOAD_PROGRESS = 4;
	public static final int MESSAGE_DOWNLOAD_COMPLETE = 5;
	public static final int MESSAGE_TRAFFIC_START     = 8;
	public static final int MESSAGE_TRAFFIC_COUNT     = 9;
	public static final int MESSAGE_TRAFFIC_RATE      = 10;
	public static final int MESSAGE_TRAFFIC_END       = 11;
	
	private static final int MENU_SETUP = 0;
	private static final int MENU_LOG = 1;
	private static final int MENU_ABOUT = 2;
	private static final int MENU_ACCESS = 3;
	
	public static final String TAG = "TETHER -> MainActivity";
	public static MainActivity currentInstance = null;
	
    private IntentFilter intentFilter;
	
    private static void setCurrent(MainActivity current){
    	MainActivity.currentInstance = current;
    }
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "Calling onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize myself
        MainActivity.setCurrent(this);
        
        // Init Application
        application = (TetherApplication)this.getApplication();
        
        // Init Table-Rows
        startTblRow = (TableRow)findViewById(R.id.startRow);
        stopTblRow = (TableRow)findViewById(R.id.stopRow);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressText = (TextView)findViewById(R.id.progressText);
        progressTitle = (TextView)findViewById(R.id.progressTitle);
        downloadUpdateLayout = (RelativeLayout)findViewById(R.id.layoutDownloadUpdate);
        batteryTemperatureLayout = (RelativeLayout)findViewById(R.id.layoutBatteryTemp);
        
        trafficRow = (RelativeLayout)findViewById(R.id.trafficRow);
        downloadText = (TextView)findViewById(R.id.trafficDown);
        uploadText = (TextView)findViewById(R.id.trafficUp);
        downloadRateText = (TextView)findViewById(R.id.trafficDownRate);
        uploadRateText = (TextView)findViewById(R.id.trafficUpRate);
        batteryTemperature = (TextView)findViewById(R.id.batteryTempText);
        quotaRow = (RelativeLayout)findViewById(R.id.quotaRow);
        quotaCurrentText = (TextView)findViewById(R.id.quotaCurrent);
        quotaMaxText = (TextView)findViewById(R.id.quotaMax);
        countDownText = (TextView)findViewById(R.id.countdownIdleText);
        keepAliveText = (TextView)findViewById(R.id.keepAliveText);
        timerText = (TextView)findViewById(R.id.countdownTimerText);

        // Define animation
        animation = new ScaleAnimation(
                0.9f, 1, 0.9f, 1, // From x, to x, from y, to y
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(600);
        animation.setFillAfter(true); 
        animation.setStartOffset(0);
        animation.setRepeatCount(1);
        animation.setRepeatMode(Animation.REVERSE);

        // Startup-Check
        if (application.startupCheckPerformed == false) {
	        application.startupCheckPerformed = true;
	        
	    	// Check if required kernel-features are enabled
	    	if (!application.coretask.isNetfilterSupported()) {
	    		if (application.settings.getBoolean("warning_nonetfilter_displayed", false) == false) {
	    			openNoNetfilterDialog();
	    			application.preferenceEditor.putBoolean("warning_nonetfilter_displayed", true);
    				application.preferenceEditor.commit();
	    		}
	    	}
	    	else {
	    		// Check if access-control-feature is supported by kernel
	    		if (!application.coretask.isAccessControlSupported()) {
	    			if (application.settings.getBoolean("warning_noaccesscontrol_displayed", false) == false) {
	    				openNoAccessControlDialog();
	    				application.preferenceEditor.putBoolean("warning_noaccesscontrol_displayed", true);
	    				application.preferenceEditor.commit();
	    			}
	    			application.accessControlSupported = false;
	    			application.whitelist.remove();
	    		}
	    	}
	    		
        	// Check root-permission, files
	    	if (!application.coretask.hasRootPermission())
	    		openNotRootDialog();
	    	
	        // Open donate-dialog
			openDonateDialog();
        
			// Check for updates
			application.checkForUpdate();
        }
        
        // Start Button
        startBtn = (Button) findViewById(R.id.startTetherBtn);
        startBtnListener = new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "StartBtn pressed ...");
				// Sending intent to TetherServiceReceiver that we want to start the service-now
		    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
		    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
		    	intent.putExtra("state", TetherService.SERVICE_START);
		    	sendBroadcast(intent);
				
			}
		};
		startBtn.setOnClickListener(this.startBtnListener);

		// Stop Button
		stopBtn = (Button) findViewById(R.id.stopTetherBtn);
		stopBtnListener = new OnClickListener() {
			public void onClick(View v) {
				// Sending intent to TetherServiceReceiver that we want to start the service-now
		    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
		    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
		    	intent.putExtra("state", TetherService.SERVICE_STOP);
		    	sendBroadcast(intent);
			}
		};
		stopBtn.setOnClickListener(this.stopBtnListener);
    }
    
    @Override
	public boolean onTrackballEvent(MotionEvent event){
		if (event.getAction() == MotionEvent.ACTION_DOWN){
			Log.d(TAG, "Trackball pressed ...");
			
            if (this.startTblRow.getVisibility() == View.VISIBLE){
				new AlertDialog.Builder(this)
				.setMessage(getString(R.string.main_activity_trackball_pressed_start))  
			    .setPositiveButton(getString(R.string.main_activity_confirm), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Log.d(TAG, "Trackball press confirmed ...");
						MainActivity.currentInstance.startBtnListener.onClick(MainActivity.currentInstance.startBtn);
					}
				}) 
			    .setNegativeButton(getString(R.string.main_activity_cancel), null)  
			    .show();
			}
            else if (this.stopTblRow.getVisibility() == View.VISIBLE || (this.stopTblRow.getVisibility() == View.VISIBLE && this.startTblRow.getVisibility() == View.VISIBLE)) {
				new AlertDialog.Builder(this)
				.setMessage(getString(R.string.main_activity_trackball_pressed_stop))  
			    .setPositiveButton(getString(R.string.main_activity_confirm), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Log.d(TAG, "Trackball press confirmed ...");
						MainActivity.currentInstance.stopBtnListener.onClick(MainActivity.currentInstance.startBtn);
					}
				})
			    .setNegativeButton(getString(R.string.main_activity_cancel), null)  
			    .show();
            }
		}
		return true;
	}
	
    @Override
	public void onStop() {
    	Log.d(TAG, "Calling onStop()");
    	super.onStop();
    }

	@Override
	public void onDestroy() {
    	Log.d(TAG, "Calling onDestroy()");
    	super.onDestroy();
	}

	@Override
	public void onPause() {
		Log.d(TAG, "Calling onPause()");
		super.onPause();
		try {
			unregisterReceiver(this.intentReceiver);
		} catch (Exception ex) {;}   
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "Calling onResume()");
		super.onResume();
		
		// Initialize Intent-Filter
		intentFilter = new IntentFilter();
		
		// Check, if the battery-temperature should be displayed
		if(this.application.settings.getString("batterytemppref", "celsius").equals("disabled") == false) {
			// Add an intent-Filter for Battery-Temperature-Updates
			intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
	        this.batteryTemperatureLayout.setVisibility(View.VISIBLE);
		}
		else {
			this.batteryTemperatureLayout.setVisibility(View.INVISIBLE);
		}

		// Add an intent-Filter for Tethering-State-Changes
        intentFilter.addAction(TetherService.STATECHANGED_INTENT);
        
        // Add an intent-Filter for Traffic-updates
        intentFilter.addAction(TetherService.TRAFFICCOUNT_INTENT);
        
        // Add an intent-Filter for Quota-updates
        intentFilter.addAction(TetherService.QUOTACOUNT_INTENT);

        // Add an intent-Filter for Countdown-updates
        intentFilter.addAction(TetherService.COUNTDOWN_INTENT);

        // Add an intent-Filter for Keepalive-updates
        intentFilter.addAction(TetherService.KEEPALIVE_INTENT);

        // Add an intent-Filter for Keepalive-updates
        intentFilter.addAction(TetherService.COUNTDOWNTIMER_INTENT);

        // Register Intent-Receiver
        registerReceiver(intentReceiver, intentFilter);
        
		// Toggles between start and stop screen
		this.toggleStartStop();
	}
	
    /**
     *Listens for intent broadcasts; Needed for the temperature-display
     */
     private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            	 int temp = (intent.getIntExtra("temperature", 0));
            	 int celsius = (int)((temp+5)/10);
            	 int fahrenheit = (int)(((temp/10)/0.555)+32+0.5);
            	 if (application.settings.getString("batterytemppref", "celsius").equals("celsius")) {
            		 batteryTemperature.setText("" + celsius + getString(R.string.main_activity_temperatureunit_celsius));
            	 }
            	 else {
            		 batteryTemperature.setText("" + fahrenheit + getString(R.string.main_activity_temperatureunit_fahrenheit));
            	 }
             }
             else if (action.equals(TetherService.STATECHANGED_INTENT)) {
            	 switch (intent.getIntExtra("state", TetherService.STATE_IDLE)) {
            	   	case TetherService.STATE_STARTING :
            	   		showDialog(MainActivity.ID_DIALOG_STARTING);
            	   		break;
            	   	case TetherService.STATE_RUNNING :
            	   		try {
            	   			dismissDialog(MainActivity.ID_DIALOG_STARTING);
            	   		} catch (Exception ex) {;}
            	   		toggleStartStop();
            	   		break;
            	   	case TetherService.STATE_STOPPING:
            	   		showDialog(MainActivity.ID_DIALOG_STOPPING);
            	   		break;            	   		
            	   	case TetherService.STATE_IDLE :
            	   		try {
            	   			dismissDialog(MainActivity.ID_DIALOG_STOPPING);
            	   		} catch (Exception ex) {;}
						toggleStartStop();
            	   		break;               	   		
            	   	case TetherService.STATE_FAILURE_LOG :
            	   		try {
            	   			dismissDialog(MainActivity.ID_DIALOG_STARTING);
            	   		} catch (Exception ex) {;}
                		application.displayToastMessage(getString(R.string.main_activity_start_errors));
                    	toggleStartStop();
            	   		break;
            	   	case TetherService.STATE_FAILURE_EXE :
            	   		try {
            	   			dismissDialog(MainActivity.ID_DIALOG_STARTING);
            	   		} catch (Exception ex) {;}
            	   		application.displayToastMessage(getString(R.string.main_activity_start_unable));
                    	toggleStartStop();
            	   		break;            	   		
            	   	default:
            	   		try {
            	   			dismissDialog(MainActivity.ID_DIALOG_STOPPING);
            	   		} catch (Exception ex) {;}
            	   		break;
            	 }
             }
             else if (action.equals(TetherService.KEEPALIVE_INTENT)) {
               	try {
	            	long[] keepalive = intent.getLongArrayExtra("keepalive");
	               	String keepalivestring = "";
	               	if (keepalive[0] == 0) {
	                   	keepalivestring  = "UP";
	                   	keepAliveText.setTextColor(getResources().getColor(R.color.Green));
	               	} else {
		            	/*
		            	 * http://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
		            	 * Workaround for Java versions below 1.5 or for systems that do not fully support the TimeUnit class (such as Android before API version 9)
		            	 */
	               		int seconds = (int) (keepalive[2] / 1000) % 60 ;
	               		int minutes = (int) ((keepalive[2] / (1000*60)) % 60);
	               		int hours   = (int) ((keepalive[2] / (1000*60*60)) % 24);
	               		
	               		keepalivestring = "DOWN - " + String.format("%02d:%02d:%02d", 
	               				hours,
	               				minutes,
	               				seconds);
	                   	keepAliveText.setTextColor(getResources().getColor(R.color.Red));
	               	}
	               	keepAliveText.setVisibility(View.VISIBLE);
	  	        	keepAliveText.setText(getString(R.string.main_activity_keepalive_headline)+": " + keepalivestring);
	  	        	keepAliveText.invalidate();
               	} catch (java.lang.IllegalAccessError ex) {
               		ex.printStackTrace();
               	}
             }
             else if (action.equals(TetherService.COUNTDOWN_INTENT)) {
              	try {
	            	long[] countdown = intent.getLongArrayExtra("countdown");
               		
	            	/*
	            	 * http://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
	            	 * Workaround for Java versions below 1.5 or for systems that do not fully support the TimeUnit class (such as Android before API version 9)
	            	 */
	            	int seconds = (int) (countdown[0] / 1000) % 60 ;
               		int minutes = (int) ((countdown[0] / (1000*60)) % 60);
               		int hours   = (int) ((countdown[0] / (1000*60*60)) % 24);
	            	
	            	String countdowncurrentstring = String.format("%02d:%02d:%02d", 
	            			hours,
	            			minutes,
	            			seconds);
	  	        	countDownText.setVisibility(View.VISIBLE);
	  	        	countDownText.setText(getString(R.string.main_activity_idle_headline)+": " + countdowncurrentstring);
	  	        	countDownText.invalidate();
              	} catch (java.lang.IllegalAccessError ex) {
              		ex.printStackTrace();
              	}
              }
             else if (action.equals(TetherService.COUNTDOWNTIMER_INTENT)) {
               	try {
 	            	long[] countdowntimer = intent.getLongArrayExtra("countdowntimer");
                		
 	            	/*
 	            	 * http://stackoverflow.com/questions/625433/how-to-convert-milliseconds-to-x-mins-x-seconds-in-java
 	            	 * Workaround for Java versions below 1.5 or for systems that do not fully support the TimeUnit class (such as Android before API version 9)
 	            	 */
 	            	int seconds = (int) (countdowntimer[0] / 1000) % 60 ;
                	int minutes = (int) ((countdowntimer[0] / (1000*60)) % 60);
                	int hours   = (int) ((countdowntimer[0] / (1000*60*60)) % 24);
 	            	
 	            	String timercurrentstring = String.format("%02d:%02d:%02d", 
 	            			hours,
 	            			minutes,
 	            			seconds);
 	  	        	timerText.setVisibility(View.VISIBLE);
 	  	        	timerText.setText(getString(R.string.main_activity_timer_headline)+": " + timercurrentstring);
 	  	        	timerText.invalidate();
               	} catch (java.lang.IllegalAccessError ex) {
               		ex.printStackTrace();
               	}
               }
             else if (action.equals(TetherService.QUOTACOUNT_INTENT)) {
             	long[] quotaData = intent.getLongArrayExtra("quota");
 	        	long quotaCurrentTraffic = quotaData[0];
	        	long quotaMaxTraffic = quotaData[1];

             	quotaRow.setVisibility(View.VISIBLE);
                quotaCurrentText.setText(MainActivity.this.formatCountMB(quotaCurrentTraffic));
                quotaMaxText.setText(MainActivity.this.formatCountMB(quotaMaxTraffic));
                quotaCurrentText.invalidate();
                quotaMaxText.invalidate();
             }
             else if (action.equals(TetherService.TRAFFICCOUNT_INTENT)) {
            	trafficRow.setVisibility(View.VISIBLE);
            	long[] trafficData = intent.getLongArrayExtra("traffic");

		        /**
		         * [0] - totalUpload
		         * [1] - totalDownload
		         * [2] - uploadRate
		         * [3] - downloadRate
		         */
 	        	long uploadTraffic = trafficData[0];
	        	long downloadTraffic = trafficData[1];
	        	long uploadRate = trafficData[2];
	        	long downloadRate = trafficData[3];

	        	// Set rates to 0 if values are negative
	        	if (uploadRate < 0)
	        		uploadRate = 0;
	        	if (downloadRate < 0)
	        		downloadRate = 0;
	        	
        		uploadText.setText(MainActivity.this.formatCount(uploadTraffic, false));
        		downloadText.setText(MainActivity.this.formatCount(downloadTraffic, false));
        		downloadText.invalidate();
        		uploadText.invalidate();

        		uploadRateText.setText(MainActivity.this.formatCount(uploadRate, true));
        		downloadRateText.setText(MainActivity.this.formatCount(downloadRate, true));
        		downloadRateText.invalidate();
        		uploadRateText.invalidate();
             }
         }
     };	

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	SubMenu setup = menu.addSubMenu(0, MENU_SETUP, 0, getString(R.string.main_activity_settings));
    	setup.setIcon(drawable.ic_menu_preferences);
    	if (this.application.accessControlSupported) { 
    		SubMenu accessctr = menu.addSubMenu(0, MENU_ACCESS, 0, getString(R.string.main_activity_accesscontrol));
    		accessctr.setIcon(drawable.ic_menu_edit);   
    	}
    	SubMenu log = menu.addSubMenu(0, MENU_LOG, 0, getString(R.string.main_activity_showlog));
    	log.setIcon(drawable.ic_menu_agenda);
    	SubMenu about = menu.addSubMenu(0, MENU_ABOUT, 0, getString(R.string.main_activity_about));
    	about.setIcon(drawable.ic_menu_info_details);    	
    	return supRetVal;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	boolean supRetVal = super.onOptionsItemSelected(menuItem);
    	Log.d(TAG, "Menuitem:getId  -  "+menuItem.getItemId()); 
    	switch (menuItem.getItemId()) {
	    	case MENU_SETUP :
		        startActivityForResult(new Intent(
		        		MainActivity.this, SetupActivity.class), 0);
		        break;
	    	case MENU_LOG :
		        startActivityForResult(new Intent(
		        		MainActivity.this, LogActivity.class), 0);
		        break;
	    	case MENU_ABOUT :
	    		this.openAboutDialog();
	    		break;
	    	case MENU_ACCESS :
		        startActivityForResult(new Intent(
		        		MainActivity.this, AccessControlActivity.class), 0);   		
    	}
    	return supRetVal;
    }    

    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == ID_DIALOG_STARTING) {
	    	progressDialog = new ProgressDialog(this);
	    	progressDialog.setTitle(getString(R.string.main_activity_start));
	    	progressDialog.setMessage(getString(R.string.main_activity_start_summary));
	    	progressDialog.setIndeterminate(false);
	    	progressDialog.setCancelable(true);
	        return progressDialog;
    	}
    	else if (id == ID_DIALOG_STOPPING) {
	    	progressDialog = new ProgressDialog(this);
	    	progressDialog.setTitle(getString(R.string.main_activity_stop));
	    	progressDialog.setMessage(getString(R.string.main_activity_stop_summary));
	    	progressDialog.setIndeterminate(false);
	    	progressDialog.setCancelable(true);
	        return progressDialog;  		
    	}
    	return null;
    }

    public Handler viewUpdateHandler = new Handler(){
        public void handleMessage(Message msg) {
        	switch(msg.what) {
        	case MESSAGE_CANT_START_TETHER :
        		Log.d(TAG, "Unable to start tethering!");
        		application.displayToastMessage(getString(R.string.main_activity_start_unable));
            	toggleStartStop();
            	break;
        	case MESSAGE_DOWNLOAD_STARTING :
        		Log.d(TAG, "Start progress bar");
        		MainActivity.this.progressBar.setIndeterminate(true);
        		MainActivity.this.progressTitle.setText((String)msg.obj);
        		MainActivity.this.progressText.setText("Starting...");
        		MainActivity.this.downloadUpdateLayout.setVisibility(View.VISIBLE);
        		break;
        	case MESSAGE_DOWNLOAD_PROGRESS :
        		MainActivity.this.progressBar.setIndeterminate(false);
        		MainActivity.this.progressText.setText(msg.arg1 + "k /" + msg.arg2 + "k");
        		MainActivity.this.progressBar.setProgress(msg.arg1*100/msg.arg2);
        		break;
        	case MESSAGE_DOWNLOAD_COMPLETE :
        		Log.d(TAG, "Finished download.");
        		MainActivity.this.progressText.setText("");
        		MainActivity.this.progressTitle.setText("");
        		MainActivity.this.downloadUpdateLayout.setVisibility(View.GONE);
        		break;
        	default:
        		MainActivity.this.toggleStartStop();
        	}
        	super.handleMessage(msg);
        }
   };

   private void toggleStartStop() {
    	if (TetherService.singleton != null && (
    						TetherService.singleton.getState() == TetherService.STATE_RUNNING || 
    						TetherService.singleton.getState() == TetherService.STATE_FAILURE_LOG ||
    						TetherService.singleton.getState() == TetherService.STATE_STOPPING)) {
    		startTblRow.setVisibility(View.GONE);
    		stopTblRow.setVisibility(View.VISIBLE);
    		// Animation
    		if (this.animation != null)
    			this.stopBtn.startAnimation(this.animation);

    	}
    	else if (TetherService.singleton == null || (
    						TetherService.singleton.getState() == TetherService.STATE_IDLE ||
    						TetherService.singleton.getState() == TetherService.STATE_STARTING)) {
    		trafficRow.setVisibility(View.INVISIBLE);
    		quotaRow.setVisibility(View.INVISIBLE);
    		countDownText.setVisibility(View.INVISIBLE);
    		keepAliveText.setVisibility(View.INVISIBLE);
    		timerText.setVisibility(View.INVISIBLE);
    		timerText.setText("");
    		keepAliveText.setText("");
    		startTblRow.setVisibility(View.VISIBLE);
    		stopTblRow.setVisibility(View.GONE);
    		//this.application.trafficCounterEnable(false);
    		// Animation
    		if (this.animation != null)
    			this.startBtn.startAnimation(this.animation);
    		// Notification
        	this.application.notificationManager.cancelAll();
    	}   	
    	else {
    		this.startTblRow.setVisibility(View.VISIBLE);
    		this.stopTblRow.setVisibility(View.VISIBLE);
    		MainActivity.this.application.displayToastMessage(getString(R.string.main_activity_start_unknownstate));
    	}
    }
   
	private String formatCount(long count, boolean rate) {
		// Converts the supplied argument into a string.
		// 'rate' indicates whether is a total bytes, or bits per sec.
		// Under 2Mb, returns "xxx.xKb"
		// Over 2Mb, returns "xxx.xxMb"
		if (count < 1e6 * 2)
			return ((float)((int)(count*10/1024))/10 + (rate ? "kbps" : "kB"));
		return ((float)((int)(count*100/1024/1024))/100 + (rate ? "mbps" : "MB"));
	}

	private String formatCountMB(long count) {
		// Converts the supplied argument into a string.
		// returns "xxx.xxMb"

		if (count < 1024)															// Bytes
			return (((long)(count)) + "B");
		else if ((count >= 1024) & (count < 1048576))								// Kilobytes (1024*1024)
			return ((float)((long)(count*100/1024))/100 + "KB");
		else if ((count >= 1048576) & (count < 1073741824))							// Megabytes (1024*1024*1024)
			return ((float)((long)(count*100/1024/1024))/100 + "MB");
		else// if ((count >= 1073741824) & (count < (long)(1024*1024*1024*1024)))	// Gigabytes (1024*1024*1024*1024)
			return ((float)((long)(count*100/1024/1024/1024))/100 + "GB");
		// FAILS TO WORK, Numbers too large
		//else if (count <= (1024*1024*1024*1024*1024))		// Terabytes (1024*1024*1024*1024*1024)
		//	return ((float)((long)(count*100/1024/1024/1024/1024))/100 + "TB");
		//else if (count > (1024*1024*1024*1024*1024))		// Petabytes (1024*1024*1024*1024*1024*1024)
		//	return ((float)((long)(count*100/1024/1024/1024/1024/1024))/100 + "PB");

	}

   	private void openNoNetfilterDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.nonetfilterview, null); 
		new AlertDialog.Builder(MainActivity.this)
        .setTitle(getString(R.string.main_activity_nonetfilter))
        .setView(view)
        .setNegativeButton(getString(R.string.main_activity_exit), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "Close pressed");
                        MainActivity.this.finish();
                }
        })
        .setNeutralButton(getString(R.string.main_activity_ignore), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d(TAG, "Override pressed");
                    MainActivity.this.application.displayToastMessage("Ignoring, note that this application will NOT work correctly.");
                }
        })
        .show();
   	}
   	
   	private void openNoAccessControlDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.noaccesscontrolview, null); 
		new AlertDialog.Builder(MainActivity.this)
        .setTitle(getString(R.string.main_activity_noaccesscontrol))
        .setView(view)
        .setNeutralButton(getString(R.string.main_activity_ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d(TAG, "OK pressed");
                    MainActivity.this.application.displayToastMessage(getString(R.string.main_activity_accesscontrol_disabled));
                }
        })
        .show();
   	}
   	
   	private void openNotRootDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null); 
		new AlertDialog.Builder(MainActivity.this)
        .setTitle(getString(R.string.main_activity_notroot))
        .setView(view)
        .setNegativeButton(getString(R.string.main_activity_exit), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "Exit pressed");
                        MainActivity.this.finish();
                }
        })
        .setNeutralButton(getString(R.string.main_activity_ignore), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Log.d(TAG, "Ignore pressed");
                    MainActivity.this.application.installFiles();
                    MainActivity.this.application.displayToastMessage("Ignoring, note that this application will NOT work correctly.");
                }
        })
        .show();
   	}
   
   	private void openAboutDialog() {
		LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.aboutview, null); 
        TextView versionName = (TextView)view.findViewById(R.id.versionName);
        versionName.setText(this.application.getVersionName());
        TextView authors = (TextView)view.findViewById(R.id.authors);
        authors.setText(Html.fromHtml(TetherApplication.AUTHORS));
        authors.setMovementMethod(LinkMovementMethod.getInstance());

        new AlertDialog.Builder(MainActivity.this)
        .setTitle(getString(R.string.main_activity_about))
        .setView(view)
        .setNeutralButton(getString(R.string.main_activity_donate), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "Donate pressed");
    					Uri uri = Uri.parse(getString(R.string.paypalUrl));
    					startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
        })
        .setNegativeButton(getString(R.string.main_activity_close), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(TAG, "Close pressed");
                }
        })
        .show();  		
   	}
   	
   	private void openDonateDialog() {
   		if (this.application.showDonationDialog()) {
   			// Disable donate-dialog for later startups
   			this.application.preferenceEditor.putBoolean("donatepref", false);
   			this.application.preferenceEditor.commit();
   			// Creating Layout
			LayoutInflater li = LayoutInflater.from(this);
	        View view = li.inflate(R.layout.donateview, null); 
	        new AlertDialog.Builder(MainActivity.this)
	        .setTitle(getString(R.string.main_activity_donation_headline))
	        .setView(view)
	        .setNeutralButton(getString(R.string.main_activity_close), new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(TAG, "Close pressed");
	                }
	        })
	        .setNegativeButton(getString(R.string.main_activity_donate), new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                        Log.d(TAG, "Donate pressed");
	    					Uri uri = Uri.parse(getString(R.string.paypalUrl));
	    					startActivity(new Intent(Intent.ACTION_VIEW, uri));
	                }
	        })
	        .show();
   		}
   	}

   	public void openUpdateDialog(final String downloadFileUrl, final String fileName, final String message,
   	    final String updateTitle) {
		LayoutInflater li = LayoutInflater.from(this);
		Builder dialog;
		View view;
		view = li.inflate(R.layout.updateview, null);
        TextView messageView = (TextView) view.findViewById(R.id.updateMessage);
        TextView updateNowText = (TextView) view.findViewById(R.id.updateNowText);
        if (fileName.length() == 0)  // No filename, hide 'download now?' string
          updateNowText.setVisibility(View.GONE);
        messageView.setText(message);
        dialog = new AlertDialog.Builder(MainActivity.this)
        .setTitle(updateTitle)
        .setView(view);
        
        if (fileName.length() > 0) {
          // Display Yes/No for if a filename is available.
          dialog.setNeutralButton(getString(R.string.main_activity_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "No pressed");
            }
          });
          dialog.setNegativeButton(getString(R.string.main_activity_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "Yes pressed");
                MainActivity.this.application.downloadUpdate(downloadFileUrl, fileName);
            }
          });          
        } else
          dialog.setNeutralButton(getString(R.string.main_activity_ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Log.d(TAG, "Ok pressed");
            }
          });

        dialog.show();
   	}

}

