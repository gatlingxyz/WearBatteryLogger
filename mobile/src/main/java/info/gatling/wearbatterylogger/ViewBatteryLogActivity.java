package info.gatling.wearbatterylogger;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.BatteryManager;
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
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Created by gimmiepepsi on 10/11/14.
 */
public class ViewBatteryLogActivity extends ListActivity implements DataApi.DataListener {

    private GoogleApiClient mGoogleApiClient;
    private final static String TAG = "TAVON Battery Logger";

    /**
     * At some point, I'm going to allow the mobile app to stop the service and start it on command.
     * I'm not sure if checking and saving battery info is putting extra strain on the battery, so I
     * would like to be able to turn it off when I don't need it anymore without having to delete the app.
     */
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
                        loadAllBatteryInfo();
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

    /**
     * Gets all the battery info saved using the DataApi and pass it to our adapter.
     */
    private void loadAllBatteryInfo() {
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                if (dataItems.getCount() != 0) {
                    setListAdapter(new BatteryLogAdapter(dataItems));
                }
                dataItems.release();
            }
        });
    }

    /**
     * Send a quick message to the wearable listener. At this time, it's only sending START but it's
     * built to be able to send STOP whenever I get the code up and running.
     * @param command
     */
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
        loadAllBatteryInfo();
    }

    private class BatteryLogAdapter extends BaseAdapter{

        private List<BatteryLogItem> batteryLogItems = new ArrayList<BatteryLogItem>();

        public BatteryLogAdapter(DataItemBuffer dataItems){
            for(DataItem dataItem : dataItems){
                final String time = dataItem.getUri().getLastPathSegment();
                Log.v(TAG, "Time: " + time);
                try {
                    batteryLogItems.add(new BatteryLogItem(Long.valueOf(dataItem.getUri().getLastPathSegment()), DataMapItem.fromDataItem(dataItem).getDataMap()));
                }
                catch(NumberFormatException e){
                    /**
                     * This exists because, if you look at my previous commits, everything used to be saved to the path
                     * "/battery". When I discovered what else I could get from the battery intent, I had to change
                     * the way the data was saved and switched to creating a new PutDataMapRequest for each timestamp
                     * that held all the relevant information. This try/catch is to remove any lingering "/battery" I had,
                     * as well as delete something that just so happens to go wrong.
                     */
                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItem.getUri()).setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                        @Override
                        public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                            Log.v(TAG, "Deleted " + deleteDataItemsResult.getNumDeleted() + " " + time + " data item");
                        }
                    });
                }
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
                // Todo: Make own view
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

            //  TODO: Get local time format or set up some customization for it
            holder.time.setText(new SimpleDateFormat("KK:mm | MMMM dd, y").format(calendar.getTime()));
            holder.battery.setText(batteryLogItem.battery + "% ||| " + batteryLogItem.fahrenheit + "\u00B0F " + batteryLogItem.powerSource);

            return convertView;
        }
    }

    private class BatteryLogItem implements Comparable<BatteryLogItem>{

        public Long time;
        public int battery;
        public int temperature;
        public String powerSource;

        public double fahrenheit;
        public double celsius;

        public BatteryLogItem(Long time, DataMap dataMap){
            this.time = time;
            this.battery = dataMap.getInt(BatteryManager.EXTRA_LEVEL);
            this.temperature = dataMap.getInt(BatteryManager.EXTRA_TEMPERATURE);

            int source = dataMap.getInt(BatteryManager.EXTRA_PLUGGED);
            switch(source){
                case BatteryManager.BATTERY_PLUGGED_AC:
                    powerSource = "(Plugged in, AC)";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    powerSource = "(Plugged in, USB)";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    powerSource = ("Wireless charging");
                    break;
                default:
                    powerSource = "(Not charging, on battery)";
            }

            this.celsius = temperature/10;
            this.fahrenheit = ((9/5) * celsius) + 32;
        }

        @Override
        public int compareTo(BatteryLogItem another) {
            return another.time.compareTo(time); // Newest up top
        }
    }
}
