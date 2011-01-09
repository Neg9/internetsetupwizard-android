/*
 * STRecoveryTask.java
 * Copyright (C) 2010 Neg9 https://neg9.org
 *
 * Authors:
 *   Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neg9.internetsetupwizard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class STRecoveryTask extends AsyncTask<String, Void, String[]>
{
    private Activity       mActivity;
    private long           mStartTime;
    private Exception      mException;
    private ProgressDialog mDialog;

    private String mSSID;
    private String mCode;

    public STRecoveryTask (Activity activity)
    {
        mActivity = activity;
        mDialog = new ProgressDialog(activity);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(activity.getString(R.string.recovering_key));
        mDialog.setOnCancelListener(new ProgressDialog.OnCancelListener() {
            public void onCancel(DialogInterface dialogInterface) {
                STRecoveryTask.this.cancel(true);
            }
        });
    }

    @Override
    protected void onPreExecute() {
        if (mException == null) {
            mDialog.show();
        } else {
            new AlertDialog.Builder(mActivity)
                .setMessage(mException.getMessage())
                .setCancelable(false)
                .show();
        }
    }

    @Override
    protected String[] doInBackground(String... strings) {
        try {
            mSSID = strings[0];
            mCode = strings[1];

            List<String> results = new ArrayList<String>();

            mStartTime = System.currentTimeMillis();

            String installPath = "/data/data/" + BinaryInstaller.ST_APP_USERNAME + "/";
            String exe = installPath + BinaryInstaller.ST_BINARY_ASSET_KEY;
            Process process = Runtime.getRuntime().exec(new String[] { exe, "-i", mCode });

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            while ((line = reader.readLine()) != null) {
                results.add(line);
            }

            reader.close();

            String[] resultsArray = new String[results.size()];
            results.toArray(resultsArray);

            return resultsArray;
        } catch (Exception ex) {
            mException = ex;
            return null;
        }
    }

    @Override
    protected void onPostExecute(String[] results) {
        mDialog.hide();
        
        if (mException == null) {
            long endTime = System.currentTimeMillis();
            long timeElapsed = ((endTime - mStartTime) / 1000);

            Intent intent = new Intent(mActivity, ResultsActivity.class);
            intent.putExtra("CODE", mCode);
            intent.putExtra("SSID", mSSID);
            intent.putExtra("TIME_ELAPSED", timeElapsed);
            intent.putExtra("RESULTS", results);
            mActivity.startActivity(intent);
        } else {
            new AlertDialog.Builder(mActivity)
                .setMessage(mException.getMessage())
                .setCancelable(false)
                .show();
        }
    }

    @Override
    protected void onCancelled() {
        mDialog.hide();
    }
}