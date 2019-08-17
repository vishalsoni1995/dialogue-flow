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
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.text.DateFormat.getDateInstance;

public class SamWorker extends Worker {

  public Context context2;
  int total_steps = 0;
  ShealthPlugin plugin;

  private StepCountReader mReporter;
  private HealthDataStore mStore;


  public SamWorker(
    @NonNull Context context,
    @NonNull WorkerParameters params) {
    super(context, params);
    context2 = context;
  }

  @Override
  public Result doWork() {

    plugin = new ShealthPlugin();



    Looper.prepare();
     plugin.initializeForBackground(getApplicationContext(), MainActivity.mainActivity);







    try {
      CallbackContext callbackContext = new CallbackContext("123",null);


        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
//        cal.set(Calendar.HOUR_OF_DAY, 0);
//        cal.set(Calendar.MINUTE, 00);
        long endTime = cal.getTimeInMillis();
        Date endDate = new Date(endTime);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 00);
        long startTime = cal.getTimeInMillis();
        Date startDate = new Date(startTime);

        if(plugin.isPermissionAcquired()){
        JSONArray jsonArray = plugin.executeForBackground(startTime,endTime, callbackContext);}

    } catch (JSONException e) {
      e.printStackTrace();
    }

    Looper.loop();
    return Result.success();

  }

}

