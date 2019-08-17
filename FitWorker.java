package org.apache.cordova.health;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.ionic.starter.MainActivity;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class FitWorker extends Worker {
  public static final String TAG = "BasicHistoryApi";
  // Identifier to identify the sign in activity.
  private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
  public Context context2;
  int total_steps=0;
  private GoogleApiClient mClient;


  public FitWorker(
    @NonNull Context context,
    @NonNull WorkerParameters params) {
    super(context, params);
    context2 = context;
  }

  @Override
  public Result doWork() {
    // Do the work here--in this case, upload the images.
    Log.d(TAG, "onSuccess()");
    Log.i(TAG, "Worker running");
    getDirectSteps();
    return Result.success();

  }

  private void getDirectSteps() {
    DataReadRequest readRequest = queryFitnessData();
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

    Looper.prepare();
    Toast.makeText(context2,"Total steps " + total_steps, Toast.LENGTH_LONG).show();
    Looper.loop();




  }



  public static DataReadRequest queryFitnessData() {

    Calendar cal = Calendar.getInstance();
    Date now = new Date();
    cal.setTime(now);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 00);
    long endTime = cal.getTimeInMillis();
    Date endDate = new Date(endTime);
    cal.add(Calendar.DAY_OF_MONTH, -1);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 00);
    long startTime = cal.getTimeInMillis();
    Date startDate = new Date(startTime);

    java.text.DateFormat dateFormat = getDateInstance();
    Log.i(TAG, "Range Start: " + startDate);
    Log.i(TAG, "Range End: " + endDate);


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

    for (DataPoint dp : dataSet.getDataPoints()) {
      Log.i(TAG, "Data point:");
      Log.i(TAG, "\tType: " + dp.getDataType().getName());
      Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
      Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
      for (Field field : dp.getDataType().getFields()) {
        Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
        total_steps = total_steps + Integer.parseInt(dp.getValue(field).toString());
      }
    }
  }
}
