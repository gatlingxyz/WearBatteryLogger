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
 * Created by gimmiepepsi on 10/11/14.
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
        Log.v(TAG, "Starting Battery Logger Service");
        if(messageEvent.getPath().endsWith("start")) {
            startService(new Intent(this, BatteryLoggerService.class));
        }
        else {
            stopService(new Intent(this, BatteryLoggerService.class));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
