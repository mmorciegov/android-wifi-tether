package android.tether.system;

import android.util.Log;

public class NativeTask {
    static {
        try {
            Log.i("JNI", "Trying to load libNativeTask.so");
            //System.loadLibrary("NativeTask");
            System.load("/data/data/android.tether/lib/libNativeTask.so");
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("JNI", "WARNING: Could not load libNativeTask.so");
        }
    }
    public static native String getProp(String name);
    public static native void runRootCommand(String command);
}
