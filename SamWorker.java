package io.ionic.starter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cordova.shealth.ShealthPlugin;
import com.cordova.shealth.StepCountReader;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class SamWorker extends Worker {

  public Context context2;
  int total_steps = 0;
  public ShealthPlugin plugin;
  public static String selectedSource = "google";
  public static final String TAG1 = "Background worker";
  public static final String TAG = "Steps";
  private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
  private GoogleApiClient mClient;
  public static JSONArray stepResult;

  public SamWorker(
    @NonNull Context context,
    @NonNull WorkerParameters params) {
    super(context, params);
    context2 = context;
  }

  @Override
  public Result doWork() {

    Calendar startCalender = Calendar.getInstance();
    Date startDate = new Date();
    startCalender.setTime(startDate);
    startCalender.add(Calendar.DAY_OF_MONTH, -2);
    startCalender.set(Calendar.HOUR_OF_DAY, 0);
    startCalender.set(Calendar.MINUTE, 00);
    startCalender.set(Calendar.MILLISECOND, 00);
    long startTime = startCalender.getTimeInMillis();

    Calendar endCalender = Calendar.getInstance();
    Date endDate = new Date();
    endCalender.setTime(endDate);
    endCalender.set(Calendar.HOUR_OF_DAY, 0);
    endCalender.set(Calendar.MINUTE, 00);
    endCalender.set(Calendar.MILLISECOND, 00);
    long endTime = endCalender.getTimeInMillis();

    Log.i(TAG1, "Selected data source is : " + selectedSource);

    if(selectedSource.equals("samsung")) {
      getDataFromSamsung(startTime,endTime);

    } else {
      getDataFromGoogle(startTime,endTime);
    }

    ;
    return Result.success();

  }

  private void getDataFromSamsung(long startTime,long endTime) {

    plugin = new ShealthPlugin();
    Looper.prepare();
    plugin.initializeForBackground(getApplicationContext(),context2);

    try {
      CallbackContext callbackContext = new CallbackContext("123",null);

      if(plugin.isPermissionAcquired()){
        plugin.executeForBackground(startTime, endTime, callbackContext);
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }
    Looper.loop();
  }

  private void getDataFromGoogle(long startTime,long endTime) {
    DataReadRequest readRequest = queryFitnessData(startTime,endTime);
    if ((mClient == null) || (!mClient.isConnected())) {
      if (!lightConnect()) {
        return;
      }
    }
    DataReadResult dataReadResult = Fitness.HistoryApi.readData(mClient, readRequest).await();
    dataReadResult.getBuckets();
    printData2(dataReadResult);
  }

  private boolean lightConnect() {

    GoogleApiClient.Builder builder = new GoogleApiClient.Builder(getApplicationContext());
    builder.addApi(Fitness.HISTORY_API);
    builder.addApi(Fitness.CONFIG_API);
    builder.addApi(Fitness.SESSIONS_API);

    mClient = builder.build();
    mClient.blockingConnect();
    if (mClient.isConnected()) {
      Log.i(TAG, "Google Fit connected (light)");
      return true;
    } else {
      Log.i(TAG, "Google Fit not connected (light)");
      return false;
    }
  }

  public void printData2(DataReadResult dataReadResult) {

    if (dataReadResult.getBuckets().size() > 0) {
      Log.i(
        TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
      for (Bucket bucket : dataReadResult.getBuckets()) {
        List<DataSet> dataSets = bucket.getDataSets();
        for (DataSet dataSet : dataSets) {
          dumpDataSet(dataSet);
        }
      }
    } else if (dataReadResult.getDataSets().size() > 0) {
      Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
      for (DataSet dataSet : dataReadResult.getDataSets()) {
        dumpDataSet(dataSet);
      }
    }

  }

  public static DataReadRequest queryFitnessData(long startTime,long endTime) {

    DataSource filteredStepsSource = new DataSource.Builder()
      .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
      .setType(DataSource.TYPE_DERIVED)
      .setStreamName("estimated_steps")
      .setAppPackageName("com.google.android.gms")
      .build();

    DataReadRequest.Builder readRequestBuilder = new DataReadRequest.Builder();
    readRequestBuilder.setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS);
    readRequestBuilder.aggregate(filteredStepsSource, DataType.AGGREGATE_STEP_COUNT_DELTA);
    readRequestBuilder.bucketByTime(1, TimeUnit.DAYS);

    return readRequestBuilder.build();
  }

  private void dumpDataSet(DataSet dataSet) {
    Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
    DateFormat dateFormat = getTimeInstance();
    JSONArray stepResponse = new JSONArray();
    for (DataPoint dp : dataSet.getDataPoints()) {
      Log.i(TAG, "Data point:");
      Log.i(TAG, "\tType: " + dp.getDataType().getName());
      Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
      Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
      for (Field field : dp.getDataType().getFields()) {
        Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
        total_steps = total_steps + Integer.parseInt(dp.getValue(field).toString());
        JSONObject daySteps = new JSONObject();
        try {
          daySteps.put("date", dp.getStartTime(TimeUnit.MILLISECONDS));
          daySteps.put("stepCount", Integer.parseInt(dp.getValue(field).toString()));
          stepResponse.put(daySteps);
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }
    showResult(stepResponse);
  }



  public static void showResult(JSONArray result){
    stepResult = result;
    Log.i(TAG1, "Steps from :" + selectedSource);
    for(int i=0;i<result.length();i++){
      try {
        Object object =  result.get(i).toString();
        JSONObject jsonObject = new JSONObject(object.toString());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(jsonObject.getString("date")));
        Date date = new Date(calendar.getTimeInMillis());
        Log.i(TAG1, "Date : " + date);
        Log.i(TAG1, "Steps : " + jsonObject.getString("stepCount"));
      } catch (JSONException e) {
        e.printStackTrace();
      }

    }
  }
}

