package com.googlecode.android.wifi.tether.system;

import java.lang.reflect.Method;

import android.content.Context;

public class UntetherHelper {

    public static String[] getTetherableIfaces(Context context) {
        String[] tetherableIfaces = null;
        Object connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Method tetherableInterfaces = null;
        try {
        	tetherableInterfaces = connectivityManager.getClass().getMethod("getTetherableIfaces", new Class[] { });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
        	tetherableIfaces = (String[])tetherableInterfaces.invoke(connectivityManager, new Object[] {});

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tetherableIfaces;
    }
    
    public static int untetherIface(Context context, String iface) {
    	int returnCode = -1;
    	Object connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	Method untether = null;
        try {
        	untether = connectivityManager.getClass().getMethod("untether", new Class[] { String.class });
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
        	returnCode = (Integer)untether.invoke(connectivityManager, new Object[] { iface });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnCode;
    }
}
