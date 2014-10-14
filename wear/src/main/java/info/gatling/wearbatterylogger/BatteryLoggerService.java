package info.gatling.wearbatterylogger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

/**
 * Created by gimmiepepsi on 10/11/14.
 */
public class BatteryLoggerService extends Service implements MessageApi.MessageListener, DataApi.DataListener {

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
        return START_STICKY;
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
        final Long time = Calendar.getInstance().getTimeInMillis();
        final Integer battery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                Wearable.DataApi.getDataItem(mGoogleApiClient,getUriForDataItem()).
                        setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                              @Override
                                              public void onResult (DataApi.DataItemResult dataItemResult){
                                                  PutDataMapRequest request;
//                                                  DataMap dataMap;

                                                  if (dataItemResult == null) {
//                                                      Log.v(TAG, "Result was null, so creating");
                                                      request = PutDataMapRequest.create("/battery");
//                                                      dataMap = request.getDataMap();

                                                  } else {
//                                                      Log.v(TAG, "Result was there, so get");
                                                      request = PutDataMapRequest.createFromDataMapItem(DataMapItem.fromDataItem(dataItemResult.getDataItem()));
//                                                      dataMap = DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap();
                                                  }

                                                  request.getDataMap().putInt(time.toString(), battery);
                                                  Log.v(TAG, "Time: " + time + " | Battery: " + battery);
                                                  Wearable.DataApi.putDataItem(mGoogleApiClient, request.asPutDataRequest()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                                      @Override
                                                      public void onResult(DataApi.DataItemResult dataItemResult) {

                                                      }
                                                  });
                                              }
                                          }

                        );
                return null;
            }
        }.execute();

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    private Uri getUriForDataItem() {
        // If you've put data on the local node
        String nodeId = getLocalNodeId();
        // Or if you've put data on the remote node
        // String nodeId = getRemoteNodeId();
        // Or If you already know the node id
        // String nodeId = "some_node_id";
        return new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path("/battery").build();
    }

    private String getLocalNodeId() {
        NodeApi.GetLocalNodeResult nodeResult = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        return nodeResult.getNode().getId();
    }

//    private PutDataMapRequest getOrCreatePutDataMapRequest(DataApi.DataItemResult result, String path){
//
//    }





}
