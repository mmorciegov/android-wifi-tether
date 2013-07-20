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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import com.googlecode.android.wifi.tether.system.Configuration;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public class SetupActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private TetherApplication application = null;
	
	private ProgressDialog progressDialog;
	
	public static final String TAG = "TETHER -> SetupActivity";

	private String currentDevice;
	private String currentSetup;
	private String currentSSID;
	private String currentMAC;
    private String currentChannel;
    private String currentPassphrase;
    private String currentLAN;
    private boolean currentEncryptionEnabled;
    private String currentTransmitPower;
    private boolean currentMssclampingEnabled;
    private boolean currentRoutefixEnabled;
    private String currentPrimaryDNS;
    private String currentSecondaryDNS;
    private boolean currentHideSSID;
    private boolean currentMacSpoofEnabled;
    private boolean currentDriverReload;
    private String keepaliveshutdown;
    private boolean currentNetdNoIfaceCmd;
    
    private EditTextPreference prefPassphrase;
    private EditTextPreference prefSSID;
    private EditTextPreference prefPrimaryDNS;
    private EditTextPreference prefSecondaryDNS;
    private ListPreference keepaliveshutdownoption;
    private CheckBoxPreference macspoofoption;
    private CheckBoxPreference driverreloadpref1;
    private CheckBoxPreference driverreloadpref2;
    
    private static int ID_DIALOG_RESTARTING = 2;
    private IntentFilter intentFilter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Init Application
        this.application = (TetherApplication)this.getApplication();
  
        // Init CurrentSettings
        this.currentDevice = this.application.settings.getString("devicepref", "auto");
        this.currentSetup = this.application.settings.getString("setuppref", "auto");
        this.currentSSID = this.application.settings.getString("ssidpref", "AndroidTether"); 
        this.currentMacSpoofEnabled = this.application.settings.getBoolean("tether.macspoof", false);
        this.currentMAC = this.application.settings.getString("macspoof.addr", "00:11:22:33:44:55"); 
        this.currentChannel = this.application.settings.getString("channelpref", "1");
        this.currentPassphrase = this.application.settings.getString("passphrasepref", this.application.DEFAULT_PASSPHRASE);
        this.currentLAN = this.application.settings.getString("lannetworkpref", this.application.DEFAULT_LANNETWORK);
        this.currentEncryptionEnabled = this.application.settings.getBoolean("encpref", false);
        this.currentTransmitPower = this.application.settings.getString("txpowerpref", "disabled");
        this.currentMssclampingEnabled = this.application.settings.getBoolean("mssclampingpref", this.application.coretask.isMSSClampingSupported());
        this.currentRoutefixEnabled = this.application.settings.getBoolean("routefixpref", this.application.coretask.isRoutefixSupported());
        this.currentPrimaryDNS = this.application.settings.getString("dnsprimarypref", "8.8.8.8");
        this.currentSecondaryDNS = this.application.settings.getString("dnssecondarypref", "8.8.4.4");
        this.currentHideSSID = this.application.settings.getBoolean("hidessidpref", false);
        this.currentDriverReload = this.application.settings.getBoolean("driverreloadpref", true);
        this.currentDriverReload = this.application.settings.getBoolean("driverreloadpref2", false);
        this.keepaliveshutdown = this.application.settings.getString("keepalivecheckoptionpref", "karetry");
        this.currentNetdNoIfaceCmd = this.application.settings.getBoolean("netd.notetherifacecmd", this.application.coretask.isNdcNoTetherCmdSupported());
        
        // Updating settings-menu
        this.updateSettingsMenu();
        
        // Disable Security (Access Control) if not supported
        if (!this.application.accessControlSupported) {
			PreferenceGroup securityGroup = (PreferenceGroup)findPreference("securityprefs");
			securityGroup.setEnabled(false);
        }
        
		// SSID-Validation
		this.prefSSID = (EditTextPreference) findPreference("ssidpref");
		this.prefSSID
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String message = validateSSID(newValue.toString());
						if (!message.equals("")) {
							SetupActivity.this.application
									.displayToastMessage(message);
							return false;
						}
						return true;
					}
				});
        
		// Primary-DNS Validation
		this.prefPrimaryDNS = (EditTextPreference) findPreference("dnsprimarypref");
		this.prefPrimaryDNS
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String message = validateIpAddress(newValue.toString());
						if (!message.equals("")) {
							SetupActivity.this.application
									.displayToastMessage(message);
							return false;
						}
						return true;
					}
				});
		
		// Primary-DNS Validation
		this.prefSecondaryDNS = (EditTextPreference) findPreference("dnssecondarypref");
		this.prefSecondaryDNS
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String message = validateIpAddress(newValue.toString());
						if (!message.equals("")) {
							SetupActivity.this.application
									.displayToastMessage(message);
							return false;
						}
						return true;
					}
				});

		// Keep-Alive shutdown option list
		this.keepaliveshutdownoption = (ListPreference) findPreference("keepalivecheckoptionpref");
		this.keepaliveshutdownoption
				.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String message = newValue.toString();
						if (message.equals("kashutdown")) {
							getPreferenceScreen().findPreference("keepalivecheckprefcheckintervalshutdownpref").setEnabled(true);
							return true;
						} else if (message.equals("karetry")) {
							getPreferenceScreen().findPreference("keepalivecheckprefcheckintervalshutdownpref").setEnabled(false);
							return true;
						} else {
							SetupActivity.this.application.displayToastMessage(message);
							getPreferenceScreen().findPreference("keepalivecheckprefcheckintervalshutdownpref").setEnabled(false);
							return false;
						}
					}
				});
		
		// MacSpoof option list
		this.macspoofoption = (CheckBoxPreference) findPreference("tether.macspoof");
		macspoofoption.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
	    public boolean onPreferenceChange(Preference preference, Object newValue) {
	        if (newValue.toString().equals("true")) {
	            getPreferenceScreen().findPreference("macspoof.addr").setEnabled(true);
	        } else {
	            getPreferenceScreen().findPreference("macspoof.addr").setEnabled(false);
	        }
	        return true;
	    }
	    });
		this.driverreloadpref1 = (CheckBoxPreference) findPreference("driverreloadpref");
		this.driverreloadpref2 = (CheckBoxPreference) findPreference("driverreloadpref2");
		if(driverreloadpref1 != null){
			// driverreload1 can be null 
		driverreloadpref1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
	    public boolean onPreferenceChange(Preference preference, Object newValue) {
	        if (newValue.toString().equals("true")) {
	            getPreferenceScreen().findPreference("driverreloadpref2").setEnabled(false);
	            driverreloadpref2.setChecked(false);
	        } else {
	            getPreferenceScreen().findPreference("driverreloadpref2").setEnabled(true);
	        }
	        return true;
	    }
	    });
		}
		if(driverreloadpref2 != null){
		// driverreload2
		driverreloadpref2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
	    public boolean onPreferenceChange(Preference preference, Object newValue) {
	        if (newValue.toString().equals("true")) {
	            getPreferenceScreen().findPreference("driverreloadpref").setEnabled(false);
	            driverreloadpref1.setChecked(false);
	        } else {
	            getPreferenceScreen().findPreference("driverreloadpref").setEnabled(true);
	        }
	        return true;
	    }
	    });
		}
    }
	
    @Override
	protected void onStop() {
    	Log.d(TAG, "Calling onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "Calling onDestroy()");
		super.onDestroy();
	}

	@Override
    protected void onResume() {
    	Log.d(TAG, "Calling onResume()");
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		// Initialize Intent-Filter
		intentFilter = new IntentFilter();
		// Add an intent-Filter for Tethering-State-Changes
        intentFilter.addAction(TetherService.STATECHANGED_INTENT);
        // Register Intent-Receiver
        registerReceiver(intentReceiver, intentFilter);
	}
    
    @Override
    protected void onPause() {
    	Log.d(TAG, "Calling onPause()");
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);   
        
        // unregister Intent-Receiver
		try {
			unregisterReceiver(this.intentReceiver);
		} catch (Exception ex) {;}   
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == ID_DIALOG_RESTARTING) {
	    	progressDialog = new ProgressDialog(this);
	    	progressDialog.setTitle(getString(R.string.setup_activity_restart_tethering_title));
	    	progressDialog.setMessage(getString(R.string.setup_activity_restart_tethering_message));
	    	progressDialog.setIndeterminate(false);
	    	progressDialog.setCancelable(true);
	        return progressDialog;
    	}
    	return null;
    }
    
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	updateConfiguration(sharedPreferences, key);
    }
    
    Handler restartingDialogHandler = new Handler(){
        public void handleMessage(Message msg) {
        	if (msg.what == 0)
        		SetupActivity.this.showDialog(SetupActivity.ID_DIALOG_RESTARTING);
        	else
        		SetupActivity.this.dismissDialog(SetupActivity.ID_DIALOG_RESTARTING);
        	super.handleMessage(msg);
        }
    };
    
   Handler displayToastMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
       		if (msg.obj != null) {
       			SetupActivity.this.application.displayToastMessage((String)msg.obj);
       		}
        	super.handleMessage(msg);
        }
    };
    
    /**
     *Listens for intent broadcasts; Needed for the temperature-display
     */
     private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             if (action.equals(TetherService.STATECHANGED_INTENT)) {
            	 switch (intent.getIntExtra("state", TetherService.STATE_IDLE)) {
            	   	case TetherService.STATE_RESTARTING :
            	   		showDialog(SetupActivity.ID_DIALOG_RESTARTING);
            	   		break;
            	   	case TetherService.STATE_RUNNING :
            	   		dismissDialog(SetupActivity.ID_DIALOG_RESTARTING);
            	   		break;
            	   	default:
            	   		dismissDialog(SetupActivity.ID_DIALOG_RESTARTING);
            	   		break;          	   		
            	 }
             }
         }
     };
    
    private void updateSettingsMenu() {

        Resources resources = getResources();
        
    	CharSequence[] targetentries;
    	CharSequence[] targetentryvalues;
        
    	// add XML-Layout
    	if (getPreferenceScreen() != null) {
    		getPreferenceScreen().removeAll();
    	}
    	addPreferencesFromResource(R.layout.setupview);
        
    	
        // Check if "auto"-setup method is selected
        String setupMethod = this.application.settings.getString("setuppref", "auto");
        if (setupMethod.equals("auto")) {
        	setupMethod = this.application.getDeviceParameters().getAutoSetupMethod();
        }
    	
        // Passphrase-Validation
        this.prefPassphrase = (EditTextPreference)findPreference("passphrasepref");
        final int origTextColorPassphrase = SetupActivity.this.prefPassphrase.getEditText().getCurrentTextColor();

        
        if (setupMethod.equals("netd") || setupMethod.equals("netdndc") || setupMethod.equals("hostapd") || setupMethod.startsWith("softap")) {

        	Log.d(TAG, "Adding validators for WPA-Encryption.");
        	this.prefPassphrase.setSummary(getString(R.string.setup_layout_passphrase_summary)+" (WPA/WPA2-PSK)");
        	this.prefPassphrase.setDialogMessage(getString(R.string.setup_activity_error_passphrase_info));
	        // Passphrase Change-Listener for WPA-encryption
        	this.prefPassphrase.getEditText().addTextChangedListener(new TextWatcher() {
	            public void afterTextChanged(Editable s) {
	            	// Nothing
	            }
		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        	// Nothing
		        }
		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        	if (s.length() < 8 || s.length() > 30) {
		        		SetupActivity.this.prefPassphrase.getEditText().setTextColor(Color.RED);
		        	}
		        	else {
		        		SetupActivity.this.prefPassphrase.getEditText().setTextColor(origTextColorPassphrase);
		        	}
		        }
	        });
        	
	        this.prefPassphrase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
	        	public boolean onPreferenceChange(Preference preference,
						Object newValue) {
		        	String validChars = "ABCDEFGHIJKLMONPQRSTUVWXYZ" +
                      "abcdefghijklmnopqrstuvwxyz" +
                      "0123456789";
	        		if (newValue.toString().length() < 8) {
	        			SetupActivity.this.application.displayToastMessage(getString(R.string.setup_activity_error_passphrase_tooshort));
	        			return false;
	        		}
	        		else if (newValue.toString().length() > 30) {
	        			SetupActivity.this.application.displayToastMessage(getString(R.string.setup_activity_error_passphrase_toolong));
	        			return false;	        			
	        		}
	        		for (int i = 0 ; i < newValue.toString().length() ; i++) {
	        		    if (!validChars.contains(newValue.toString().substring(i, i+1))) {
	        		      SetupActivity.this.application.displayToastMessage(getString(R.string.setup_activity_error_passphrase_invalidchars));
	        		      return false;
	        		    }
	        		  }
	        		return true;
	        	}
	        }); 
        }
        else {
        	Log.d(TAG, "Adding validators for WEP-Encryption.");
        	this.prefPassphrase.setSummary(getString(R.string.setup_layout_passphrase_summary)+" (WEP 128-bit)");
        	this.prefPassphrase.setDialogMessage(getString(R.string.setup_activity_error_passphrase_13chars));
        	// Passphrase Change-Listener for WEP-encryption
	        this.prefPassphrase.getEditText().addTextChangedListener(new TextWatcher() {
	            public void afterTextChanged(Editable s) {
	            	// Nothing
	            }
		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        	// Nothing
		        }
		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        	if (s.length() == 13) {
		        		SetupActivity.this.prefPassphrase.getEditText().setTextColor(origTextColorPassphrase);
		        	}
		        	else {
		        		 SetupActivity.this.prefPassphrase.getEditText().setTextColor(Color.RED);
		        	}
		        }
	        });
	        
	        this.prefPassphrase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
	        	public boolean onPreferenceChange(Preference preference,
						Object newValue) {
	        	  String validChars = "ABCDEFGHIJKLMONPQRSTUVWXYZ" +
	        	                      "abcdefghijklmnopqrstuvwxyz" +
	        	                      "0123456789";
	        		if(newValue.toString().length() == 13){
	        		  for (int i = 0 ; i < 13 ; i++) {
	        		    if (!validChars.contains(newValue.toString().substring(i, i+1))) {
	        		      SetupActivity.this.application.displayToastMessage(getString(R.string.setup_activity_error_passphrase_invalidchars));
	        		      return false;
	        		    }
	        		  }
	        			return true;
	        		}
	        		else{
	        			SetupActivity.this.application.displayToastMessage(getString(R.string.setup_activity_error_passphrase_tooshort));
	        			return false;
	        		}
	        }});
        }       
        
        // Disable "Transmit power" if not supported
        if (setupMethod.equals("wext") == false) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	ListPreference txpowerPreference = (ListPreference)findPreference("txpowerpref");
        	wifiGroup.removePreference(txpowerPreference);
        }

        // Disable "encryption-setup-method"
        if (setupMethod.equals("wext") == false) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	ListPreference encsetupPreference = (ListPreference)findPreference("encsetuppref");
        	wifiGroup.removePreference(encsetupPreference);
        }
        
        // Disable hide-ssid-option if setupMethod is not softap
        if (setupMethod.startsWith("softap") == false) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	CheckBoxPreference hideSSIDPreference = (CheckBoxPreference)findPreference("hidessidpref");
        	wifiGroup.removePreference(hideSSIDPreference);
        }

        
        
        // Disable MSS Clamping
        if (application.coretask.isMSSClampingSupported() == false) {
        	PreferenceGroup lanGroup = (PreferenceGroup)findPreference("lanprefs");
        	CheckBoxPreference mssClampingPreference = (CheckBoxPreference)findPreference("mssclampingpref");
        	mssClampingPreference.setChecked(false);
        	lanGroup.removePreference(mssClampingPreference);
        }
        
        // Disabling Force Wifi-Relod && Netd Max Client
        if (!(setupMethod.startsWith("softap") || setupMethod.equals("netd") || setupMethod.equals("netdndc"))) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	CheckBoxPreference reloadWifiPreference = (CheckBoxPreference)findPreference("driverreloadpref");
        	reloadWifiPreference.setChecked(false);
        	wifiGroup.removePreference(reloadWifiPreference);
        	
        	//TODO:hack for loading outside wifitether script
        	CheckBoxPreference reloadWifiPreference2 = (CheckBoxPreference)findPreference("driverreloadpref2");
        	reloadWifiPreference2.setChecked(false);
        	wifiGroup.removePreference(reloadWifiPreference2);
        }

        // netdndc Max Client
        if (!(setupMethod.startsWith("netd"))) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	CheckBoxPreference maxClientPreference = (CheckBoxPreference)findPreference("netd.maxclientcmd");
        	maxClientPreference.setChecked(false);
        	wifiGroup.removePreference(maxClientPreference);
        }

        // netdndc Max Client
    	CheckBoxPreference noTetherIfacePreference = (CheckBoxPreference)findPreference("netd.notetherifacecmd");
        if (!(setupMethod.startsWith("netd"))) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	noTetherIfacePreference.setChecked(false);
        	noTetherIfacePreference.setDefaultValue(false);
        	wifiGroup.removePreference(noTetherIfacePreference);
        }   
        noTetherIfacePreference.setChecked(this.currentNetdNoIfaceCmd);
    	this.application.preferenceEditor.putBoolean("netd.notetherifacecmd", this.currentNetdNoIfaceCmd);
		this.application.preferenceEditor.commit();
        
        // Disable Route-Fix
        if (application.coretask.isRoutefixSupported() == false) {
        	PreferenceGroup lanGroup = (PreferenceGroup)findPreference("lanprefs");
        	CheckBoxPreference routeFixPreference = (CheckBoxPreference)findPreference("routefixpref");
        	lanGroup.removePreference(routeFixPreference);
        }
        
        if (application.configuration.getDevice().equals(Configuration.DEVICE_SPHD700) == false) {
        	PreferenceGroup miscGroup = (PreferenceGroup)findPreference("miscprefs");
        	CheckBoxPreference enable4gPreference = (CheckBoxPreference)findPreference("enable4gpref");
        	miscGroup.removePreference(enable4gPreference);
        }

        // Grey out Keep-Alive shutdown option if not selected.
        if (this.application.settings.getString("keepalivecheckoptionpref", "karetry").equals("karetry")) {
			getPreferenceScreen().findPreference("keepalivecheckprefcheckintervalshutdownpref").setEnabled(false);
        } else {
			getPreferenceScreen().findPreference("keepalivecheckprefcheckintervalshutdownpref").setEnabled(true);
        }


        
        // Remove Auto-Channel option if not supported by device
        ListPreference channelpref = (ListPreference)findPreference("channelpref");
        String[] channelnames = resources.getStringArray(R.array.channelnames);
        String[] channelvalues = resources.getStringArray(R.array.channelvalues);
    	if (setupMethod.startsWith("softap") == false) {
    		targetentries = new CharSequence[channelnames.length-1];
    		targetentryvalues = new CharSequence[channelvalues.length-1];
	        for (int i=1;i<channelnames.length;i++) {
	        	targetentries[i-1] = channelnames[i];
	        	targetentryvalues[i-1] = channelvalues[i];
	        }
    	}
    	else {
    		targetentries = new CharSequence[channelnames.length];
    		targetentryvalues = new CharSequence[channelvalues.length];
    		for (int i=0;i<channelnames.length;i++) {
    			targetentries[i] = channelnames[i];
    			targetentryvalues[i] = channelvalues[i];
    		}
    	}
    	channelpref.setEntries(targetentries);
    	channelpref.setEntryValues(targetentryvalues);
    	
    	
    	// Remove unsupported setup-methods
        ListPreference setuppref = (ListPreference)findPreference("setuppref");
        String[] setupnames = resources.getStringArray(R.array.setupnames);
        String[] setupvalues = resources.getStringArray(R.array.setupvalues);
        
        ArrayList<String> tmpsetupnames = new ArrayList<String>();
        ArrayList<String> tmpsetupvalues = new ArrayList<String>();
		for (int i=0;i<setupvalues.length;i++) {
			
			if (setupvalues[i].equals("netd")) {
				if (this.application.configuration.isNetdSupported() == false) {
					continue;
				}
			}
			else if (setupvalues[i].equals("netdndc")) {
				if (this.application.configuration.isNetdNdcSupported() == false) {
					continue;
				}
			}			
			else if (setupvalues[i].equals("hostapd")) {
				if (this.application.configuration.isHostapdSupported() == false) {
					continue;
				}
			}
			else if (setupvalues[i].equals("softap")) {
				if (this.application.configuration.isSoftapSupported() == false) {
					continue;
				}
			}
			else if (setupvalues[i].equals("softap_samsung")) {
				if (this.application.configuration.isSoftapSamsungSupported() == false) {
					continue;
				}			
			}
			else if (setupvalues[i].equals("wext")) {
				if (this.application.configuration.isWextSupported() == false) {
					continue;
				}			
			}
			else if (setupvalues[i].equals("framework_tether")) {
				if (this.application.configuration.isFrameworkTetherSupported() == false) {
					continue;
				}			
			}
			//framework_tether
			tmpsetupnames.add(setupnames[i]);
			tmpsetupvalues.add(setupvalues[i]);
		}     
		targetentries = new CharSequence[tmpsetupnames.size()];
		targetentryvalues = new CharSequence[tmpsetupvalues.size()];
		for (int i=0;i<tmpsetupnames.size();i++) {
			targetentries[i] = tmpsetupnames.get(i);
			targetentryvalues[i] = tmpsetupvalues.get(i);
		}
		setuppref.setEntries(targetentries);
		setuppref.setEntryValues(targetentryvalues);		
    	
    }
    
    private void updateConfiguration(final SharedPreferences sharedPreferences, final String key) {
    	new Thread(new Runnable(){
			public void run(){
				Looper.prepare();
			   	String message = null;
			   	
			   	if (key.equals("devicepref")) {
			   		String newDevice = sharedPreferences.getString("devicepref", "auto");
			   		
			   		if (SetupActivity.this.currentDevice.equals(newDevice) == false) {
			   			SetupActivity.this.currentDevice = newDevice;
			   			SetupActivity.this.application.updateDeviceParameters();
			   			SetupActivity.this.updateSettingsMenuHandler.sendEmptyMessage(0);
	    				message = getString(R.string.setup_activity_info_device_changedto)+" '"+newDevice+"'.";
	    				try{
		    				if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
		    					TetherService.singleton.restart();
		    				}
	    				}
	    				catch (Exception ex) {
	    					message = getString(R.string.setup_activity_error_restart_tethering);
	    				}
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
			   		}
			   	}
			   	else if (key.equals("setuppref")) {
			   		String newSetup = sharedPreferences.getString("setuppref", "auto");
			   		
			   		if (SetupActivity.this.currentSetup.equals(newSetup) == false) {
			   			SetupActivity.this.currentSetup = newSetup;
			   			SetupActivity.this.application.updateDeviceParameters();
			   			SetupActivity.this.updateSettingsMenuHandler.sendEmptyMessage(0);
	    				message = getString(R.string.setup_activity_info_setup_changedto)+" '"+newSetup+"'.";
	    				try{
	    					if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
	    						TetherService.singleton.restart();
		    				}
	    				}
	    				catch (Exception ex) {
	    					message = getString(R.string.setup_activity_error_restart_tethering);
	    				}
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
			   		}
			   	}			   	
			   	else if (key.equals("ssidpref")) {
		    		String newSSID = sharedPreferences.getString("ssidpref", "AndroidTether");
		    		if (SetupActivity.this.currentSSID.equals(newSSID) == false) {
	    				SetupActivity.this.currentSSID = newSSID;
	    				message = getString(R.string.setup_activity_info_ssid_changedto)+" '"+newSSID+"'.";
	    				try{
	    					if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
	    						TetherService.singleton.restart();
		    				}
	    				}
	    				catch (Exception ex) {
	    					message = getString(R.string.setup_activity_error_restart_tethering);
	    				}
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
			   	else if (key.equals("macspoof.addr")) {
		    		String newMAC = sharedPreferences.getString("macspoof.addr", "00:11:22:33:44:55");
		    		if (SetupActivity.this.currentMAC.equals(newMAC) == false) {
	    				SetupActivity.this.currentMAC = newMAC;
	    				message = "Mac Set to '"+newMAC+"'.";
	    				try{
	    					if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
	    						TetherService.singleton.restart();
		    				}
	    				}
	    				catch (Exception ex) {
	    					message = getString(R.string.setup_activity_error_restart_tethering);
	    				}
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
			   	else if (key.equals("keepalivecheckoptionpref")) {
		    		String newkeepaliveshutdown = sharedPreferences.getString("keepalivecheckoptionpref", "karetry");
		    		if (SetupActivity.this.keepaliveshutdown.equals(newkeepaliveshutdown) == false) {
	    				SetupActivity.this.keepaliveshutdown = newkeepaliveshutdown;

	    				// Send Message
	    				message = getString(R.string.setup_activity_info_keepaliveshutdowntimer);
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("channelpref")) {
		    		String newChannel = sharedPreferences.getString("channelpref", "1");
		    		if (SetupActivity.this.currentChannel.equals(newChannel) == false) {
	    				SetupActivity.this.currentChannel = newChannel;
	    				message = getString(R.string.setup_activity_info_channel_changedto)+" '"+newChannel+"'.";
	    				try{
	    					if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
	    						TetherService.singleton.restart();
		    				}
	    				}
	    				catch (Exception ex) {
	    					message = getString(R.string.setup_activity_error_restart_tethering);
	    				}
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("wakelockpref")) {
					try {
						boolean disableWakeLock = sharedPreferences.getBoolean("wakelockpref", true);
						if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
							if (disableWakeLock){
								SetupActivity.this.application.releaseWakeLock();
								message = getString(R.string.setup_activity_info_wakelock_disabled);
							}
							else{
								SetupActivity.this.application.acquireWakeLock();
								message = getString(R.string.setup_activity_info_wakelock_enabled);
							}
						}
					}
					catch (Exception ex) {
						Log.e(TAG, "Can't enable/disable Wake-Lock!");
					}
					
					// Send Message
	    			Message msg = new Message();
	    			msg.obj = message;
	    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    	}
		    	else if (key.equals("acpref")) {
		    		boolean enableAccessCtrl = sharedPreferences.getBoolean("acpref", false);
		    		if (enableAccessCtrl) {
		    			if (SetupActivity.this.application.whitelist.exists() == false) {
		    				try {
								application.whitelist.touch();
								if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
									TetherService.singleton.reloadACRules();
								}
								message = getString(R.string.setup_activity_info_accesscontrol_enabled);
		    				} catch (IOException e) {
		    					message = "Unable to touch 'whitelist_mac.conf'.";
							}
		    			}
		    		}
		    		else {
		    			if (SetupActivity.this.application.whitelist.exists() == true) {
		    				application.whitelist.remove();
		    				if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
		    					TetherService.singleton.reloadACRules();
		    				}
		    				message = getString(R.string.setup_activity_info_accesscontrol_disabled);
		    			}
		    		}
		    		
		    		// Send Message
	    			Message msg = new Message();
	    			msg.obj = message;
	    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    	}
		    	else if (key.equals("encpref")) {
		    		boolean enableEncryption = sharedPreferences.getBoolean("encpref", false);
		    		if (enableEncryption != SetupActivity.this.currentEncryptionEnabled) {
			    		// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
						}
						
						SetupActivity.this.currentEncryptionEnabled = enableEncryption;
						
						// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
			   	
		    	else if (key.equals("hidessidpref")) {
		    		boolean hideSSID = sharedPreferences.getBoolean("hidessidpref", false);
		    		if (hideSSID != SetupActivity.this.currentHideSSID) {
			    		// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
						}
						
						SetupActivity.this.currentHideSSID = hideSSID;
						
						// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    			
			   			// Display TXPower-Warning
			   			if (SetupActivity.this.currentHideSSID == true) {
							LayoutInflater li = LayoutInflater.from(SetupActivity.this);
					        View view = li.inflate(R.layout.hidessidwarningview, null); 
					        new AlertDialog.Builder(SetupActivity.this)
					        .setTitle(getString(R.string.setup_activity_hidessid_warning_title))
					        .setView(view)
					        .setNeutralButton(getString(R.string.setup_activity_hidessid_warning_ok), new DialogInterface.OnClickListener() {
					                public void onClick(DialogInterface dialog, int whichButton) {
					                        Log.d(TAG, "Close pressed");
					                }
					        })
					        .show();
			   			}		    			
		    		}
		    	}
		    	else if (key.equals("tether.macspoof")) {
		    		boolean macspoofCheckbox = sharedPreferences.getBoolean("tether.macspoof", false);
		    		if (macspoofCheckbox != SetupActivity.this.currentMacSpoofEnabled) {
			    		// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
						}
						
						SetupActivity.this.currentMacSpoofEnabled = macspoofCheckbox;
						
						// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    			  			
		    		}
		    	}
			   	
			   	
		    	else if (key.equals("driverreloadpref")) {
		    		boolean driverReload = sharedPreferences.getBoolean("driverreloadpref", false);
		    		if (driverReload != SetupActivity.this.currentDriverReload) {
			    		// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
						}
						
						SetupActivity.this.currentDriverReload = driverReload;
						
						// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
			   	
			   	
		    	else if (key.equals("passphrasepref")) {
		    		String passphrase = sharedPreferences.getString("passphrasepref", SetupActivity.this.application.DEFAULT_PASSPHRASE);
		    		if (passphrase.equals(SetupActivity.this.currentPassphrase) == false) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}
		    			
						message = getString(R.string.setup_activity_info_passphrase_changedto)+" '"+passphrase+"'.";
						SetupActivity.this.currentPassphrase = passphrase;
						
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("txpowerpref")) {
		    		String transmitPower = sharedPreferences.getString("txpowerpref", "disabled");
		    		if (transmitPower.equals(SetupActivity.this.currentTransmitPower) == false) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
						}
						catch (Exception ex) {
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}
		    			
						message = getString(R.string.setup_activity_info_txpower_changedto)+" '"+transmitPower+"'.";
						SetupActivity.this.currentTransmitPower = transmitPower;
						
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    			
			   			// Display TXPower-Warning
						boolean shoTxPowerWarning = SetupActivity.this.application.settings.getBoolean("txpowerwarningpref", false);
			   			if (shoTxPowerWarning == false && transmitPower.equals("disabled") == false) {
							LayoutInflater li = LayoutInflater.from(SetupActivity.this);
					        View view = li.inflate(R.layout.txpowerwarningview, null); 
					        new AlertDialog.Builder(SetupActivity.this)
					        .setTitle(getString(R.string.setup_activity_txpower_warning_title))
					        .setView(view)
					        .setNeutralButton(getString(R.string.setup_activity_txpower_warning_ok), new DialogInterface.OnClickListener() {
					                public void onClick(DialogInterface dialog, int whichButton) {
					                        Log.d(TAG, "Close pressed");
					    		   			SetupActivity.this.application.preferenceEditor.putBoolean("txpowerwarningpref", true);
					    		   			SetupActivity.this.application.preferenceEditor.commit();
					                }
					        })
					        .show();
			   			}
		    		}
		    	}		    	
		    	else if (key.equals("lannetworkpref")) {
		    		String lannetwork = sharedPreferences.getString("lannetworkpref", SetupActivity.this.application.DEFAULT_LANNETWORK);
		    		if (lannetwork.equals(SetupActivity.this.currentLAN) == false) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
							message = getString(R.string.setup_activity_info_lan_changedto)+" '"+lannetwork+"'.";
							SetupActivity.this.currentLAN = lannetwork;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
			   	
		    	else if (key.equals("mssclampingpref")) {
		    		boolean mssclamping = sharedPreferences.getBoolean("mssclampingpref", false);;
		    		if (mssclamping != SetupActivity.this.currentMssclampingEnabled) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
							SetupActivity.this.currentMssclampingEnabled = mssclamping;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("routefixpref")) {
		    		boolean routefix = sharedPreferences.getBoolean("routefixpref", false);;
		    		if (routefix != SetupActivity.this.currentRoutefixEnabled) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
							SetupActivity.this.currentRoutefixEnabled = routefix;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("dnsprimarypref")) {
		    		String dns = sharedPreferences.getString("dnsprimarypref", "8.8.8.8");;
		    		if (dns.equals(SetupActivity.this.currentPrimaryDNS) == false) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
							SetupActivity.this.currentPrimaryDNS = dns;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}
		    	else if (key.equals("dnssecondarypref")) {
		    		String dns = sharedPreferences.getString("dnssecondarypref", "8.8.4.4");;
		    		if (dns.equals(SetupActivity.this.currentSecondaryDNS) == false) {
		    			// Restarting
						try{
							if (TetherService.singleton != null && TetherService.singleton.getState() == TetherService.STATE_RUNNING) {
								TetherService.singleton.restart();
		    				}
							SetupActivity.this.currentSecondaryDNS = dns;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}			   	
			   	
	    	Looper.loop();
			}
		}).start();
    }
 
    Handler updateSettingsMenuHandler = new Handler() {
    	public void handleMessage(Message msg) {
			updateSettingsMenu();
        	super.handleMessage(msg);
    	}
    };
    
	public String validateSSID(String newSSID) {
		String message = "";
		String validChars = "ABCDEFGHIJKLMONPQRSTUVWXYZ"
				+ "abcdefghijklmnopqrstuvwxyz" + "0123456789_.";
		for (int i = 0; i < newSSID.length(); i++) {
			if (!validChars.contains(newSSID.substring(i, i + 1))) {
				message = getString(R.string.setup_activity_error_ssid_invalidchars);
			}
		}
		if (newSSID.equals("")) {
			message = getString(R.string.setup_activity_error_ssid_empty);
		}
		if (message.length() > 0)
			message += getString(R.string.setup_activity_error_ssid_notsaved);
		return message;
	}

	public String validateIpAddress(String newIpAddress) {
		String message = "";
		final Pattern IP_PATTERN = Pattern.compile("(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])");
		if (IP_PATTERN.matcher(newIpAddress).matches() == false) {
			message = getString(R.string.setup_activity_error_ipaddress_invalid);
		}
		return message;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	SubMenu installBinaries = menu.addSubMenu(0, 0, 0, getString(R.string.setup_activity_reinstall));
    	installBinaries.setIcon(drawable.ic_menu_set_as);
    	return supRetVal;
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	boolean supRetVal = super.onOptionsItemSelected(menuItem);
    	Log.d(TAG, "Menuitem:getId  -  "+menuItem.getItemId()+" -- "+menuItem.getTitle()); 
    	if (menuItem.getItemId() == 0) {
    		this.application.installFiles();
    	}
    	return supRetVal;
    }
}
