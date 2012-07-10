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

package android.tether;

import java.io.IOException;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Build;
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
import android.preference.PreferenceManager;
import android.tether.system.Configuration;
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
	
	public static final String MSG_TAG = "TETHER -> SetupActivity";

    private String currentSSID;
    private String currentChannel;
    private String currentPassphrase;
    private String currentLAN;
    private boolean currentEncryptionEnabled;
    private String currentTransmitPower;
    
    private EditTextPreference prefPassphrase;
    private EditTextPreference prefSSID;
    
    private static int ID_DIALOG_RESTARTING = 2;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Init Application
        this.application = (TetherApplication)this.getApplication();
        
        // Init CurrentSettings
        this.currentSSID = this.application.settings.getString("ssidpref", "AndroidTether"); 
        this.currentChannel = this.application.settings.getString("channelpref", "1");
        this.currentPassphrase = this.application.settings.getString("passphrasepref", this.application.DEFAULT_PASSPHRASE);
        this.currentLAN = this.application.settings.getString("lannetworkpref", this.application.DEFAULT_LANNETWORK);
        this.currentEncryptionEnabled = this.application.settings.getBoolean("encpref", false);
        this.currentTransmitPower = this.application.settings.getString("txpowerpref", "disabled");
        
        addPreferencesFromResource(R.layout.setupview); 
        
        // Disable Security (Access Control) if not supported
        if (!this.application.accessControlSupported) {
			PreferenceGroup securityGroup = (PreferenceGroup)findPreference("securityprefs");
			securityGroup.setEnabled(false);
        }
        
        // Disable "Transmit power" if not supported
        if (!this.application.isTransmitPowerSupported()) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	ListPreference txpowerPreference = (ListPreference)findPreference("txpowerpref");
        	wifiGroup.removePreference(txpowerPreference);
        }
        
        // Diable Bluetooth-tethering if not supported by the kernel
        if (Configuration.hasKernelFeature("CONFIG_BT_BNEP=") == false) {
        	PreferenceGroup btGroup = (PreferenceGroup)findPreference("btprefs");
        	btGroup.setEnabled(false);
        }
        else {
            // Disable "Bluetooth discoverable" if not supported
            if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ECLAIR) {
            	PreferenceGroup btGroup = (PreferenceGroup)findPreference("btprefs");
            	CheckBoxPreference btdiscoverablePreference = (CheckBoxPreference)findPreference("bluetoothdiscoverable");
            	btGroup.removePreference(btdiscoverablePreference);
            }        	
        }
        
        // Disable "encryption-setup-method"
        if (this.application.interfaceDriver.startsWith("softap") 
        		|| this.application.interfaceDriver.equals(Configuration.DRIVER_HOSTAP)) {
        	PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
        	ListPreference encsetupPreference = (ListPreference)findPreference("encsetuppref");
        	wifiGroup.removePreference(encsetupPreference);
        }
        
        // Remove Auto-Channel option if not supported by device
        if (this.application.interfaceDriver.startsWith("softap") == false
        		|| this.application.interfaceDriver.equals(Configuration.DRIVER_HOSTAP) == false) {
        	ListPreference channelpref = (ListPreference)findPreference("channelpref");
        	CharSequence[] entries = channelpref.getEntries();
        	CharSequence[] targetentries = new CharSequence[entries.length-1];
        	for (int i=1;i<entries.length;i++) {
        		targetentries[i-1] = entries[i];
        	}
        	CharSequence[] entryvalues = channelpref.getEntryValues();
        	CharSequence[] targetentryvalues = new CharSequence[entries.length-1];
        	for (int i=1;i<entryvalues.length;i++) {
        		targetentryvalues[i-1] = entryvalues[i];
        	}
        	channelpref.setEntries(targetentries);
        	channelpref.setEntryValues(targetentryvalues);
        }
        
        // SSID-Validation
        this.prefSSID = (EditTextPreference)findPreference("ssidpref");
        this.prefSSID.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
          public boolean onPreferenceChange(Preference preference,
          Object newValue) {
            String message = validateSSID(newValue.toString());
            if(!message.equals("")) {
              SetupActivity.this.application.displayToastMessage(message);
              return false;
            }
            return true;
        }});

        // Passphrase-Validation
        this.prefPassphrase = (EditTextPreference)findPreference("passphrasepref");
        final int origTextColorPassphrase = SetupActivity.this.prefPassphrase.getEditText().getCurrentTextColor();

        if (Configuration.getWifiInterfaceDriver(this.application.deviceType).startsWith("softap")
        		|| Configuration.getWifiInterfaceDriver(this.application.deviceType).equals(Configuration.DRIVER_HOSTAP)) {
        	Log.d(MSG_TAG, "Adding validators for WPA-Encryption.");
        	this.prefPassphrase.setSummary(this.prefPassphrase.getSummary()+" (WPA/WPA2-PSK)");
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
        	Log.d(MSG_TAG, "Adding validators for WEP-Encryption.");
        	this.prefPassphrase.setSummary(this.prefPassphrase.getSummary()+" (WEP 128-bit)");
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
		Boolean bluetoothOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("bluetoothon", false);
		Message msg = Message.obtain();
		msg.what = bluetoothOn ? 0 : 1;
		SetupActivity.this.setWifiPrefsEnableHandler.sendMessage(msg);
    }
	
    @Override
    protected void onResume() {
    	Log.d(MSG_TAG, "Calling onResume()");
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onPause() {
    	Log.d(MSG_TAG, "Calling onPause()");
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);   
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
        	System.gc();
        }
    };
    
   Handler displayToastMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
       		if (msg.obj != null) {
       			SetupActivity.this.application.displayToastMessage((String)msg.obj);
       		}
        	super.handleMessage(msg);
        	System.gc();
        }
    };
    
    
    private void updateConfiguration(final SharedPreferences sharedPreferences, final String key) {
    	new Thread(new Runnable(){
			public void run(){
				Looper.prepare();
			   	String message = null;
		    	if (key.equals("ssidpref")) {
		    		String newSSID = sharedPreferences.getString("ssidpref", "AndroidTether");
		    		if (SetupActivity.this.currentSSID.equals(newSSID) == false) {
	    				SetupActivity.this.currentSSID = newSSID;
	    				message = getString(R.string.setup_activity_info_ssid_changedto)+" '"+newSSID+"'.";
	    				try{
		    				if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
				    			// Show RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
		    					// Restart Tethering
				    			SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
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
		    	else if (key.equals("channelpref")) {
		    		String newChannel = sharedPreferences.getString("channelpref", "1");
		    		if (SetupActivity.this.currentChannel.equals(newChannel) == false) {
	    				SetupActivity.this.currentChannel = newChannel;
	    				message = getString(R.string.setup_activity_info_channel_changedto)+" '"+newChannel+"'.";
	    				try{
		    				if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
				    			// Show RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
				    			// Restart Tethering
		    					SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
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
						Log.e(MSG_TAG, "Can't enable/disable Wake-Lock!");
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
								application.restartSecuredWifi();
								message = getString(R.string.setup_activity_info_accesscontrol_enabled);
		    				} catch (IOException e) {
		    					message = "Unable to touch 'whitelist_mac.conf'.";
							}
		    			}
		    		}
		    		else {
		    			if (SetupActivity.this.application.whitelist.exists() == true) {
		    				application.whitelist.remove();
		    				application.restartSecuredWifi();
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
							if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
				    			// Show RestartDialog
								SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
				    			// Restart Tethering
								SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
								SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
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
		    	else if (key.equals("passphrasepref")) {
		    		String passphrase = sharedPreferences.getString("passphrasepref", SetupActivity.this.application.DEFAULT_PASSPHRASE);
		    		if (passphrase.equals(SetupActivity.this.currentPassphrase) == false) {
		    			// Restarting
						try{
							if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq") && application.wpasupplicant.exists()) {
				    			// Show RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
				    			// Restart Tethering
								SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
							}
						}
						catch (Exception ex) {
							Log.e(MSG_TAG, "Exception happend while restarting service - Here is what I know: "+ex);
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
							if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
				    			// Show RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
				    			// Restart Tethering
								SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
							}
						}
						catch (Exception ex) {
							Log.e(MSG_TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}
		    			
						message = getString(R.string.setup_activity_info_txpower_changedto)+" '"+transmitPower+"'.";
						SetupActivity.this.currentTransmitPower = transmitPower;
						
		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    			
			   			// Display Bluetooth-Warning
						boolean shoTxPowerWarning = SetupActivity.this.application.settings.getBoolean("txpowerwarningpref", false);
			   			if (shoTxPowerWarning == false && transmitPower.equals("disabled") == false) {
							LayoutInflater li = LayoutInflater.from(SetupActivity.this);
					        View view = li.inflate(R.layout.txpowerwarningview, null); 
					        new AlertDialog.Builder(SetupActivity.this)
					        .setTitle(getString(R.string.setup_activity_txpower_warning_title))
					        .setView(view)
					        .setNeutralButton(getString(R.string.setup_activity_txpower_warning_ok), new DialogInterface.OnClickListener() {
					                public void onClick(DialogInterface dialog, int whichButton) {
					                        Log.d(MSG_TAG, "Close pressed");
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
							if (application.coretask.isNatEnabled() && application.coretask.isProcessRunning("bin/dnsmasq")) {
				    			// Show RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
				    			// Restart Tethering
								SetupActivity.this.application.restartTether();
				    			// Dismiss RestartDialog
				    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
							}
							message = getString(R.string.setup_activity_info_lan_changedto)+" '"+lannetwork+"'.";
							SetupActivity.this.currentLAN = lannetwork;
						}
						catch (Exception ex) {
							message = getString(R.string.setup_activity_error_restart_tethering);
							Log.e(MSG_TAG, "Exception happend while restarting service - Here is what I know: "+ex);
						}

		    			// Send Message
		    			Message msg = new Message();
		    			msg.obj = message;
		    			SetupActivity.this.displayToastMessageHandler.sendMessage(msg);
		    		}
		    	}		    	
		    	else if (key.equals("bluetoothon")) {
		    		Boolean bluetoothOn = sharedPreferences.getBoolean("bluetoothon", false);
		    		Message msg = Message.obtain();
		    		msg.what = bluetoothOn ? 0 : 1;
		    		SetupActivity.this.setWifiPrefsEnableHandler.sendMessage(msg);
					try{
						if (application.coretask.isNatEnabled() && (application.coretask.isProcessRunning("bin/dnsmasq") || application.coretask.isProcessRunning("bin/pand"))) {
			    			// Show RestartDialog
			    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(0);
			    			
			    			// Restart Tethering
			    			SetupActivity.this.application.restartTether();

			    			// Dismiss RestartDialog
			    			SetupActivity.this.restartingDialogHandler.sendEmptyMessage(1);
						}
					}
					catch (Exception ex) {
						message = getString(R.string.setup_activity_error_restart_tethering);
					}

		   			// Display Bluetooth-Warning
					boolean showBtWarning = SetupActivity.this.application.settings.getBoolean("btwarningpref", false);
		   			if (showBtWarning == false && bluetoothOn == true) {
						LayoutInflater li = LayoutInflater.from(SetupActivity.this);
				        View view = li.inflate(R.layout.btwarningview, null); 
				        new AlertDialog.Builder(SetupActivity.this)
				        .setTitle(getString(R.string.setup_activity_bt_warning_title))
				        .setView(view)
				        .setNeutralButton(getString(R.string.setup_activity_bt_warning_ok), new DialogInterface.OnClickListener() {
				                public void onClick(DialogInterface dialog, int whichButton) {
				                        Log.d(MSG_TAG, "Close pressed");
				    		   			SetupActivity.this.application.preferenceEditor.putBoolean("btwarningpref", true);
				    		   			SetupActivity.this.application.preferenceEditor.commit();
				                }
				        })
				        .show();
		   			}
		    	}
		    	else if (key.equals("bluetoothkeepwifi")) {
		    		Boolean bluetoothWifi = sharedPreferences.getBoolean("bluetoothkeepwifi", false);
		    		if (bluetoothWifi) {
		    			SetupActivity.this.application.enableWifi();
		    		}
		    	}
		    	Looper.loop();
			}
		}).start();
    }
    
    Handler  setWifiPrefsEnableHandler = new Handler() {
    	public void handleMessage(Message msg) {
			PreferenceGroup wifiGroup = (PreferenceGroup)findPreference("wifiprefs");
			wifiGroup.setEnabled(msg.what == 1);
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
    	Log.d(MSG_TAG, "Menuitem:getId  -  "+menuItem.getItemId()+" -- "+menuItem.getTitle()); 
    	if (menuItem.getItemId() == 0) {
    		this.application.installFiles();
    	}
    	return supRetVal;
    }
}
