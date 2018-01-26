package com.sierrawireless.avphone.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.sierrawireless.avphone.MainActivity;
import com.sierrawireless.avphone.ObjectsManager;
import com.sierrawireless.avphone.R;
import com.sierrawireless.avphone.model.AvPhoneObject;
import com.sierrawireless.avphone.tools.Tools;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MonitoringService extends Service {
    private static final String TAG = "MonitoringService";
    
    // system services
    private TelephonyManager telephonyManager;
    private ActivityManager activityManager;
    private ConnectivityManager connManager;



    // Intent extra keys
    public static final String DEVICE_ID = "device_id";
    public static final String SERVER_HOST = "server_host";
    public static final String PASSWORD = "password";
    public static final String CONNECT = "connect";
    public static final String OBJECT_NAME ="objname";

    private MqttPushClient client = null;

    private Long startedSince;

    private Long lastRun;
    private String lastLog;
    private NewData lastData = new NewData();
    /* the date of the last location reading */
    private long lastLocation;

    // FIXME(pht) for testing, to compare with "last known location"
    private Location networkLocation = null;
    private Location gpsLocation = null;
    private LocationListener networkLocationListener;
    private LocationListener gpsLocationListener;
    private ObjectsManager objectsManager;

    @Override
    public void onCreate() {
        // Unique Identification Number for the Notification.

        Log.d(TAG, "onCreate: " + this);
        objectsManager = ObjectsManager.getInstance();

        // Display a notification icon





        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);


        startedSince = System.currentTimeMillis();

    }

    public  void startSendData(){
        int NOTIFICATION = R.string.notif_title;
        // Create an intent to start the activity when clicking the notification
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this.getApplicationContext()) //
                .setContentTitle(getText(R.string.notif_title)) //
                .setContentText(getText(R.string.notif_desc)) //
                .setSmallIcon(R.drawable.ic_notif) //
                .setOngoing(true) //
                .setContentIntent(resultPendingIntent) //
                .build();

        startForeground(NOTIFICATION, notification);

    }

    public void stopSendData() {

        // Cancel the persistent notification.
        stopForeground(true);
    }

    @SuppressLint("HardwareIds")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        lastRun = System.currentTimeMillis();
        objectsManager = ObjectsManager.getInstance();
        AvPhoneObject object = objectsManager.getCurrentObject();


        try {
            final Boolean mustConnect = intent.getBooleanExtra(CONNECT, true);

            /* First we have to create the system if it doesn't exist */

            if (this.client == null) {

                //
                // Ensure intent is valid
                //
                final String deviceId = Tools.buildSerialNumber(intent.getStringExtra(DEVICE_ID), object.name);
                final String password = intent.getStringExtra(PASSWORD);
                final String serverHost = intent.getStringExtra(SERVER_HOST);

                final List<String> intentValuesList = Arrays.asList(deviceId, password, serverHost);
                if (intentValuesList.contains(null)) {
                    // Stop service when unable to start MQTT client
                    stopSelfResult(startId);
                    return Service.START_STICKY;
                }

                // Now, create client
                client = new MqttPushClient(deviceId, password, serverHost, mqttCallback);
            }
            if (!client.isConnected()) {
                client.connect();
            }

            if (mustConnect) {
                Location location = getLastKnownLocation();

                // retrieve data
                NewData data = new NewData();

                List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
                if (cellInfos != null && !cellInfos.isEmpty()) {
                    CellInfo cellInfo = cellInfos.get(0);
                    if (cellInfo instanceof CellInfoGsm) {
                        data.setRssi(((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm());
                   // } else if (cellInfo instanceof CellInfoWcdma) {
                        // RSSI ?
                        // data.setRssi(((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm());
                    } else if (cellInfo instanceof CellInfoLte) {
                        data.setRsrp(((CellInfoLte) cellInfo).getCellSignalStrength().getDbm());
                    }
                }

                if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                    data.setImei(telephonyManager.getDeviceId());
                }

                data.setOperator(telephonyManager.getNetworkOperatorName());

                switch (telephonyManager.getNetworkType()) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        data.setNetworkType("GPRS");
                        break;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        data.setNetworkType("EDGE");
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        data.setNetworkType("UMTS");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        data.setNetworkType("HSDPA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        data.setNetworkType("HSPA+");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        data.setNetworkType("HSPA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                        data.setNetworkType("HSUPA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        data.setNetworkType("LTE");
                        break;
                    // to be continued
                    default:
                }

                data.setActiveWifi(connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected());
                data.setRunningApps(activityManager.getRunningAppProcesses().size());
                data.setAndroidVersion(Build.VERSION.RELEASE);

                MemoryInfo mi = new MemoryInfo();
                activityManager.getMemoryInfo(mi);
                data.setMemoryUsage((float) ((mi.totalMem - mi.availMem) / ((Long) mi.totalMem).doubleValue()));

                // battery level
                final IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                final Intent batteryStatus = this.registerReceiver(null, iFilter);
                if (batteryStatus != null) {
                    final int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    final int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    data.setBatteryLevel(level / (float) scale);
                }

                // location
                if (location != null && location.getTime() != lastLocation) {
                    data.setLatitude(location.getLatitude());
                    data.setLongitude(location.getLongitude());
                    lastLocation = location.getTime();
                }

                // bytes sent/received
                data.setBytesReceived(TrafficStats.getMobileRxBytes());
                data.setBytesSent(TrafficStats.getMobileTxBytes());

                //execute action on current object datas
                objectsManager.execOnCurrent();
                // Custom data
                data.setCustom();


                //customDataSource.next(new Date());

                // save new data values
                if (data.getExtras() != null) {
                    lastData.putExtras(data.getExtras());
                }

                // dispatch new data event to update the activity UI
                LocalBroadcastManager.getInstance(this).sendBroadcast(data);

                this.client.push(data);
                lastLog = data.size() + " data pushed to the server";
                LocalBroadcastManager.getInstance(this).sendBroadcast(new LogMessage(lastLog));

                setUpLocationListeners();
            }

        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(TAG, "error", e);
            lastLog = "ERROR: " + e.getMessage();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new LogMessage(lastLog));
        }


        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("MonitoringService", "Stopping service");

        if (this.client != null) {
            try {
                this.client.disconnect();
            } catch (MqttException e) {
                Crashlytics.logException(e);
                Log.e(TAG, "error", e);
            }
        }

        // Cancel the persistent notification.
        stopForeground(true);

        stopLocationListeners();
    }

    private void setUpLocationListeners() {
        final LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null) {
            Log.e(TAG, "setUpLocationListeners: Can't get the location service");
            Toast.makeText(getApplicationContext(), "can't get location service", Toast.LENGTH_SHORT).show();
            return;
        }
        LocationProvider networkLocationProvider = locManager.getProvider(LocationManager.NETWORK_PROVIDER);
        if (networkLocationProvider != null) {
            networkLocationListener = new LocationListenerAdapter() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Received Network location update " + location.getLatitude() + ";" + location.getLongitude());
                    networkLocation = location;
                }
            };
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60 * 1000, 5, networkLocationListener);
        }
        LocationProvider gpsLocationProvider = locManager.getProvider(LocationManager.GPS_PROVIDER);
        if (gpsLocationProvider != null) {
            gpsLocationListener = new LocationListenerAdapter() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Received GPS location update " + location.getLatitude() + ";" + location.getLongitude());
                    gpsLocation = location;
                }
            };
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 5, gpsLocationListener);
        }
    }

    private void stopLocationListeners() {
        final LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null) {
            Log.e(TAG, "setUpLocationListeners: Can't get the location service");
            Toast.makeText(getApplicationContext(), "can't get location service", Toast.LENGTH_SHORT).show();
            return;
        }
        if (networkLocationListener != null) {
            locManager.removeUpdates(networkLocationListener);
        }
        if (gpsLocationListener != null) {
            locManager.removeUpdates(gpsLocationListener);
        }
    }


    private Location getLastKnownLocation() {
        final LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null) {
            Log.e(TAG, "setUpLocationListeners: Can't get the location service");
            Toast.makeText(getApplicationContext(), "can't get location service", Toast.LENGTH_SHORT).show();
            return null;
        }
        final String locationProvider = locManager.getBestProvider(new Criteria(), true);
        Log.d(TAG, "Getting last known location from provider: " + locationProvider);

        Location location = null;
        if (locationProvider != null) {
            location = locManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                Log.d(TAG, "Last known location : " + location.getLatitude() + "," + location.getLongitude());
            } else {
                Log.d(TAG, "Read null location");
            }
            if (networkLocation != null) {
                Log.d(TAG, "Last Network Location : " + networkLocation.getLatitude() + "," + networkLocation.getLongitude());
            } else {
                Log.d(TAG, "No known network location");
            }
            if (gpsLocation != null) {
                Log.d(TAG, "Last GPS Location : " + gpsLocation.getLatitude() + "," + gpsLocation.getLongitude());
            } else {
                Log.d(TAG, "No known GPSlocation");
            }
        }
        return location;
    }

    public void sendAlarmEvent() {

        if (this.client == null) {
            Toast.makeText(getApplicationContext(), "Alarm client is not available,wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!client.isConnected()) {
            Log.e(TAG, "onStartCommand: client connect called");
            try {
                client.connect();
            }
             catch (Exception e) {
                Crashlytics.logException(e);
                Log.e(TAG, "error", e);
                lastLog = "ERROR: " + e.getMessage();
                LocalBroadcastManager.getInstance(this).sendBroadcast(new LogMessage(lastLog));
            }
        }

        NewData data = new NewData();
        data.setAlarmActivated();

        // save alarm state
        if (data.getExtras() != null) {
            lastData.putExtras(data.getExtras());
        }

        try {
            this.client.push(data);
        } catch (MqttException e) {
            Toast.makeText(getApplicationContext(), "Can't send Alarm ", Toast.LENGTH_SHORT).show();
            Crashlytics.logException(e);
            Log.e(TAG, "Could not push the alarm event", e);
        }
    }

    // Service binding

    private ServiceBinder binder = new ServiceBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    public class ServiceBinder extends Binder {

        public MonitoringService getService() {
            return MonitoringService.this;
        }
    }

    public long getStartedSince() {
        return startedSince;
    }

    public NewData getLastData() {
        return lastData;
    }

    public String getLastLog() {
        return lastLog;
    }

    public Long getLastRun() {
        return lastRun;
    }

    // MQTT client callback

    @SuppressWarnings("unused")
    private MqttCallback mqttCallback = new MqttCallback() {

        class Message {
            String uid;
            long timestamp;
            Command command;
        }

        class Command {
            String id;
            Map<String, String> params;
        }

        @Override
        public void messageArrived(String topic, MqttMessage msg) throws Exception {
            Log.d(TAG, "MQTT msg received: " + new String(msg.getPayload()));

            // parse json payload
            Message[] messages = new Gson().fromJson(new String(msg.getPayload(), "UTF-8"), Message[].class);

            // display a new notification
            Notification notification = new Notification.Builder(MonitoringService.this.getApplicationContext()) //
                    .setContentTitle(getText(R.string.notif_new_message)) //
                    .setContentText(messages[0].command.params.get("message")) //
                    .setSmallIcon(R.drawable.ic_notif) //
                    .setAutoCancel(true) //
                    .build();

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager == null) {
                Log.e(TAG, "setUpLocationListeners: Can't get the notification service");
                Toast.makeText(getApplicationContext(), "can't get notification service for incoming data", Toast.LENGTH_SHORT).show();
                return;
            }
            mNotificationManager.notify((int) messages[0].timestamp, notification);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
            //
        }

        @Override
        public void connectionLost(Throwable arg0) {
            //
        }
    };

}
