/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package io.ionic.starter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.*;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends CordovaActivity
{

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        Constraints constraints = new Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build();

        String workTag = "SamJob";
        WorkManager.getInstance().cancelAllWorkByTag(workTag);
        PeriodicWorkRequest compressionWork =
          new PeriodicWorkRequest.Builder(SamWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag(workTag)
            .build();
        Log.i(TAG, "Work request id "+  compressionWork.getId().toString());
        WorkManager.getInstance().enqueueUniquePeriodicWork(workTag, ExistingPeriodicWorkPolicy.REPLACE,compressionWork);
    }
}
