package info.gatling.wearbatterylogger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

/**
 * Created by gimmiepepsi on 10/11/14.
 */
public class BatteryLoggerService extends Service {

    private GoogleApiClient mGoogleApiClient;
    private final static String TAG = "TAVON Battery Logger Service";

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Log.v(TAG, "Battery level changed");
            addBatteryLogItem(intent);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;    //  What's the best one for this? I kind of randomly picked START_STICKY
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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

    private void addBatteryLogItem(Intent intent){
        long time = Calendar.getInstance().getTimeInMillis();
        PutDataMapRequest request = PutDataMapRequest.create("/" + time);

        request.getDataMap().putInt(BatteryManager.EXTRA_LEVEL, intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
        request.getDataMap().putInt(BatteryManager.EXTRA_TEMPERATURE, intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
        request.getDataMap().putInt(BatteryManager.EXTRA_PLUGGED, intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

        Wearable.DataApi.putDataItem(mGoogleApiClient, request.asPutDataRequest()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

}
