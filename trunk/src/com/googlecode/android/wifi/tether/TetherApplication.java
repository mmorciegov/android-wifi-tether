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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import com.googlecode.android.wifi.tether.data.ClientData;
import com.googlecode.android.wifi.tether.system.Configuration;
import com.googlecode.android.wifi.tether.system.CoreTask;
import com.googlecode.android.wifi.tether.system.WebserviceTask;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class TetherApplication extends Application {

	public static final String MSG_TAG = "TETHER -> TetherApplication";

	// See private void openAboutDialog() in TetherApplication.java
	public static final String AUTHORS = "<HTML><a href=\"https://plus.google.com/u/0/107088765107518450541\">Harald M&uuml;ller</a>, Sofia Lemons, Ben Buxton, Andrew Robinson, <a href=\"http://sirgatez.blogspot.com\">Joshua Briefman</a>, <a href=\"http://androidsecuritytest.com\">Trevor Eckhart</a></HTML>";

	public final String DEFAULT_PASSPHRASE = "abcdefghijklm";
	public final String DEFAULT_LANNETWORK = "192.168.2.0/24";
	public final String DEFAULT_ENCSETUP   = "wpa_supplicant";
	
	// TetherService
	//private TetherService tetherService = null;

	// StartUp-Check perfomed
	public boolean startupCheckPerformed = false;

	// Client-Connect-Thread
	public static final int CLIENT_CONNECT_ACDISABLED = 0;
	public static final int CLIENT_CONNECT_AUTHORIZED = 1;
	public static final int CLIENT_CONNECT_NOTAUTHORIZED = 2;
	
	//public String tetherNetworkDevice = null;
	
	// PowerManagement
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	// Preferences
	public SharedPreferences settings = null;
	public SharedPreferences.Editor preferenceEditor = null;
	
    // Notification
	public NotificationManager notificationManager;
	private Notification notification;
	private int clientNotificationCount = 0;
	
	// Intents
	private PendingIntent mainIntent;
	private PendingIntent accessControlIntent;
	
	// Client
	ArrayList<ClientData> clientDataAddList = new ArrayList<ClientData>();
	ArrayList<String> clientMacRemoveList = new ArrayList<String>();
	
	// Access-control
	boolean accessControlSupported = true;
	
	//device string for setup
	String device = "Unknown";
	
	// Whitelist
	public CoreTask.Whitelist whitelist = null;
	// Supplicant
	public CoreTask.WpaSupplicant wpasupplicant = null;
	// TiWlan.conf
	public CoreTask.TiWlanConf tiwlan = null;
	// tether.conf
	public CoreTask.TetherConfig tethercfg = null;
	// hostapd
	public CoreTask.HostapdConfig hostapdcfg = null;

	
	// CoreTask
	public CoreTask coretask = null;
	
	// WebserviceTask
	public WebserviceTask webserviceTask = null;
	
	// Update Url
	private static final String APPLICATION_PROPERTIES_URL = "http://android-wifi-tether.googlecode.com/svn/download/update/wifi-tether/application.properties";
	private static final String APPLICATION_DOWNLOAD_URL = "http://android-wifi-tether.googlecode.com/files/";
	
	// Configuration
	Configuration configuration = null;
	
	@Override
	public void onCreate() {
		Log.d(MSG_TAG, "Calling onCreate()");
		
		//create CoreTask
		this.coretask = new CoreTask();
		this.coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		Log.d(MSG_TAG, "Current directory is "+this.getApplicationContext().getFilesDir().getParent());

		//create WebserviceTask
		this.webserviceTask = new WebserviceTask();
		
        // Check Homedir, or create it
        this.checkDirs(); 
        
        // Preferences
		this.settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // preferenceEditor
        this.preferenceEditor = settings.edit();
		
        // Whitelist
        this.whitelist = this.coretask.new Whitelist();
        
        // Supplicant config
        this.wpasupplicant = this.coretask.new WpaSupplicant();
        
        // tiwlan.conf
        this.tiwlan = this.coretask.new TiWlanConf();
        
        // tether.cfg
        this.tethercfg = this.coretask.new TetherConfig();
        this.tethercfg.read();
    	
        // Init Device flag
        this.device = android.os.Build.DEVICE; 
        
    	// hostapd
    	this.hostapdcfg = this.coretask.new HostapdConfig();

        // Powermanagement
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TETHER_WAKE_LOCK");

        // init notificationManager
        this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    	this.notification = new Notification(R.drawable.start_notification, "WiFi Tether", System.currentTimeMillis());
    	this.mainIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
    	this.accessControlIntent = PendingIntent.getActivity(this, 1, new Intent(this, AccessControlActivity.class), 0);
    	
    	// Initialize configuration
    	this.updateDeviceParameters();
	}

	@Override
	public void onTerminate() {
		Log.d(MSG_TAG, "Calling onTerminate()");
    	// Stopping Tether
		//tetherService.stop();
		// Remove all notifications
		this.notificationManager.cancelAll();
	}
	
	// ClientDataList Add
	public synchronized void addClientData(ClientData clientData) {
		this.clientDataAddList.add(clientData);
	}

	public synchronized void removeClientMac(String mac) {
		this.clientMacRemoveList.add(mac);
	}
	
	public synchronized ArrayList<ClientData> getClientDataAddList() {
		ArrayList<ClientData> tmp = this.clientDataAddList;
		this.clientDataAddList = new ArrayList<ClientData>();
		return tmp;
	}
	
	public synchronized ArrayList<String> getClientMacRemoveList() {
		ArrayList<String> tmp = this.clientMacRemoveList;
		this.clientMacRemoveList = new ArrayList<String>();
		return tmp;
	}	
	
	public synchronized void resetClientMacLists() {
		this.clientDataAddList = new ArrayList<ClientData>();
		this.clientMacRemoveList = new ArrayList<String>();
	}
	
	public void updateDeviceParameters() {
        String device = this.settings.getString("devicepref", "auto");
        if (device.equals("auto")) {
    		this.configuration = new Configuration();
        }
        else {
        	this.configuration = new Configuration(device);
        }
	}
	
	public Configuration getDeviceParameters() {
		return this.configuration;
	}
	
	public void updateConfiguration() {
		
		long startStamp = System.currentTimeMillis();

		// Updating configuration
		updateDeviceParameters();
		
		boolean encEnabled = this.settings.getBoolean("encpref", false);
		boolean acEnabled = this.settings.getBoolean("acpref", false);
		String ssid = this.settings.getString("ssidpref", "AndroidTether");
        String txpower = this.settings.getString("txpowerpref", "disabled");
        String lannetwork = this.settings.getString("lannetworkpref", DEFAULT_LANNETWORK);
        String wepkey = this.settings.getString("passphrasepref", DEFAULT_PASSPHRASE);
        String wepsetupMethod = this.settings.getString("encsetuppref", DEFAULT_ENCSETUP);
        String channel = this.settings.getString("channelpref", "1");
        boolean mssclampingEnabled = this.settings.getBoolean("mssclampingpref", true);
        boolean routefixEnabled = this.settings.getBoolean("routefixpref", true);
        String primaryDns = this.settings.getString("dnsprimarypref", "8.8.8.8");
        String secondaryDns = this.settings.getString("dnssecondarypref", "8.8.4.4");
        boolean hideSSID = this.settings.getBoolean("hidessidpref", false);
        boolean reloadDriver = this.settings.getBoolean("driverreloadpref", true);
        boolean reloadDriver2 = this.settings.getBoolean("driverreloadpref2", false);
        boolean netdMaxClientCmd = this.settings.getBoolean("netd.maxclientcmd", false);
        // Check if "auto"-setup method is selected
        String setupMethod = this.settings.getString("setuppref", "auto");
        
        if (configuration.isTiadhocSupported() == false) {
	        if (setupMethod.equals("auto")) {
	        	setupMethod = configuration.getAutoSetupMethod();
	        }
        }
        else {
        	setupMethod = "tiwlan0";
        }
	        
		// tether.conf
        String subnet = lannetwork.substring(0, lannetwork.lastIndexOf("."));
        //this.tethercfg.read();
		this.tethercfg.put("device.type", configuration.getDevice());
        this.tethercfg.put("wifi.essid", ssid);
        this.tethercfg.put("wifi.channel", channel);
		this.tethercfg.put("ip.network", lannetwork.split("/")[0]);
		this.tethercfg.put("ip.gateway", subnet + ".254");
		this.tethercfg.put("ip.netmask", "255.255.255.0");
		
		// dns
		this.tethercfg.put("dns.primary", primaryDns);
		this.tethercfg.put("dns.secondary", secondaryDns);
		
		if (mssclampingEnabled) {
			this.tethercfg.put("mss.clamping", "true");
		}
		else {
			this.tethercfg.put("mss.clamping", "false");
		}
		
		if(netdMaxClientCmd){
			//netdndcmaxclientcmd sets max clients to 25, true might fix stuff
			this.tethercfg.put("netd.maxclientcmd", "true");
		}
		else {
			this.tethercfg.put("netd.maxclientcmd", "false");
		}

		if (hideSSID) {
			this.tethercfg.put("wifi.essid.hide", "1");
		}
		else {
			this.tethercfg.put("wifi.essid.hide", "0");
		}

		//wifi driver reload inside tether script
		if (reloadDriver) {
			this.tethercfg.put("wifi.driver.reload", "true");
		}
		else {
			this.tethercfg.put("wifi.driver.reload", "false");
		}
		
		//TODO: wifi driver hack for outside tether script
		if (reloadDriver2) {
			this.tethercfg.put("wifi.driver.reload2", "true");
		}
		else {
			this.tethercfg.put("wifi.driver.reload2", "false");
		}
		
		if (routefixEnabled) {
			this.tethercfg.put("tether.fix.route", "true");
		}
		else {
			this.tethercfg.put("tether.fix.route", "false");
		}
		
		if (configuration.doWifiFinalDriverLoad()) {
			this.tethercfg.put("wifi.final.load.cmd", Configuration.getWifiFinalloadCmd());
		}
		else {
			this.tethercfg.put("wifi.final.load.cmd", "none");
		}

        // Write tether-section variable
   		this.tethercfg.put("setup.section.generic", ""+configuration.isGenericSetupSection());
   		
   		// Wifi-interface
   		if(this.coretask.getProp("wifi.interface").equals("undefined")){
   			//TODO: put in better undefined check.  this wires it to netd's interface
   			this.tethercfg.put("wifi.interface", configuration.getNetdInterface());
   		} else {
   			this.tethercfg.put("wifi.interface", this.coretask.getProp("wifi.interface"));
   		}
   		
		this.tethercfg.put("wifi.driver", setupMethod);
		if (setupMethod.equals("wext")) {
			this.tethercfg.put("tether.interface", this.tethercfg.get("wifi.interface"));
			if (encEnabled) {
				this.tethercfg.put("wifi.encryption", "wep");
			}
		}
		else if (setupMethod.equals("netd") || setupMethod.equals("netdndc")) {
			this.tethercfg.put("tether.interface", configuration.getNetdInterface());
			if (encEnabled) {
				this.tethercfg.put("wifi.encryption", configuration.getEncryptionIdentifier());
			}
			else {
				this.tethercfg.put("wifi.encryption", configuration.getOpennetworkIdentifier());
			}
		}
		else if (setupMethod.equals("hostapd")) {
			this.tethercfg.put("hostapd.module.path", configuration.getHostapdKernelModulePath());
			this.tethercfg.put("hostapd.module.name", configuration.getHostapdKernelModuleName());
			this.tethercfg.put("hostapd.bin.path", configuration.getHostapdPath());
			this.tethercfg.put("tether.interface", configuration.getHostapdInterface());
			if (encEnabled) {
				this.tethercfg.put("wifi.encryption", "unused");
			}
			if (configuration.getHostapdLoaderCmd() == null || configuration.getHostapdLoaderCmd().length() <= 0) {
				this.tethercfg.put("hostapd.loader.cmd", "disabled");
			}
			else {
				this.tethercfg.put("hostapd.loader.cmd", configuration.getHostapdLoaderCmd());
			}
		}
		else if (setupMethod.equals("tiwlan0")) {
			this.tethercfg.put("tether.interface", configuration.getTiadhocInterface());
			if (encEnabled) {
				this.tethercfg.put("wifi.encryption", "wep");
			}
		}		
		else if (setupMethod.startsWith("softap")) {
			this.tethercfg.put("tether.interface", configuration.getSoftapInterface());
			this.tethercfg.put("wifi.firmware.path", configuration.getSoftapFirmwarePath());
			if (encEnabled) {
				this.tethercfg.put("wifi.encryption", configuration.getEncryptionIdentifier());
			}
			else {
				this.tethercfg.put("wifi.encryption", configuration.getOpennetworkIdentifier());
			}
		}

		this.tethercfg.put("wifi.load.cmd", Configuration.getWifiLoadCmd());
		this.tethercfg.put("wifi.unload.cmd", Configuration.getWifiUnloadCmd());
		
		this.tethercfg.put("wifi.txpower", txpower);

		// Encryption
		if (encEnabled) {
			// Storing wep-key
			this.tethercfg.put("wifi.encryption.key", wepkey);

			// Getting encryption-method if setup-method on auto 
			if (wepsetupMethod.equals("auto")) {
				if (configuration.isWextSupported()) {
					wepsetupMethod = "iwconfig";
				}
				else if (configuration.isTiadhocSupported()) {
					wepsetupMethod = "wpa_supplicant";
				}
			}
			// Setting setup-mode
			this.tethercfg.put("wifi.setup", wepsetupMethod);
			// Prepare wpa_supplicant-config if wpa_supplicant selected
			if (wepsetupMethod.equals("wpa_supplicant")) {
				// Install wpa_supplicant.conf-template
				if (this.wpasupplicant.exists() == false) {
					this.installWpaSupplicantConfig();
				}
				
				// Update wpa_supplicant.conf
				Hashtable<String,String> values = new Hashtable<String,String>();
				values.put("ssid", "\""+this.settings.getString("ssidpref", "AndroidTether")+"\"");
				values.put("wep_key0", "\""+this.settings.getString("passphrasepref", DEFAULT_PASSPHRASE)+"\"");
				this.wpasupplicant.write(values);
			}
        }
		else {
			this.tethercfg.put("wifi.encryption", "open");
			this.tethercfg.put("wifi.encryption.key", "none");
			
			// Make sure to remove wpa_supplicant.conf
			if (this.wpasupplicant.exists()) {
				this.wpasupplicant.remove();
			}			
		}
		
		// DNS Ip-Range
        String[] lanparts = lannetwork.split("\\.");
        this.tethercfg.put("dhcp.iprange", lanparts[0]+"."+lanparts[1]+"."+lanparts[2]+".100,"+lanparts[0]+"."+lanparts[1]+"."+lanparts[2]+".108,12h");
        
		// writing config-file
		if (this.tethercfg.write() == false) {
			Log.e(MSG_TAG, "Unable to update tether.conf!");
		}       
		
		// hostapd.conf
		if (setupMethod.equals("hostapd")) {
			this.installHostapdConfig(configuration.getHostapdTemplate());
			this.hostapdcfg.read();
			
			// Update the hostapd-configuration in case we have Motorola Droid X
			if (configuration.getHostapdTemplate().equals("droi")) {
				this.hostapdcfg.put("ssid", ssid);
				this.hostapdcfg.put("channel", channel);
				this.hostapdcfg.put("interface", configuration.getHostapdInterface());
				if (encEnabled) {
					this.hostapdcfg.put("wpa", ""+2);
					this.hostapdcfg.put("wpa_pairwise", "CCMP");
					this.hostapdcfg.put("rsn_pairwise", "CCMP");
					this.hostapdcfg.put("wpa_passphrase", wepkey);
				}
			}
			// Update the hostapd-configuration in case we have ZTE Blade
			else if (configuration.getHostapdTemplate().equals("mini")) {
				this.hostapdcfg.put("ssid", ssid);
				this.hostapdcfg.put("channel_num", channel);
				if (encEnabled) {
					this.hostapdcfg.put("wpa", ""+2);
					this.hostapdcfg.put("wpa_key_mgmt", "WPA-PSK");
					this.hostapdcfg.put("wpa_pairwise", "CCMP");
					this.hostapdcfg.put("wpa_passphrase", wepkey);
				}
				if (netdMaxClientCmd){
					this.hostapdcfg.put("max_num_sta", "25");
					this.hostapdcfg.put("ieee80211n", "1");
					this.hostapdcfg.put("ctrl_interface", "/data/misc/wifi/hostapd");
				}
			}
			// Update the hostapd-configuration in case we have a ???
			else if (configuration.getHostapdTemplate().equals("tiap")) {
				this.hostapdcfg.put("ssid", ssid);
				this.hostapdcfg.put("channel", channel);
				this.hostapdcfg.put("interface", configuration.getHostapdInterface());
				if (encEnabled) {
					this.hostapdcfg.put("wpa", ""+2);
					this.hostapdcfg.put("wpa_pairwise", "CCMP");
					this.hostapdcfg.put("rsn_pairwise", "CCMP");
					this.hostapdcfg.put("wpa_passphrase", wepkey);
				}
			}
			
			if (this.hostapdcfg.write() == false) {
				Log.e(MSG_TAG, "Unable to update hostapd.conf!");
			}
		}
		
		// whitelist
		if (acEnabled) {
			if (this.whitelist.exists() == false) {
				try {
					this.whitelist.touch();
				} catch (IOException e) {
					Log.e(MSG_TAG, "Unable to update whitelist-file!");
					e.printStackTrace();
				}
			}
		}
		else {
			if (this.whitelist.exists()) {
				this.whitelist.remove();
			}
		}
		
		if (configuration.isTiadhocSupported()) {
			TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/tiwlan.ini", "0644", R.raw.tiwlan_ini);
			Hashtable<String,String> values = this.tiwlan.get();
			values.put("dot11DesiredSSID", this.settings.getString("ssidpref", "AndroidTether"));
			values.put("dot11DesiredChannel", this.settings.getString("channelpref", "1"));
			this.tiwlan.write(values);
		}
		else {
			File tiwlanconf = new File(CoreTask.DATA_FILE_PATH+"/conf/tiwlan.ini");
			if (tiwlanconf.exists()) {
				tiwlanconf.delete();
			}
		}
		
		
		Log.d(MSG_TAG, "Creation of configuration-files took ==> "+(System.currentTimeMillis()-startStamp)+" milliseconds.");
	}
	
    public String getTetherNetworkDevice() {
    	return this.tethercfg.get("tether.interface");
    }
    
    // gets user preference on whether wakelock should be disabled during tethering
    public boolean isWakeLockDisabled(){
		return this.settings.getBoolean("wakelockpref", true);
	} 
	
    // gets user preference on whether sync should be disabled during tethering
    public boolean isSyncDisabled(){
		return this.settings.getBoolean("syncpref", false);
	}
    
    // gets user preference on whether sync should be disabled during tethering
    public boolean isUpdatecDisabled(){
		return this.settings.getBoolean("updatepref", false);
	}
    
    // get preferences on whether donate-dialog should be displayed
    public boolean showDonationDialog() {
    	return this.settings.getBoolean("donatepref", true);
    }

    
    // WakeLock
	public void releaseWakeLock() {
		try {
			if(this.wakeLock != null && this.wakeLock.isHeld()) {
				Log.d(MSG_TAG, "Trying to release WakeLock NOW!");
				this.wakeLock.release();
			}
		} catch (Exception ex) {
			Log.d(MSG_TAG, "Ups ... an exception happend while trying to release WakeLock - Here is what I know: "+ex.getMessage());
		}
	}
    
	public void acquireWakeLock() {
		try {
			if (this.isWakeLockDisabled() == false) {
				Log.d(MSG_TAG, "Trying to acquire WakeLock NOW!");
				this.wakeLock.acquire();
			}
		} catch (Exception ex) {
			Log.d(MSG_TAG, "Ups ... an exception happend while trying to acquire WakeLock - Here is what I know: "+ex.getMessage());
		}
	}
    
    public int getNotificationType() {
		return Integer.parseInt(this.settings.getString("notificationpref", "2"));
    }
    
    // Notification
    public Notification getStartNotification() {
		notification.flags = Notification.FLAG_ONGOING_EVENT;
    	notification.setLatestEventInfo(this, getString(R.string.global_application_name), getString(R.string.global_application_tethering_running), this.mainIntent);
    	this.notificationManager.notify(-1, this.notification);
    	return notification;
    }
    
    Handler clientConnectHandler = new Handler() {
 	   public void handleMessage(Message msg) {
 		    ClientData clientData = (ClientData)msg.obj;
 		    TetherApplication.this.showClientConnectNotification(clientData, msg.what);
 	   }
    };
    
    Handler shutdownHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		showShutdownNotification();
    	}
    };

    public void showShutdownNotification() {
 	   	Notification shutdownNotification = new Notification(R.drawable.stop_notification, getString(R.string.global_application_name), System.currentTimeMillis());
 	   	if (this.settings.getBoolean("autoshutdownidle", true))
 	   		shutdownNotification.tickerText = getString(R.string.global_application_tethering_shutdown_inactive);
 	   	else if (this.settings.getBoolean("autoshutdowntimer", true))
 	   		shutdownNotification.tickerText = getString(R.string.global_application_tethering_shutdown_timer);
 	   	else if (this.settings.getBoolean("autoshutdownquota", true))
 	   		shutdownNotification.tickerText = getString(R.string.global_application_tethering_quotashutdown_limit);
 	   	else if (this.settings.getBoolean("autoshutdownkeepalive", true))
 	   		shutdownNotification.tickerText = getString(R.string.global_application_tethering_shutdown_keepalive);

   		this.preferenceEditor.putBoolean("autoshutdownidle", false);
   		this.preferenceEditor.putBoolean("autoshutdowntimer", false);
   		this.preferenceEditor.putBoolean("autoshutdownquota", false);
   		this.preferenceEditor.putBoolean("autoshutdownkeepalive", false);
   		this.preferenceEditor.commit();
   		shutdownNotification.setLatestEventInfo(this, getString(R.string.global_application_name), shutdownNotification.tickerText, this.mainIntent);
 	   	shutdownNotification.flags = Notification.FLAG_AUTO_CANCEL;
 	   	notificationManager.notify(-1, shutdownNotification);    	
    }

    public void showClientConnectNotification(ClientData clientData, int authType) {
    	int notificationIcon = R.drawable.secmedium;
    	String notificationString = "";
    	switch (authType) {
	    	case CLIENT_CONNECT_ACDISABLED :
	    		notificationIcon = R.drawable.secmedium;
	    		notificationString = getString(R.string.global_application_accesscontrol_disabled);
	    		break;
	    	case CLIENT_CONNECT_AUTHORIZED :
	    		notificationIcon = R.drawable.sechigh;
	    		notificationString = getString(R.string.global_application_accesscontrol_authorized);
	    		break;
	    	case CLIENT_CONNECT_NOTAUTHORIZED :
	    		notificationIcon = R.drawable.seclow;
	    		notificationString = getString(R.string.global_application_accesscontrol_authorized);
    	}
		Log.d(MSG_TAG, "New (" + notificationString + ") client connected ==> "+clientData.getClientName()+" - "+clientData.getMacAddress());
 	   	Notification clientConnectNotification = new Notification(notificationIcon, getString(R.string.global_application_name), System.currentTimeMillis());
 	   	clientConnectNotification.tickerText = clientData.getClientName()+" ("+clientData.getMacAddress()+")";
 	   	if (!settings.getString("notifyring", "").equals(""))
 	   		clientConnectNotification.sound = Uri.parse(this.settings.getString("notifyring", ""));

 	   	if(settings.getBoolean("notifyvibrate", true))
 	   		clientConnectNotification.vibrate = new long[] {100, 200, 100, 200};

 	   	if (accessControlSupported) 
 	   		clientConnectNotification.setLatestEventInfo(this, getString(R.string.global_application_name)+" - " + notificationString, clientData.getClientName()+" ("+clientData.getMacAddress()+") "+getString(R.string.global_application_connected)+" ...", this.accessControlIntent);
 	   	else 
 	   		clientConnectNotification.setLatestEventInfo(this, getString(R.string.global_application_name)+" - " + notificationString, clientData.getClientName()+" ("+clientData.getMacAddress()+") "+getString(R.string.global_application_connected)+" ...", this.mainIntent);
 	   	
 	   	clientConnectNotification.flags = Notification.FLAG_AUTO_CANCEL;
 	   	notificationManager.notify(this.clientNotificationCount, clientConnectNotification);
 	   	clientNotificationCount++;
    }    
    
    public boolean binariesExists() {
    	File file = new File(CoreTask.DATA_FILE_PATH+"/bin/tether");
    	return file.exists();
    }
    
    public void installWpaSupplicantConfig() {
    	this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/wpa_supplicant.conf", "0644", R.raw.wpa_supplicant_conf);
    }
    
    public void installHostapdConfig(String hostapdTemplate) {
    	if (hostapdTemplate.equals("droi")) {
    		this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/hostapd.conf", "0644", R.raw.hostapd_conf_droi);
    	}
    	else if (hostapdTemplate.equals("mini")) {
    		this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/hostapd.conf", "0644", R.raw.hostapd_conf_mini);
    	}
    	else if (hostapdTemplate.equals("tiap")) {
    		this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/hostapd.conf", "0644", R.raw.hostapd_conf_tiap);
    	}
    }
    
    Handler displayMessageHandler = new Handler(){
        public void handleMessage(Message msg) {
       		if (msg.obj != null) {
       			TetherApplication.this.displayToastMessage((String)msg.obj);
       		}
        	super.handleMessage(msg);
        }
    };

    public void installFiles() {
		String message = null;
		// tether
		if (message == null) {
	    	message = TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/bin/tether", "0755", R.raw.tether);
		}
		// dnsmasq
		if (message == null) {
	    	message = TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/bin/dnsmasq", "0755", R.raw.dnsmasq);
		}
		// iptables
		if (message == null) {
	    	message = TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/bin/iptables", "0755", R.raw.iptables);
		}
		// iwconfig
		if (message == null) {
	    	message = TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/bin/iwconfig", "0755", R.raw.iwconfig);
		}
		// ifconfig
		if (message == null) {
	    	message = TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/bin/ifconfig", "0755", R.raw.ifconfig);
		}
    	/*
		if (configuration.enableFixPersist()) {	
			// fixpersist.sh
			if (message == null) {
				message = TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/bin/fixpersist.sh", "0755", R.raw.fixpersist_sh);
			}				
		}*/
		// edify script
		if (message == null) {
			TetherApplication.this.copyFile(CoreTask.DATA_FILE_PATH+"/conf/tether.edify", "0644", R.raw.tether_edify);
		}
		// tether.cfg
		/*if (message == null) {
			TetherApplication.this.copyFile(TetherApplication.this.coretask.DATA_FILE_PATH+"/conf/tether.conf", "0644", R.raw.tether_conf);
		}*/
		
		// wpa_supplicant drops privileges, we need to make files readable.
		TetherApplication.this.coretask.chmod(CoreTask.DATA_FILE_PATH+"/conf/", "0755");
	
		if (message == null) {
	    	message = getString(R.string.global_application_installed);
		}
		
		// Sending message
		Message msg = new Message();
		msg.obj = message;
		displayMessageHandler.sendMessage(msg);
    }
    
    /*
     * Update checking. We go to a predefined URL and fetch a properties style file containing
     * information on the update. These properties are:
     * 
     * versionCode: An integer, version of the new update, as defined in the manifest. Nothing will
     *              happen unless the update properties version is higher than currently installed.
     * fileName: A string, URL of new update apk. If not supplied then download buttons
     *           will not be shown, but instead just a message and an OK button.
     * message: A string. A yellow-highlighted message to show to the user. Eg for important
     *          info on the update. Optional.
     * title: A string, title of the update dialog. Defaults to "Update available".
     * 
     * Only "versionCode" is mandatory.
     */
    public void checkForUpdate() {
    	if (this.isUpdatecDisabled()) {
    		Log.d(MSG_TAG, "Update-checks are disabled!");	
    		return;
    	}
    	new Thread(new Runnable(){
			public void run(){
				Looper.prepare();
				// Getting Properties
				Properties updateProperties = TetherApplication.this.webserviceTask.queryForProperty(APPLICATION_PROPERTIES_URL);
				if (updateProperties != null && updateProperties.containsKey("versionCode")) {
				  
					int availableVersion = Integer.parseInt(updateProperties.getProperty("versionCode"));
					int installedVersion = TetherApplication.this.getVersionNumber();
					String fileName = updateProperties.getProperty("fileName", "");
					String updateMessage = updateProperties.getProperty("message", "");
					String updateTitle = updateProperties.getProperty("title", "Update available");
					if (availableVersion != installedVersion) {
						Log.d(MSG_TAG, "Installed version '"+installedVersion+"' and available version '"+availableVersion+"' do not match!");
						MainActivity.currentInstance.openUpdateDialog(APPLICATION_DOWNLOAD_URL+fileName,
						    fileName, updateMessage, updateTitle);
					}
				}
				Looper.loop();
			}
    	}).start();
    }
   
    public void downloadUpdate(final String downloadFileUrl, final String fileName) {
    	new Thread(new Runnable(){
			public void run(){
				Message msg = Message.obtain();
            	msg.what = MainActivity.MESSAGE_DOWNLOAD_STARTING;
            	msg.obj = "Downloading update...";
            	MainActivity.currentInstance.viewUpdateHandler.sendMessage(msg);
				TetherApplication.this.webserviceTask.downloadUpdateFile(downloadFileUrl, fileName);
				Intent intent = new Intent(Intent.ACTION_VIEW); 
			    intent.setDataAndType(android.net.Uri.fromFile(new File(WebserviceTask.DOWNLOAD_FILEPATH+"/"+fileName)),"application/vnd.android.package-archive"); 
			    MainActivity.currentInstance.startActivity(intent);
			}
    	}).start();
    }
    
    private String copyFile(String filename, String permission, int ressource) {
    	String result = this.copyFile(filename, ressource);
    	if (result != null) {
    		return result;
    	}
    	if (this.coretask.chmod(filename, permission) != true) {
    		result = "Can't change file-permission for '"+filename+"'!";
    	}
    	return result;
    }
    
    private String copyFile(String filename, int ressource) {
    	File outFile = new File(filename);
    	Log.d(MSG_TAG, "Copying file '"+filename+"' ...");
    	InputStream is = this.getResources().openRawResource(ressource);
    	byte buf[] = new byte[1024];
        int len;
        try {
        	OutputStream out = new FileOutputStream(outFile);
        	while((len = is.read(buf))>0) {
				out.write(buf,0,len);
			}
        	out.close();
        	is.close();
		} catch (IOException e) {
			return "Couldn't install file - "+filename+"!";
		}
		return null;
    }
    
    private void checkDirs() {
    	File dir = new File(CoreTask.DATA_FILE_PATH);
    	if (dir.exists() == false) {
    			this.displayToastMessage("Application data-dir does not exist!");
    	}
    	else {
    		//String[] dirs = { "/bin", "/var", "/conf", "/library" };
    		String[] dirs = { "/bin", "/var", "/conf" };
    		for (String dirname : dirs) {
    			dir = new File(CoreTask.DATA_FILE_PATH + dirname);
    	    	if (dir.exists() == false) {
    	    		if (!dir.mkdir()) {
    	    			this.displayToastMessage("Couldn't create " + dirname + " directory!");
    	    		}
    	    	}
    	    	else {
    	    		Log.d(MSG_TAG, "Directory '"+dir.getAbsolutePath()+"' already exists!");
    	    	}
    		}
    	}
    }
    
    // Display Toast-Message
	public void displayToastMessage(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	}
    
    public int getVersionNumber() {
    	int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }
    
    public String getVersionName() {
    	String version = "?";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (Exception e) {
            Log.e(MSG_TAG, "Package name not found", e);
        }
        return version;
    }

    /*
     * This method checks if changing the transmit-power is supported
     */
    public boolean isTransmitPowerSupported() {
    	// TODO
    	// Only supported for the nexusone 
    	/*if (Configuration.getWifiInterfaceDriver(deviceType).equals(Configuration.DRIVER_WEXT)) {
    		return true;
    	}
    	return false;*/
    	
    	return true;
    }    
}
