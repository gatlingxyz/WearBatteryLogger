package info.gatling.wearbatterylogger;

import android.app.ListActivity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by gimmiepepsi on 10/11/14.
 */
public class ViewBatteryLogActivity extends ListActivity implements DataApi.DataListener {

    private GoogleApiClient mGoogleApiClient;
    private final static String TAG = "TAVON Battery Logger";

    private enum ServiceCommand{
        START("/start"), STOP("/stop");

        public String command;

        private ServiceCommand(String command){
            this.command = command;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        Wearable.DataApi.addListener(mGoogleApiClient, ViewBatteryLogActivity.this);
                        Toast.makeText(ViewBatteryLogActivity.this, "Connected to wearable", Toast.LENGTH_SHORT).show();

                        talkToWearableService(ServiceCommand.START);
                        init();
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

    private void init(){

        new AsyncTask<Void, Void, DataMap>(){

            @Override
            protected DataMap doInBackground(Void... params) {

                DataApi.DataItemResult dataItemResult = Wearable.DataApi.getDataItem(mGoogleApiClient, getUriForDataItem()).await();
                if(dataItemResult != null && dataItemResult.getDataItem() != null) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap();
                    return dataMap;
                }

                return null;
            }

            @Override
            protected void onPostExecute(DataMap dataMap) {
                if(dataMap != null) {
                    setListAdapter(new BatteryLogAdapter(dataMap));
                }

            }
        }.execute();
//
//
//        Wearable.DataApi.getDataItem(mGoogleApiClient, getUriForDataItem()).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//            @Override
//            public void onResult(DataApi.DataItemResult dataItemResult) {
//
//
//
//            }
//        });


    }

    private Uri getUriForDataItem() {
        String nodeId = getRemoteNodeId();
        return new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(nodeId).path("/battery").build();
    }

    private String getRemoteNodeId() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodesResult =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = nodesResult.getNodes();
        if (nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
    }

    private void talkToWearableService(final ServiceCommand command){

        new AsyncTask<Void, Void, List<Node>>(){

            @Override
            protected List<Node> doInBackground(Void... params) {
                return getNodes();
            }

            @Override
            protected void onPostExecute(List<Node> nodeList) {
                for(Node node : nodeList) {

                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient,
                            node.getId(),
                            command.command,
                            null
                    );

                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.v(TAG, "Phone: " + sendMessageResult.getStatus().getStatusMessage());
                        }
                    });
                }
            }
        }.execute();

    }

    private List<Node> getNodes() {
        List<Node> nodes = new ArrayList<Node>();
        NodeApi.GetConnectedNodesResult rawNodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : rawNodes.getNodes()) {
            nodes.add(node);
        }
        return nodes;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "On DataChanged");
        init();
    }

    private class BatteryLogAdapter extends BaseAdapter{

        private List<BatteryLogItem> batteryLogItems = new ArrayList<BatteryLogItem>();

        public BatteryLogAdapter(DataMap dataMap){
            Set<String> keys = dataMap.keySet();
            for(String key : keys){
                batteryLogItems.add(new BatteryLogItem(Long.valueOf(key), dataMap.getInt(key)));
            }
            Collections.sort(batteryLogItems);
        }

        @Override
        public int getCount() {
            return batteryLogItems.size();
        }

        @Override
        public BatteryLogItem getItem(int position) {
            return batteryLogItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).time;
        }

        private class BatteryHolder{
            public TextView time, battery;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BatteryHolder holder;
            if(convertView == null){
                holder = new BatteryHolder();
                convertView = getLayoutInflater().inflate(android.R.layout.two_line_list_item, null);
                holder.time = (TextView) convertView.findViewById(android.R.id.text1);
                holder.battery = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            }
            else{
                holder = (BatteryHolder) convertView.getTag();
            }

            BatteryLogItem batteryLogItem = getItem(position);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(batteryLogItem.time);

            holder.time.setText(new SimpleDateFormat("KK:mm | MMMM dd, y").format(calendar.getTime()));
            holder.battery.setText(batteryLogItem.battery + "%");


            return convertView;
        }


    }

    private class BatteryLogItem implements Comparable<BatteryLogItem>{

        public Long time;
        public Integer battery;

        public BatteryLogItem(Long time, Integer battery){
            this.time = time;
            this.battery = battery;
        }

        @Override
        public int compareTo(BatteryLogItem another) {
            return another.time.compareTo(time);
        }
    }
}
