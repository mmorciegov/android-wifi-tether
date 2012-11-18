package com.googlecode.android.wifi.tether.system;


import android.app.Application;
import android.util.Log;

public class HostapdSymlinks extends Application {
	//Tag
	public static final String TAG = "TETHER -> SamsungSymlinks";
    //TODO: please fix me
    public static void removeHostapdInstall() {
 			Log.d(TAG, "Remove copies of files");
 			CoreTask.runRootCommand("/system/bin/sh " + CoreTask.DATA_FILE_PATH+"/bin/removeSamsungInstall.sh");
    }
    //TODO: please fix me
    public static void initialHostapdInstall() {
    	Log.d(TAG, "Copy Files from apk");
 		CoreTask.runRootCommand("busybox mount -o rw,remount /system");
 		CoreTask.runRootCommand("busybox cp " + CoreTask.DATA_FILE_PATH+"/bin/hostapd /system/bin/hostapd_4wifitether");
		
	 	Log.d(TAG, "Running Initial Hostapd Install");
	 	CoreTask.runRootCommand("/system/bin/sh " + CoreTask.DATA_FILE_PATH+"/bin/initialSamsungInstall.sh");
    }
    //TODO: please fix me
    public static void symlinkTetherBins() {
		Log.d(TAG, "Symlink Bins to start tethering");
		CoreTask.runRootCommand("/system/bin/sh " + CoreTask.DATA_FILE_PATH+"/bin/symlinkTetherBin.sh");
    }
    //TODO: please fix me
    public static void symlinkNativeBins() {
		Log.d(TAG, "Symlink Bins to allow native tethering");
		CoreTask.runRootCommand("/system/bin/sh " + CoreTask.DATA_FILE_PATH+"/bin/symlinkNativeBin.sh");
    }
}