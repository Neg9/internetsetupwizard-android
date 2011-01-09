/*
 * MainActivity.java
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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.*;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.util.List;

public class MainActivity extends ListActivity
{
    private STRecoveryTask mTask;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
            BinaryInstaller installer = new BinaryInstaller();
            installer.start(true);
        } catch (Exception ex) {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage(ex.getMessage())
                .show();
        }

        getListView().setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ScanResult result = (ScanResult) adapterView.getItemAtPosition(position);
                // FIXME: Use a real RegEx here.
                if (result.SSID.startsWith("SpeedTouch")) {
                    String code = result.SSID.substring(10, 16);
                    mTask = new STRecoveryTask(MainActivity.this);
                    mTask.execute(result.SSID, code);
                } else {
                    Toast.makeText(MainActivity.this, R.string.unsupported_ssid, 10).show();
                }
            }
        });

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!manager.isWifiEnabled()) {
            Toast.makeText(this, R.string.enabling_wifi, 10).show();
            manager.setWifiEnabled(true);
        }
        
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> results = manager.getScanResults();
                setListAdapter(new ScanResultAdapter(MainActivity.this, results));
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        
        manager.startScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        if (item.getItemId() == R.id.manual_lookup) {
            final EditText editText = new EditText(this);

            new AlertDialog.Builder(this)
                .setTitle(R.string.enter_network_id)
                .setView(editText)
                .setPositiveButton(R.string.recover_key, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mTask = new STRecoveryTask(MainActivity.this);
                        mTask.execute(null, editText.getText().toString());
                    }
                })
                .show();

            return true;
        }

        return false;
    }

    private class ScanResultAdapter extends ArrayAdapter<ScanResult>
    {
        public ScanResultAdapter (Context context, List<ScanResult> results)
        {
            super(context, 0, results);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent)
        {
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, null);
            }

            ScanResult result = getItem(position);

            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            textView.setText(result.SSID);

            textView = (TextView) convertView.findViewById(android.R.id.text2);
            textView.setText(result.BSSID);

            return convertView;
        }
    }
}
