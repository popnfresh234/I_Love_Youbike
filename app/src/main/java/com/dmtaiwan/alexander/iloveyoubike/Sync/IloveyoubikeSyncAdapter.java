package com.dmtaiwan.alexander.iloveyoubike.Sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.dmtaiwan.alexander.iloveyoubike.R;
import com.dmtaiwan.alexander.iloveyoubike.Utilities.Utilities;
import com.dmtaiwan.alexander.iloveyoubike.Data.StationContract;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

/**
 * Created by Alexander on 7/29/2015.
 */
public class IloveyoubikeSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String LOG_TAG = IloveyoubikeSyncAdapter.class.getSimpleName();
    private static final String APIUrl = "https://ybapp01.youbike.com.tw/json/gwjs.json";
    private static final int NUMBER_OF_STATIONS = 396;

    public static final String ACTION_DATA_UPDATED = "com.dmtaiwan.alexander.iloveyoubike.app.ACTION_DATA_UPDATED";

    public static final int SYNC_INTERVAL = 60*60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    public static final int STATUS_SERVER_DOWN = 0;
    public static final int STATUS_SERVER_INVALID = 1;
    public static final int STATUS_SERVER_UNKNOWN = 2;
    public static final int STATUS_FAVORITES_EMPTY = 3;

    public IloveyoubikeSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        URL url;
        HttpURLConnection urlConnection = null;
        String responseString = null;
        try {
            url = new URL(APIUrl);
            OkHttpClient client = Utilities.getUnsafeOkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            int responseCode = response.code();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                parseJson(response.body().string());
            }else{
                switch (responseCode) {
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        Utilities.setServerStatus(getContext(), STATUS_SERVER_INVALID);
                        return;
                    default:
                        Utilities.setServerStatus(getContext(), STATUS_SERVER_DOWN);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utilities.setServerStatus(getContext(), STATUS_SERVER_DOWN);

        }
    }




    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utilities.setServerStatus(getContext(), STATUS_SERVER_DOWN);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

    private void parseJson(String jsonData) {



        try {
            JSONObject result = new JSONObject(jsonData);
            JSONArray resultsArray = result.getJSONArray("retVal");
            Vector<ContentValues> cVVector = new Vector<ContentValues>(resultsArray.length());
            for (int i = 0; i < NUMBER_OF_STATIONS; i++) {
                JSONObject stationObject = resultsArray.getJSONObject(i);
                String stationId = stationObject.getString("iid");
                String stationNameChinese = stationObject.getString("sna");
                String stationDistrictChinese = stationObject.getString("sarea");
                String stationNameEnglish = stationObject.getString("snaen");
                String stationDistrictEnglish = stationObject.getString("sareaen");
                double stationLat = stationObject.getDouble("lat");
                double stationLong = stationObject.getDouble("lng");
                int bikesAvailable = stationObject.getInt("sbi");
                int spacesAvailable = stationObject.getInt("bemp");
                long time = stationObject.getLong("mday");

                ContentValues stationValues = new ContentValues();
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_ID, stationId);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_NAME_ZH, stationNameChinese);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_DISTRICT_ZH, stationDistrictChinese);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_NAME_EN, stationNameEnglish);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_DISTRICT_EN, stationDistrictEnglish);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_LAT, stationLat);
                stationValues.put(StationContract.StationEntry.COLUMN_STATION_LONG, stationLong);
                stationValues.put(StationContract.StationEntry.COLUMN_BIKES_AVAIABLE, bikesAvailable);
                stationValues.put(StationContract.StationEntry.COLUMN_SPACES_AVAILABLE, spacesAvailable);
                stationValues.put(StationContract.StationEntry.COLUMN_LAST_UPDATED,time);

                cVVector.add(stationValues);
            }

            if (cVVector.size() > 0) {
                Utilities.updateWidgets(getContext());
                //Remove old values
                getContext().getContentResolver().delete(StationContract.StationEntry.buildUriAllStations(), null, null);
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                getContext().getContentResolver().bulkInsert(StationContract.StationEntry.CONTENT_URI, cvArray);
                Utilities.updateWidgets(getContext());
            }

//            String sortOrder = StationContract.StationEntry.COLUMN_STATION_NAME_EN + " ASC";
//            Uri uri = StationContract.StationEntry.CONTENT_URI_TEST;
//            Cursor cur = getContext().getContentResolver().query(uri, null, null, null, sortOrder);
//            cVVector = new Vector<ContentValues>(cur.getCount());
//            if (cur.moveToFirst()) {
//                do {
//                    ContentValues cv = new ContentValues();
//                    DatabaseUtils.cursorRowToContentValues(cur, cv);
//                    cVVector.add(cv);
//                } while (cur.moveToNext());
//            }

        } catch (JSONException e) {
            e.printStackTrace();
            Utilities.setServerStatus(getContext(), STATUS_SERVER_INVALID);
        }
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        IloveyoubikeSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }



}
