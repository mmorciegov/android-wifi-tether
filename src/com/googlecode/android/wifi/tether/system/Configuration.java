package com.googlecode.android.wifi.tether.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import android.util.Log;

public class Configuration {

	public static final String MSG_TAG = "TETHER -> Configuration";

	public static final int    SDK_EC             = 5;					// SDK Minimum Eclair
	public static final int    SDK_FR             = 7;					// SDK Minimum Froyo
	public static final int    SDK_GB             = 9;					// SDK Minimum Gingerbread
	public static final int    SDK_ICS            = 14;					// SDK Minimum Ice Cream Sandwich

	public static final String DEVICE_GENERIC     = "generic";			// Generic Non-ICS Profile
	public static final String DEVICE_GENERIC_ICS = "generic_ics";		// Generic ICS Profile
	
	public static final String DEVICE_BLADE       = "blade";			// ZTE Blade
	public static final String DEVICE_PASSION     = "passion";			// Google NexusOne
	
	public static final String DEVICE_SPHD700     = "SPH-D700";			// Samsung Epic 4G
	public static final String DEVICE_SCHI500     = "SCH-I500";			// Samsung Fascinate
	public static final String DEVICE_SCHI510     = "SCH-I510";			// Samsung Droid Charge
	public static final String DEVICE_SGHI897     = "SGH-I897";			// Samsung Captivate
	public static final String DEVICE_SCHR910     = "SCH-R910";			// Samsung Galaxy Indulge
	
	public static final String DEVICE_MAGURO      = "maguro";			// Samsung Galaxy Nexus (GSM)
	public static final String DEVICE_TORO        = "toro";				// Samsung Galaxy Nexus (CDMA/LTE)
	public static final String DEVICE_GTI9000     = "GT-I9000";			// Samsung Galaxy S
	public static final String DEVICE_GTI9100     = "GT-I9100";			// Samsung Galaxy S2
	public static final String DEVICE_SPHD710     = "SPH-D710";			// Samsung/Sprint Epic Touch 4G
	public static final String DEVICE_SUPERSONIC  = "supersonic";		// HTC Evo 4G (Supersonic)
	public static final String DEVICE_PYRAMID     = "pyramid";			// HTC Evo 3D

	public static final String DEVICE_VIGOR       = "vigor";			// HTC Rezound / Vigor (LTE/CDMA/GSM)

	public static final String DEVICE_THUNDERC    = "thunderc";			// LG Optimus S
	public static final String DEVICE_BRAVO       = "bravo";			// HTC Desire (GSM)
	public static final String DEVICE_BRAVOC      = "bravoc";			// HTC Desire (CDMA)
	public static final String DEVICE_MECHA       = "mecha";			// HTC Thunderbolt / Mecha (LTE/CDMA)
	
	public static final String DEVICE_SAPPHIRE    = "sapphire";			// HTC Magic
	public static final String DEVICE_DREAM       = "dream";			// HTC Dream
	public static final String DEVICE_HERO        = "hero";				// HTC Hero (GSM)
	public static final String DEVICE_HEROC       = "heroc";			// HTC Hero (CDMA)
	
	/* TI WiLink hostapd Devices */
	// Motorola OMAP3 WiLink6 Devices
	public static final String DEVICE_DROID2WE	  = "cdma_droid2we";	// Motorola Droid2 Global (DualBand)
	public static final String DEVICE_UMTSJORDAN  = "umts_jordan";		// Motorola Defy (GSM)
	public static final String DEVICE_CDMASHOLES  = "cdma_sholes";		// Motorola Droid (CDMA)
	public static final String DEVICE_UMTSSHOLES  = "umts_sholes";		// Motorola Milestone (GSM)
	public static final String DEVICE_CDMASHADOW  = "cdma_shadow";		// Motorola DroidX (CDMA)
	public static final String DEVICE_UMTSSHADOW  = "umts_shadow";		// Motorola MilestoneX (GSM)
	public static final String DEVICE_CDMADROID2  = "cdma_droid2";		// Motorola Droid2 (CDMA)
	public static final String DEVICE_UMTSDROID2  = "umts_droid2";		// Motorola Milestone2 (GSM)
	public static final String DEVICE_CDMAVENUS2  = "cdma_venus2";		// Motorola Droid Pro (CDMA)
	public static final String DEVICE_UMTSVENUS2  = "umts_venus2";		// Motorola Milestone Plus (GSM)
	// Motorola OMAP4 WiLink7 Devices
	public static final String DEVICE_EDISON      = "edison";			// Motorola Atrix 2
	public static final String DEVICE_CDMATARGA   = "cdma_targa";		// Motorola Droid Bionic
	public static final String DEVICE_CDMASOLANA  = "cdma_solana";		// Motorola Droid 3
	public static final String DEVICE_UMTSSOLANA  = "umts_solana";		// Motorola Milestone 3
	public static final String DEVICE_CDMASPYDER  = "cdma_spyder";		// Motorola Droid RAZR (CDMA/LTE)
	public static final String DEVICE_UMTSSPYDER  = "umts_spyder";		// Motorola Droid RAZR (GSM)
	// LG OMAP3 WiLink6 Devices
	public static final String DEVICE_LS855       = "ls855";			// LG Marquee
	public static final String DEVICE_LU3000      = "ls3000";			// LG Optimus Mach
	public static final String DEVICE_P970        = "p970";				// LG Optimus Black	
	public static final String DEVICE_P970G       = "p970g";			// LG Optimus Black
	// LG OMAP4 WiLink7 Devices
	public static final String DEVICE_P920        = "p920";				// LG Optimus 3D
	public static final String DEVICE_P925        = "p925";				// LG Thrill (AT&T)
	public static final String DEVICE_P925G       = "p925g";			// LG Thrill (Rogers Canada)
	// Samsung OMAP3 WiLink6 Devices
	public static final String DEVICE_GALAXYSL    = "galaxysl";			// Samsung Galaxy SL
	// Generic WiLink7	
	public static final String DEVICE_RUBY        = "ruby";				// HTC Amaze
	
	
	private String device                  = DEVICE_GENERIC;
	private int sdk                        = 0;
	
	private boolean wextSupported          = false;
	private boolean hostapdSupported       = false;
	private boolean softapSupported        = false;
	private boolean softapSamsungSupported = false;
	private boolean netdSupported          = false;
	private boolean tiadhocSupported       = false;
	private boolean autoInternalNetSetup   = false;
	
	// wext-values
	private String wextInterface           = "";
	
	// hostapd-values
	private String hostapdPath             = "";
	private String hostapdKernelModulePath = "";
	private String hostapdKernelModuleName = "";
	private String hostapdInterface        = "";
	private String hostapdTemplate         = "";
	private String hostapdLoaderCmd        = "";
	
	// netd-values
	private String netdInterface           = "";
	
	// softap-values
	private String softapInterface         = "";
	private String softapFirmwarePath      = "";
	
	private String tiadhocInterface        = "";
	
	// Encryption-type for netd and softap
	private String encryptionIdentifier    = "wep";
	private String opennetworkIdentifier   = "open";
	
	// Setup-method which should be used if "auto" is selected
	private String autoSetupMethod         = "wext";
	
	private boolean genericSetupSection    = true;
	
	private String wifiLoadCmd = "none";
	private String wifiUnloadCmd = "none";
	
	public Configuration() {
		this.device = android.os.Build.DEVICE; //NativeTask.getProp("ro.product.device");
		this.sdk = android.os.Build.VERSION.SDK_INT; //Integer.parseInt(NativeTask.getProp("ro.build.version.sdk"));
		Log.d(MSG_TAG, "Device [ Model / SDK ] *Auto_Detected*: " + this.device + " / " + this.sdk);
		this.setupDevice();
	}

	public Configuration(String device) {
		this.device = device;
		this.sdk = android.os.Build.VERSION.SDK_INT; //Integer.parseInt(NativeTask.getProp("ro.build.version.sdk"));
		Log.d(MSG_TAG, "Device [ Model / SDK ] *User_Selected*: " + this.device + " / " + this.sdk);
		this.setupDevice();
	}
	
	private void setupDevice() {
		// ZTE Blade
		if (device.equals(DEVICE_BLADE)) {
			this.setupBlade();
		}
		// Nexus One
		else if (device.equals(DEVICE_PASSION)) {
			this.setupSoftapGoogle();
		}
		// Samsung Galaxy S
		else if (device.equals(DEVICE_GTI9000)) {
			this.setupGTI9000();
		}
		// Samsung Galaxy S2/Epic Touch
		else if (device.equals(DEVICE_GTI9100) ||
				 device.equals(DEVICE_SPHD710)) {
			if (android.os.Build.VERSION.SDK_INT >= 14)  // 14 is ICS
				this.setupNetdGalaxyNexus();
			else 
				this.setupGTI9100();
		}
		// Samsung Galaxy Nexus
		else if (device.equals(DEVICE_MAGURO) ||
				 device.equals(DEVICE_TORO)) {
			this.setupNetdGalaxyNexus();
		}
		// LG Optimus S
		else if (device.equals(DEVICE_THUNDERC)) {
			this.setupThunderc();
		}
		else if (device.equals(DEVICE_BRAVOC) ||
				 device.equals(DEVICE_BRAVO) ||
				 device.equals(DEVICE_SUPERSONIC) ||
				 device.equals(DEVICE_PYRAMID) ||
				 device.equals(DEVICE_MECHA)) {
			this.setupSoftapHTC();
		}
		else if (device.equals(DEVICE_DREAM) || 
				 device.equals(DEVICE_SAPPHIRE) ||
				 device.equals(DEVICE_HERO) ||
				 device.equals(DEVICE_HEROC)) {
			this.setupTiAdhoc();
		}
		else if (device.equals(DEVICE_SGHI897) ||
				 device.equals(DEVICE_SCHI500) ||
				 device.equals(DEVICE_SCHI510) ||
				 //device.equals(DEVICE_SPHD700) ||
				 device.equals(DEVICE_SCHR910)) {
			this.setupSoftapSamsung();
		}
		// LG OMAP3 WiLink6 w/Samsung Galaxy SL
		else if (device.equals(DEVICE_LS855) ||
				 device.equals(DEVICE_LU3000) ||
				 device.equals(DEVICE_P970) ||
				 device.equals(DEVICE_P970G) ||
				 device.equals(DEVICE_GALAXYSL)) {
			this.setupHostapLGomap3();
 		}
		// LG OMAP4 WiLink7
		else if (device.equals(DEVICE_P920) ||
				 device.equals(DEVICE_P925) ||
				 device.equals(DEVICE_P925G)) {
			this.setupHostapLGomap4();
		}
		// Motorola OMAP3 WiLink6 Devices
		else if (device.equals(DEVICE_DROID2WE) ||
				 device.equals(DEVICE_UMTSSHOLES) ||
				 device.equals(DEVICE_UMTSJORDAN) ||
				 device.equals(DEVICE_CDMASHADOW) ||
				 device.equals(DEVICE_UMTSSHADOW) ||
				 device.equals(DEVICE_CDMADROID2) ||
				 device.equals(DEVICE_UMTSDROID2) ||
				 device.equals(DEVICE_CDMAVENUS2) ||
				 device.equals(DEVICE_UMTSVENUS2)) {
			this.setupHostapMotOMAP3();
		}
		// Motorola OMAP4 WiLink7 Devices
		else if (device.equals(DEVICE_EDISON) ||
				 device.equals(DEVICE_CDMATARGA) ||
				 device.equals(DEVICE_CDMASOLANA) ||
				 device.equals(DEVICE_UMTSSOLANA) ||
				 device.equals(DEVICE_CDMASPYDER) ||
				 device.equals(DEVICE_UMTSSPYDER)) {
			this.setupHostapMotOMAP4();
		}
		// Generic WiLink
 		else if (device.equals(DEVICE_RUBY)) {
			this.setupHostapGenWiLink7();
 		}
 		else if (device.equals(DEVICE_GENERIC_ICS)) {
 			this.setupNetdGalaxyNexus();
 		}
 		else if (device.equals(DEVICE_VIGOR)) {
 			this.setupNetdHTCRezound();
 		}
		else {
			// HTC Generic
			if ((new File("/vendor/firmware/fw_bcm4329_apsta.bin").exists() || 
					new File("/etc/firmware/fw_bcm4329_apsta.bin").exists()) &&
				 new File("/system/lib/modules/bcm4329.ko").exists()) {
				this.setupSoftapHTC();
			}
			// Motorola OMAP3 WiLink6 Catcher
			else if ((new File("/system/bin/Hostapd")).exists() == true &&
					(new File("/system/bin/wlan_loader")).exists() == true &&
					(new File("/system/etc/wifi/fw_tiwlan_ap.bin")).exists() == true &&
					(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
				this.setupHostapMotOMAP3();
			}
			// Motorola OMAP4 WiLink7 Catcher
			else if ((new File("/system/bin/Hostapd")).exists() == true &&
					(new File("/system/bin/tiap_loader")).exists() == true &&
					(new File("/system/etc/wifi/fw_wlan1281_AP.bin")).exists() == true &&
					(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
				this.setupHostapMotOMAP4();
			}
			// LG OMAP3 WiLink6 Catcher
			else if ((new File("/system/bin/hostapd")).exists() == true &&
					(new File("/system/bin/tiap_loader")).exists() == true &&
					(new File("/system/etc/wifi/softap/firmware_ap.bin")).exists() == true &&
					(new File("/system/etc/wifi/softap/tiwlan_ap.ini")).exists() == true) {
				this.setupHostapLGomap3();
			}
			// LG OMAP4 WiLink7 Catcher
			else if ((new File("/system/bin/hostap")).exists() == true &&
					(new File("/system/bin/tiap_loader")).exists() == true &&
					(new File("/system/etc/wifi/softap/firmware_ap.bin")).exists() == true &&
					(new File("/system/etc/wifi/softap/tiwlan_ap.ini")).exists() == true) {
				this.setupHostapLGomap4();
			}
			// Generic WiLink Catcher
			else if ((new File("/system/bin/hostapd")).exists() == true &&
					(new File("/system/bin/tiap_loader")).exists() == true &&
					(new File("/system/etc/wifi/firmware_ap.bin")).exists() == true &&
					(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
				this.setupHostapGenWiLink7();
			}
			else {
				this.setupGeneric();
			}
		}
	}
	
	/**
	 * TI-ADHOC - used for sapphire, dream, hero, heroc
	 */
	private void setupTiAdhoc() {
		
		this.wextSupported          = false;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = false;
		this.tiadhocSupported       = true;
		this.tiadhocInterface       = "tiwlan0";
		this.genericSetupSection    = true;
		this.autoSetupMethod        = "tiwlan0";
	}	
	
	/**
	 * ZTE BLADE
	 */
	private void setupBlade() {
		
		this.wextSupported        = true;
		this.softapSupported      = false;
		this.softapSamsungSupported = false;
		this.netdSupported        = true;
		this.tiadhocSupported     = false;
		
		this.wextInterface = "wlan0";
		
		// hostapd support
		if ((new File("/system/bin/hostapd")).exists() == true) {
			this.hostapdSupported   = true;
			this.hostapdPath        = "/system/bin/hostapd";
			this.hostapdInterface   = "wlan0";
			this.hostapdTemplate    = "mini";
			this.autoSetupMethod    = "hostapd";
			this.hostapdLoaderCmd   = "disabled";
		}
		else {
			this.hostapdSupported = false;
			this.autoSetupMethod = "wext";
		}
		this.netdInterface         = "wlap0";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/wifi/ar6000.ko";
		this.hostapdKernelModuleName = "ar6000";
		
		this.genericSetupSection = true;
	}

	/**
	 * Passion aka Nexus One
	 */
	private void setupSoftapGoogle() {

		this.wextSupported			= true;
		this.softapSupported		= true;
		this.softapSamsungSupported	= false;
		this.netdSupported			= true;
		this.hostapdSupported		= false;
		this.tiadhocSupported		= false;
		this.autoInternalNetSetup	= true;
		
		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		if (new File("/vendor/firmware/fw_bcm4329_apsta.bin").exists()) {
			this.softapFirmwarePath = "/vendor/firmware/fw_bcm4329_apsta.bin";
		}
		else if (new File("/etc/firmware/fw_bcm4329_apsta.bin").exists()) {
			this.softapFirmwarePath = "/etc/firmware/fw_bcm4329_apsta.bin";
		}
		
		this.autoSetupMethod = "softap";
		this.genericSetupSection = true;
	}

	/**
	 * HTC Rezound
	 */
	private void setupNetdHTCRezound() {
		this.wextSupported          = true;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = true;
		this.hostapdSupported       = false;
		this.tiadhocSupported       = false;
		this.autoInternalNetSetup	= true;
		
		if (sdk >= SDK_ICS) {
			// Vigor on ICS
			//ICS
			this.wextInterface = "wlan0";
			this.netdInterface = "wlan0";
			this.softapInterface = "wlan0";
			if ((new File("/etc/firmware/fw_bcm4330_apsta_b1.bin")).exists() == true &&
					(new File("/system/lib/modules/bcmdhd.ko")).exists() == true) {
				this.softapFirmwarePath = "/etc/firmware/fw_bcm4330_apsta_b1.bin";
			}
		//} else if (sdk >= SDK_GB) {
		//}
		} else {
			// Vigor on GB or who knows lets try GB anyway.... There was never a stock less than GB for Vigor
			this.wextInterface = "eth0";
			this.netdInterface = "eth0";
			this.softapInterface = "wlan0";
			if ((new File("/etc/firmware/fw_bcm4329_apsta.bin")).exists() == true &&
					(new File("/system/lib/modules/bcm4329.ko")).exists() == true) {
				this.softapFirmwarePath = "/etc/firmware/fw_bcm4329_apsta.bin";
			}
		}

		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		//this.autoSetupMethod = "softap";
		this.autoSetupMethod = "netd";
		this.genericSetupSection = true;

	}

	/**
	 * Bravo - HTC Desire (GSM), Bravoc - HTC Desire (CDMA)
	 */
	private void setupSoftapHTC() {

		this.wextSupported        = true;
		this.softapSupported      = true;
		this.softapSamsungSupported = false;
		this.netdSupported        = false;
		this.hostapdSupported     = false;
		this.tiadhocSupported      = false;
		
		this.wextInterface = "eth0";

		this.netdInterface = "eth0";
		this.softapInterface = "eth0";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		if (new File("/vendor/firmware/fw_bcm4329_apsta.bin").exists()) {
			this.softapFirmwarePath = "/vendor/firmware/fw_bcm4329_apsta.bin";
		}
		else if (new File("/etc/firmware/fw_bcm4329_apsta.bin").exists()) {
			this.softapFirmwarePath = "/etc/firmware/fw_bcm4329_apsta.bin";
		}
		
		this.autoSetupMethod = "softap";
		this.genericSetupSection = true;
	}

	/**
	 * HTC Amaze
	 */
	private void setupHostapGenWiLink7() {
		this.wextSupported        	= false;
		this.softapSupported      	= false;
		this.softapSamsungSupported = false;
		this.netdSupported        	= false;
		this.tiadhocSupported      	= false;
		
		this.wextInterface = "tiwlan0";
		
		// hostapd support
		if ((new File("/system/bin/hostapd")).exists() == true &&
			(new File("/system/bin/tiap_loader")).exists() == true &&
			(new File("/system/etc/wifi/firmware_ap.bin")).exists() == true &&
			(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
			this.hostapdSupported = true;
			this.hostapdPath      = "/system/bin/hostapd";
			this.hostapdInterface = "tiap0";
			//this.hostapdTemplate  = "tiap"; TODO
			this.hostapdTemplate  = "droi";
			this.autoSetupMethod  = "hostapd";
			this.hostapdLoaderCmd = "/system/bin/tiap_loader tiap0 -f /system/etc/wifi/firmware_ap.bin -e /proc/calibration -i /system/etc/wifi/tiwlan_ap.ini";
		}
		
		this.netdInterface         = "";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/lib/modules/tiap_drv.ko";
		this.hostapdKernelModuleName = "tiap_drv";
		
		this.genericSetupSection = true;
	}		
	
	/**
	 * Motorola DroidX (CDMA), Droid2 (CDMA)
	 */
	private void setupHostapMotOMAP3() {
		this.wextSupported          = true;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "tiwlan0";
		
		// hostapd support
		if ((new File("/system/bin/Hostapd")).exists() == true &&
			(new File("/system/bin/wlan_loader")).exists() == true &&
			(new File("/system/etc/wifi/fw_tiwlan_ap.bin")).exists() == true &&
			(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
			this.hostapdSupported = true;
			this.hostapdPath      = "/system/bin/Hostapd";
			this.hostapdInterface = "tiwlan0";
			this.hostapdTemplate  = "droi";
			this.autoSetupMethod  = "hostapd";
			this.hostapdLoaderCmd = "/system/bin/wlan_loader -f /system/etc/wifi/fw_tiwlan_ap.bin -i /system/etc/wifi/tiwlan_ap.ini -e /pds/wifi/nvs_map.bin";
		}
		else {
			this.hostapdSupported = false;
			this.autoSetupMethod = "wext";
		}
		
		this.netdInterface         = "tiwlan0";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/lib/modules/tiap_drv.ko";
		this.hostapdKernelModuleName = "tiap_drv";
		
		this.genericSetupSection = true;
	}

	/**
	 * Motorola Targa
	 */
	private void setupHostapMotOMAP4() {
		this.wextSupported        	= false;
		this.softapSupported      	= false;
		this.softapSamsungSupported = false;
		this.netdSupported        	= false;
		this.tiadhocSupported      	= false;
		
		this.wextInterface = "tiwlan0";
		
		// hostapd support
		if ((new File("/system/bin/Hostapd")).exists() == true &&
			(new File("/system/bin/tiap_loader")).exists() == true &&
			(new File("/system/etc/wifi/fw_wlan1281_AP.bin")).exists() == true &&
			(new File("/system/etc/wifi/tiwlan_ap.ini")).exists() == true) {
			this.hostapdSupported = true;
			this.hostapdPath      = "/system/bin/Hostapd";
			this.hostapdInterface = "tiap0";
			this.hostapdTemplate  = "droi";
			this.autoSetupMethod = "hostapd";
			this.hostapdLoaderCmd = "/system/bin/tiap_loader -f /system/etc/wifi/fw_wlan1281_AP.bin -i /system/etc/wifi/tiwlan_ap.ini -e /pds/wifi/nvs_map.bin";
		}
		else {
			this.hostapdSupported = false;
			this.autoSetupMethod = "wext";
		}		
		this.netdInterface         = "tiap0";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/lib/modules/tiap_drv.ko";
		this.hostapdKernelModuleName = "tiap_drv";
		
		this.genericSetupSection = true;
	}	
	
	/**
	 * Samsung Generic
	 */
	private void setupSoftapSamsung() {
		this.wextSupported        = true;
		this.softapSupported      = false;
		this.softapSamsungSupported = false;
		this.netdSupported        = false;
		this.hostapdSupported     = false;
		this.tiadhocSupported      = false;
		
		this.autoSetupMethod      = "wext";
		
		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wep";
		this.opennetworkIdentifier = "open";
		
		if (new File("/etc/wifi/bcm4329_aps.bin").exists()) {
			this.softapFirmwarePath = "/etc/wifi/bcm4329_aps.bin";
			this.autoSetupMethod = "softap_samsung";
			this.softapSamsungSupported = true;
			this.netdSupported = false;
			this.encryptionIdentifier = "wpa2-psk";
		}
		this.genericSetupSection = true;
	}

	/**
	 * Samsung Galaxy Nexus
	 */
	private void setupNetdGalaxyNexus() {
		this.wextSupported          = true;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = true;
		this.hostapdSupported       = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "wlan0";

		this.netdInterface = "wlan0";
		this.softapInterface = "wlan0";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		this.softapFirmwarePath = ""; //"/system/vendor/firmware/fw_bcmdhd_apsta.bin";
		
		this.autoSetupMethod = "netd";
		this.genericSetupSection = true;
	}

	/**
	 * Samsung Galaxy S
	 *//*
	private void setupNetdSamsung() {
		this.wextSupported          = true;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = true;
		this.hostapdSupported       = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		this.softapFirmwarePath = "/etc/wifi/bcm4329_aps.bin";
		
		this.autoSetupMethod = "netd";
		this.genericSetupSection = true;
	} */
	
	/**
	 * Samsung Galaxy S
	 */
	private void setupGTI9000() {
		this.wextSupported          = true;
		this.softapSupported        = true;
		this.softapSamsungSupported = false;
		this.netdSupported          = true;
		this.hostapdSupported       = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		this.softapFirmwarePath = "/etc/wifi/bcm4329_aps.bin";

		this.wifiLoadCmd = "none";
		this.wifiUnloadCmd = "/system/bin/mfgloader -u;/system/bin/rmmod dhd";
		
		this.autoSetupMethod = "softap";
		this.genericSetupSection = true;
	}

	/**
	 * Samsung Galaxy S2 SoftAP
	 */
	private void setupGTI9100() {
		this.wextSupported          = true;
		this.softapSupported        = true;
		this.softapSamsungSupported = false;
		this.netdSupported          = true;
		this.hostapdSupported       = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";
		
		if (new File("/system/vendor/firmware/bcm4330_aps.bin").exists()) {
			this.softapFirmwarePath = "/system/vendor/firmware/bcm4330_aps.bin";
		}
		else if (new File("/system/etc/wifi/bcm4330_aps.bin").exists()) {
			this.softapFirmwarePath = "/system/etc/wifi/bcm4330_aps.bin";
		}
		else if (new File("/etc/wifi/bcm4330_aps.bin").exists()) {
			this.softapFirmwarePath = "/etc/wifi/bcm4330_aps.bin";
		}
		
		//this.wifiLoadCmd = "/system/bin/insmod /lib/modules/dhd.ko firmware_path=/system/etc/wifi/bcm4330_aps.bin nvram_path=/system/etc/wifi/nvram_net.txt";
		//this.wifiUnloadCmd = "/system/bin/rmmod dhd";
		
		this.wifiLoadCmd = "none";
		this.wifiUnloadCmd = "/system/bin/mfgloader -u;/system/bin/rmmod dhd";
		
		this.autoSetupMethod = "netd";
		this.genericSetupSection = true;
	}

	/**
	 * LG Optimus S - thunderc
	 */
	private void setupThunderc() {

		this.wextSupported        = true;
		this.softapSupported      = true;
		this.softapSamsungSupported = false;
		this.netdSupported        = true;
		this.hostapdSupported     = false;
		this.tiadhocSupported      = false;

		this.wextInterface = "eth0";

		this.netdInterface = "wl0.1";
		this.softapInterface = "wl0.1";
		this.encryptionIdentifier = "wpa2-psk";
		this.opennetworkIdentifier = "open";

		if (new File("/etc/wl/rtecdc-apsta.bin").exists()) {
			this.softapFirmwarePath = "/etc/wl/rtecdc-apsta.bin";
			this.autoSetupMethod = "softap";
			this.softapSupported = true;
			this.netdSupported = true;
		} else {
			this.autoSetupMethod = "wext";
			this.softapSupported = false;
			this.netdSupported = false;
		}
		this.genericSetupSection = true;
	}

	/**
	 * LG OMAP3 WiLink6
	 */
	private void setupHostapLGomap3() {
		this.wextSupported          = false;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "tiwlan0";
		
		// hostapd support
		if ((new File("/system/bin/hostapd")).exists() == true &&
			(new File("/system/bin/tiap_loader")).exists() == true &&
			(new File("/system/etc/wifi/softap/firmware_ap.bin")).exists() == true &&
			(new File("/system/etc/wifi/softap/tiwlan_ap.ini")).exists() == true) {
			this.hostapdSupported = true;
			this.hostapdPath      = "/system/bin/hostap";
			this.hostapdInterface = "tiap0";
			this.hostapdTemplate  = "droi";
			this.autoSetupMethod  = "hostapd";
			this.hostapdLoaderCmd = "/system/bin/tiap_loader -f /system/etc/wifi/softap/firmware_ap.bin -i /system/etc/wifi/softap/tiwlan_ap.ini -e /data/misc/wifi/nvs_map.bin";		
		}
		else {
			this.hostapdSupported = false;
			this.autoSetupMethod = "wext";
		}
		
		this.netdInterface         = "tiap0";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/etc/wifi/softap/tiap_drv.ko";
		this.hostapdKernelModuleName = "tiap_drv";
		
		this.genericSetupSection = true;
	}
	
	/**
	 * LG OMAP4 WiLink7
	 */
	private void setupHostapLGomap4() {
		this.wextSupported          = false;
		this.softapSupported        = false;
		this.softapSamsungSupported = false;
		this.netdSupported          = false;
		this.tiadhocSupported       = false;
		
		this.wextInterface = "tiwlan0";
		
		// hostapd support
		if ((new File("/system/bin/hostap")).exists() == true &&
			(new File("/system/bin/tiap_loader")).exists() == true &&
			(new File("/system/etc/wifi/softap/firmware_ap.bin")).exists() == true &&
			(new File("/system/etc/wifi/softap/tiwlan_ap.ini")).exists() == true) {
			this.hostapdSupported = true;
			this.hostapdPath      = "/system/bin/hostap";
			this.hostapdInterface = "tiap0";
			this.hostapdTemplate  = "droi";
			this.autoSetupMethod  = "hostapd";
			this.hostapdLoaderCmd = "/system/bin/tiap_loader -f /system/etc/wifi/softap/firmware_ap.bin -i /system/etc/wifi/softap/tiwlan_ap.ini -e /data/misc/wifi/nvs_map.bin";
		}
		else {
			this.hostapdSupported = false;
			this.autoSetupMethod = "wext";
		}
		
		this.netdInterface         = "tiap0";
		this.encryptionIdentifier  = "wpa";
		this.opennetworkIdentifier = "open";
		
		// Kernel-Module
		this.hostapdKernelModulePath = "/system/etc/wifi/softap/tiap_drv.ko";
		this.hostapdKernelModuleName = "tiap_drv";
		
		this.genericSetupSection = true;
	}	
	
	/**
	 * GENERIC
	 */
	private void setupGeneric() {
		this.wextSupported        = true;
		this.hostapdSupported     = false;
		this.softapSupported      = false;
		this.netdSupported        = false;
		this.tiadhocSupported      = false;
		
		this.autoSetupMethod      = "wext";
		this.encryptionIdentifier = "wep";
		this.genericSetupSection  = true;
	}
	
	// ===========================================================================	
	public String getDevice() {
		return device;
	}

	public boolean isTiadhocSupported() {
		return tiadhocSupported;
	}

	public String getTiadhocInterface() {
		return tiadhocInterface;
	}

	public boolean isWextSupported() {
		return wextSupported;
	}

	public boolean isHostapdSupported() {
		return hostapdSupported;
	}

	public boolean isSoftapSupported() {
		return softapSupported;
	}

	public boolean isSoftapSamsungSupported() {
		return softapSamsungSupported;
	}

	public boolean isNetdSupported() {
		return netdSupported;
	}
	
	public String getWextInterface() {
		return wextInterface;
	}

	public String getHostapdPath() {
		return hostapdPath;
	}

	public String getHostapdTemplate() {
		return hostapdTemplate;
	}

	public synchronized String getHostapdKernelModuleName() {
		return hostapdKernelModuleName;
	}

	public String getHostapdKernelModulePath() {
		return hostapdKernelModulePath;
	}

	public String getHostapdInterface() {
		return hostapdInterface;
	}

	public String getNetdInterface() {
		return netdInterface;
	}

	public String getSoftapInterface() {
		return softapInterface;
	}	
	
	public String getEncryptionIdentifier() {
		return encryptionIdentifier;
	}
	
	public String getOpennetworkIdentifier() {
		return opennetworkIdentifier;
	}	
	
	public boolean isAutoInternalNetSetup() {
		return autoInternalNetSetup;
	}

	public void setAutoInternalConfig(boolean autoInternalConfig) {
		this.autoInternalNetSetup = autoInternalConfig;
	}

	public boolean isGenericSetupSection() {
		return genericSetupSection;
	}

	public String getSoftapFirmwarePath() {
		return softapFirmwarePath;
	}

	public String getAutoSetupMethod() {
		return autoSetupMethod;
	}
	
    public String getHostapdLoaderCmd() {
		return hostapdLoaderCmd;
	}

	public String getWifiLoadCmd() {
		return wifiLoadCmd;
	}

	public void setWifiLoadCmd(String wifiLoadCmd) {
		this.wifiLoadCmd = wifiLoadCmd;
	}

	public String getWifiUnloadCmd() {
		return wifiUnloadCmd;
	}

	public void setWifiUnloadCmd(String wifiUnloadCmd) {
		this.wifiUnloadCmd = wifiUnloadCmd;
	}

	public static boolean hasKernelFeature(String feature) {
    	try {
			File cfg = new File("/proc/config.gz");
			if (cfg.exists() == false) {
				return true;
			}
			FileInputStream fis = new FileInputStream(cfg);
			GZIPInputStream gzin = new GZIPInputStream(fis);
			BufferedReader in = null;
			String line = "";
			in = new BufferedReader(new InputStreamReader(gzin));
			while ((line = in.readLine()) != null) {
				   if (line.startsWith(feature)) {
					    gzin.close();
						return true;
					}
			}
			gzin.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	return false;
    }
}
