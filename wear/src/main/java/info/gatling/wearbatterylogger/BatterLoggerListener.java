package info.gatling.wearbatterylogger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Here's my thought process. I needed a service on the wearable to track battery.
 * Initially was going to use IntentService, but realized I would have to get and start a new
 * Google API Client each and every time. I figured that was going to tax the battery. So I switched
 * to just a service that started by this WearableListenerService. This class's only function is
 * to start (and stop in the future) the service on command.
 */
public class BatterLoggerListener extends WearableListenerService {

    private final static String TAG = "TAVON Battery Logger Listener";
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if(messageEvent.getPath().endsWith("start")) {
            Log.v(TAG, "Starting Battery Logger Service");
            startService(new Intent(this, BatteryLoggerService.class));
        }
        else {
            Log.v(TAG, "Stopping Battery Logger Service");
            stopService(new Intent(this, BatteryLoggerService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
