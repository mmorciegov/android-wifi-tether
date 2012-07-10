package com.googlecode.android.wifi.tether;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

public class TetherWidget extends AppWidgetProvider {
    static final String TAG = "TetherWidget";

    static final ComponentName THIS_APPWIDGET = new ComponentName("com.googlecode.android.wifi.tether", "com.googlecode.android.wifi.tether.TetherWidget");

    // If the widget gets more botton we will define it here
    private static final int BUTTON_TETHER = 0;

    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;

    private static final StateTracker tetherState = new TetherStateTracker();
   
    /**
     * The state machine for Tether-toggling, tracking
     * reality versus the user's intent.
     *
     * This is necessary because reality moves relatively slowly
     * (turning on &amp; off radio drivers), compared to user's
     * expectations.
     */
    private abstract static class StateTracker {
        
    	// Is the state in the process of changing?
        private boolean inTransition = false;
        private Boolean actualState = null;  // initially not set
        private Boolean intendedState = null;  // initially not set

        // Did a toggle request arrive while a state update was
        // already in-flight?  If so, the mIntendedState needs to be
        // requested when the other one is done, unless we happened to
        // arrive at that state already.
        private boolean deferredStateChangeRequestNeeded = false;

        /**
         * User pressed a button to change the state.  Something
         * should immediately appear to the user afterwards, even if
         * we effectively do nothing.  Their press must be heard.
         */
        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            boolean newState = false;
            switch (currentState) {
                case STATE_ENABLED:
                    newState = false;
                    break;
                case STATE_DISABLED:
                    newState = true;
                    break;
                case STATE_INTERMEDIATE:
                    if (intendedState != null) {
                        newState = !intendedState;
                    }
                    break;
            }
            intendedState = newState;
            if (inTransition) {
                // We don't send off a transition request if we're
                // already transitioning.  Makes our state tracking
                // easier, and is probably nicer on lower levels.
                // (even though they should be able to take it...)
                deferredStateChangeRequestNeeded = true;
            } else {
                inTransition = true;
                requestStateChange(context, newState);
            }
        }

        /**
         * Update internal state from a broadcast state change.
         */
        public abstract void onActualStateChange(Context context, Intent intent);

        /**
         * Sets the value that we're now in.  To be called from onActualStateChange.
         *
         * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
         *                 STATE_TURNING_OFF, STATE_UNKNOWN
         */
        protected final void setCurrentState(Context context, int newState) {
            final boolean wasInTransition = inTransition;
            switch (newState) {
                case STATE_DISABLED:
                    inTransition = false;
                    actualState = false;
                    break;
                case STATE_ENABLED:
                    inTransition = false;
                    actualState = true;
                    break;
                case STATE_TURNING_ON:
                    inTransition = true;
                    actualState = false;
                    break;
                case STATE_TURNING_OFF:
                    inTransition = true;
                    actualState = true;
                    break;
            }

            if (wasInTransition && !inTransition) {
                if (deferredStateChangeRequestNeeded) {
                    Log.v(TAG, "processing deferred state change");
                    if (actualState != null && intendedState != null &&
                        intendedState.equals(actualState)) {
                        Log.v(TAG, "... but intended state matches, so no changes.");
                    } else if (intendedState != null) {
                        inTransition = true;
                        requestStateChange(context, intendedState);
                    }
                    deferredStateChangeRequestNeeded = false;
                }
            }
        }


        /**
         * If we're in a transition mode, this returns true if we're
         * transitioning towards being enabled.
         */
        public final boolean isTurningOn() {
            return intendedState != null && intendedState;
        }

        /**
         * Returns simplified 3-state value from underlying 5-state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
         */
        public final int getTriState(Context context) {
            if (inTransition) {
                // If we know we just got a toggle request recently
                // (which set mInTransition), don't even ask the
                // underlying interface for its state.  We know we're
                // changing.  This avoids blocking the UI thread
                // during UI refresh post-toggle if the underlying
                // service state accessor has coarse locking on its
                // state (to be fixed separately).
                return STATE_INTERMEDIATE;
            }
            switch (getActualState(context)) {
                case STATE_DISABLED:
                    return STATE_DISABLED;
                case STATE_ENABLED:
                    return STATE_ENABLED;
                default:
                    return STATE_INTERMEDIATE;
            }
        }

        /**
         * Gets underlying actual state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
         *         or or STATE_UNKNOWN.
         */
        public abstract int getActualState(Context context);

        /**
         * Actually make the desired change to the underlying radio
         * API.
         */
        protected abstract void requestStateChange(Context context, boolean desiredState);
    }

    /**
     * Subclass of StateTracker to get/set Tether state.
     */
    private static final class TetherStateTracker extends StateTracker {
    	@Override
        public int getActualState(Context context) {
            TetherService tetherService = TetherService.singleton;
            if (tetherService != null) {
            	return wifiStateToFiveState(tetherService.getState());
            }
            return wifiStateToFiveState(TetherService.STATE_IDLE);
        }

        @Override
        protected void requestStateChange(final Context context, final boolean desiredState) {
            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                	if (desiredState == true) {
        				// Sending intent to TetherServiceReceiver that we want to start the service-now
        		    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
        		    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
        		    	intent.putExtra("state", TetherService.SERVICE_START);
        		    	context.sendBroadcast(intent);
                	}
                	
                	if (desiredState == false) {
        				// Sending intent to TetherServiceReceiver that we want to start the service-now
        		    	Intent intent = new Intent(TetherService.SERVICEMANAGE_INTENT);
        		    	intent.setAction(TetherService.SERVICEMANAGE_INTENT);
        		    	intent.putExtra("state", TetherService.SERVICE_STOP);
        		    	context.sendBroadcast(intent);
                	}
                	return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            int tetherState = intent.getIntExtra("state", TetherService.STATE_IDLE);
            setCurrentState(context, wifiStateToFiveState(tetherState));
        }

        /**
         * Converts Tether-state values into our
         * common state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case TetherService.STATE_IDLE:
                    return STATE_DISABLED;
                case TetherService.STATE_RUNNING:
                    return STATE_ENABLED;
                case TetherService.STATE_STOPPING:
                    return STATE_TURNING_OFF;
                case TetherService.STATE_STARTING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
    	// Update each requested appWidgetId
        RemoteViews view = buildUpdate(context, -1);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }

    @Override
    public void onEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.googlecode.android.wifi.tether", ".TetherWidget"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }


	@Override
    public void onDisabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.googlecode.android.wifi.tether", ".TetherWidget"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(context, appWidgetId, BUTTON_TETHER));
        updateButtons(views, context);
        return views;
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context, -1);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
    }

    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
        switch (tetherState.getTriState(context)) {
            case STATE_DISABLED:
                views.setImageViewResource(R.id.img_wifi,
                                           R.drawable.tether_wifi_off);
                views.setImageViewResource(R.id.ind_wifi,
                                           R.drawable.appwidget_ind_off_c);
                break;
            case STATE_ENABLED:
                views.setImageViewResource(R.id.img_wifi,
                                           R.drawable.tether_wifi_on);
                views.setImageViewResource(R.id.ind_wifi,
                                           R.drawable.appwidget_ind_on_c);
                break;
            case STATE_INTERMEDIATE:
                // In the transitional state, the bottom green bar
                // shows the tri-state (on, off, transitioning), but
                // the top dark-gray-or-bright-white logo shows the
                // user's intent.  This is much easier to see in
                // sunlight.
                if (tetherState.isTurningOn()) {
                    views.setImageViewResource(R.id.img_wifi,
                                               R.drawable.tether_wifi_off);
                    views.setImageViewResource(R.id.ind_wifi,
                                               R.drawable.appwidget_ind_mid_c);
                } else {
                    views.setImageViewResource(R.id.img_wifi,
                                               R.drawable.tether_wifi_on);
                    views.setImageViewResource(R.id.ind_wifi,
                                               R.drawable.appwidget_ind_off_c);
                }
                break;
        }
    }

    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId, int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, TetherWidget.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
                launchIntent, 0 /* no flags */);
        return pi;
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == BUTTON_TETHER) {
                tetherState.toggleState(context);
            }
        } else if(intent.getAction().equals(TetherService.STATECHANGED_INTENT)) {
        	tetherState.onActualStateChange(context, intent);
    	} else {
            // Don't fall-through to updating the widget.  The Intent
            // was something unrelated or that our super class took
            // care of.
            return;
        }

        // State changes fall through
        updateWidget(context);
    }
}
