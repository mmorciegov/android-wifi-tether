package com.googlecode.android.wifi.tether.system;

import java.lang.reflect.Method;
import android.app.Application;
import android.os.IBinder;
import android.util.Log;

public class ServiceHandler extends Application {
	public static final String TAG = "TETHER -> ServiceHandler";
/** 
	private static IBinder getService(String service) throws Exception {
        Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
        Method getService_method = ServiceManager.getMethod("getService", new Class[]{String.class});
        IBinder b = (IBinder)getService_method.invoke(null, new Object[]{service});
        return b;
	}**/
	
	public static void setMaxClients(IBinder binder, int clients) throws Exception {
		
		// IBinder binder = getService("network_management");
        Class<?> stub = Class.forName("android.os.INetworkManagementService$Stub");
        Method asInterface_method = stub.getMethod("asInterface", new Class[]{IBinder.class});
        Object asInterface = asInterface_method.invoke(null, new Object[] {binder});
        
        Log.d(TAG,"About to run SetMaxClient " + clients);
        Method setMaxClients_method = asInterface.getClass().getMethod("setMaxClient", new Class[]{int.class});
        setMaxClients_method.invoke(asInterface, new Object[]{clients});
	}
	
	public static void wifiFirmwareReload(IBinder binder, String Adaptor, String firmwaremode) throws Exception {

		//IBinder binder = getService("network_management");
       Class<?> stub = Class.forName("android.os.INetworkManagementService$Stub");
       Method asInterface_method = stub.getMethod("asInterface", new Class[]{IBinder.class});
       Object asInterface = asInterface_method.invoke(null, new Object[] {binder});

       //setInterfaceDown
       //setInterfaceUp
       Method setFirmwareMode_method = asInterface.getClass().getMethod("wifiFirmwareReload", new Class[]{String.class, String.class});
       setFirmwareMode_method.invoke(asInterface, new Object[]{Adaptor, firmwaremode});
	}
	
}
