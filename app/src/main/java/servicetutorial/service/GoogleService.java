package servicetutorial.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.http.NameValuePair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GoogleService extends Service implements LocationListener {
    String url = "http://54.255.200.93/bbpts/d1.php";
    String data;
    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude, longitude;
    LocationManager locationManager;
    Location location;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    long notify_interval = 5000;
    public static String str_receiver = "servicetutorial.service.receiver";
    Intent intent;
    String device_id="";
    int android_version;
    int s_no = 0;
    int isOnline;
    String TAG ="Location****";
    public GoogleService() {
        android_version = android.os.Build.VERSION.SDK_INT; }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetLocation(), 5, notify_interval);
        intent = new Intent(str_receiver);
    }
    @Override
    public void onLocationChanged(Location location) {
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    @Override
    public void onProviderDisabled(String provider) {
    }
    private void fn_getlocation() {

        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.e("resu",String.valueOf(isGPSEnable)+isNetworkEnable);
        if (!isGPSEnable && !isNetworkEnable) {
        } else {
            if (isGPSEnable) {
                location = null;
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        Log.e("latitude", location.getLatitude() + "");
                        Log.e("longitude", location.getLongitude() + "");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        fn_update(location,"GPS");
                    }
                }
            }
            else if (isNetworkEnable) {
                Log.e(TAG,"isNetword");
                location = null;
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location != null) {
                        Log.e("latitude", location.getLatitude() + "");
                        Log.e("longitude", location.getLongitude() + "");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        fn_update(location,"Network");
                    }
                }
            }
        }
    }
    private class TimerTaskToGetLocation extends TimerTask{
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fn_getlocation();
                }
            });
        }
    }

    private void fn_update(Location location,String from){
        Log.e("Updatelatitude@@@@",location.getLatitude()+"");
        Log.e("Updatelongitude@@@",location.getLongitude()+"");
        s_no=s_no+1;
        ContentValues contentValues = new ContentValues();
        contentValues.put(LocationsContentProvider.LocationsDB.FIELD_LAT, location.getLatitude());
        contentValues.put(LocationsContentProvider.LocationsDB.FIELD_LNG, location.getLongitude());
        LocationInsertTask insertTask = new LocationInsertTask();
        insertTask.execute(contentValues);
        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
        String mobileAppVersion = "mobv1.0";
        String deviceName = android.os.Build.MODEL;
        String currentTime= String.valueOf(System.currentTimeMillis());

        if(isNetworkEnable){
            isOnline=1;
        }else{
            isOnline=0;
        }
        data = ""+mobileAppVersion+","+device_id+","+currentTime+","+location.getLatitude()+","+location.getLongitude()+","+isOnline+","+deviceName+","+android_version+","+s_no+","+from+"";
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        Log.e(TAG,activeNetworkInfo + "" +activeNetworkInfo.isConnected());
        if(activeNetworkInfo != null && activeNetworkInfo.isConnected()){
            new GetJSONTask().doInBackground(url,data);
        }else{
            Log.e(TAG,"No net");
            //Get sqlite data;
            Uri uri = LocationsContentProvider.CONTENT_URI;
            Cursor cursor= getContentResolver().query(LocationsContentProvider.CONTENT_URI,null,null,null,null);
            String[] data      = null;

            if (cursor.moveToFirst()) {
                do {
                    // get the data into array, or class variable
                } while (cursor.moveToNext());
            }
            cursor.close();
            Log.e(TAG, String.valueOf(data));
        }
//        intent.putExtra("latutide",location.getLatitude()+"");
//        intent.putExtra("longitude",location.getLongitude()+"");
//        sendBroadcast(intent);
    }
    private class LocationInsertTask extends AsyncTask<ContentValues, Void, Void> {
        @Override
        protected Void doInBackground(ContentValues... contentValues) {
            Log.e("insertSQlite", String.valueOf(contentValues[0]));
            getContentResolver().insert(LocationsContentProvider.CONTENT_URI, contentValues[0]);
            return null;
        }
    }
    private class LocationDeleteInsertTask extends AsyncTask<ContentValues, Void, Void> {
        @Override
        protected Void doInBackground(ContentValues... contentValues) {
            getContentResolver().delete(LocationsContentProvider.CONTENT_URI, null, null);
            return null;
        }
    }
    private Loader<Cursor> onCreateLoader() {
        Uri uri = LocationsContentProvider.CONTENT_URI;
        Cursor cursor= getContentResolver().query(LocationsContentProvider.CONTENT_URI,null,null,null,null);
        String[] data      = null;

        if (cursor.moveToFirst()){
            do {
                // Passing values
                String column1 = cursor.getString(0);
                String column2 = cursor.getString(1);
                String column3 = cursor.getString(2);
                // Do something Here with values
            } while(cursor.moveToNext());
        }
        cursor.close();
        return new CursorLoader(this, uri, null, null, null, null);
    }
    private class GetJSONTask extends AsyncTask<String, Void, String> {

        // onPreExecute called before the doInBackgroud start for display
        // progress dialog.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... params) {
            String res="";
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try {
                String url = params[0];
                String data = params[1];
                Log.e("url",url);
                Log.e("data",data);
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                String urlParameters = "data=" + data;

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                //print result
                System.out.println("@@@@@@@@@@@@" + response.toString());
                res= response.toString();
            } catch (Exception e) {
                Log.e("error", String.valueOf(e));
            }

            return res;
        }

        // onPostExecute displays the results of the doInBackgroud and also we
        // can hide progress dialog.
        @Override
        protected void onPostExecute(String result) {

        }
    }

}
