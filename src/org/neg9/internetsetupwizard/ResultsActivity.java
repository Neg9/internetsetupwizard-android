/*
 * ResultsActivity.java
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

import android.app.ListActivity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ResultsActivity extends ListActivity
{
    private String mCode;
    private String mSSID;

    @Override
    protected void onCreate (Bundle bundle)
    {
        super.onCreate(bundle);
        setContentView(R.layout.results);

        mCode = getIntent().getStringExtra("CODE");
        mSSID = getIntent().getStringExtra("SSID");
        
        final long timeElapsed = getIntent().getLongExtra("TIME_ELAPSED", -1);
        final String[] results = getIntent().getStringArrayExtra("RESULTS");

        if (mSSID != null)
            setTitle("SSID: " + mSSID + " - found keys: " + results.length);
        else
            setTitle("Code: " + mCode + " - found keys: " + results.length);

        registerForContextMenu(getListView());
        setListAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, results));

        getListView().setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick (AdapterView<?> adapterView, View view, int position, long id) {
                String key = (String) adapterView.getItemAtPosition(position);

                if (mSSID != null) {
                    connectWithKey(key);
                } else {
                    copyKey(key);
                }
            }
        });
    }

    @Override
    public void onCreateContextMenu (ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        if (mSSID != null) {
            menu.add(0, 0, 0, R.string.connect_with_key);
        }
        menu.add(0, 1, 0, R.string.copy_key);
    }

    @Override
    public boolean onContextItemSelected (MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        String key = (String) getListAdapter().getItem(info.position);

        if (item.getItemId() == 0) {
            connectWithKey(key);
        } else {
            copyKey(key);
        }
      
        return true;
    }

    private void copyKey (String key)
    {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setText(key);
        Toast.makeText(this, R.string.key_copied, 10).show();
    }

    private void connectWithKey (String key)
    {
        // FIXME: This doesn't seem to actually work.

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + mSSID + "\"";
        config.priority = 40;
        config.wepKeys = new String[] { "\"" + key + "\"" };
        config.wepTxKeyIndex = 0;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int netId = manager.addNetwork(config);
        if (netId != -1) {
            manager.enableNetwork(netId, true);
            Toast.makeText(ResultsActivity.this, R.string.connecting, 10).show();
        } else {
            Toast.makeText(ResultsActivity.this, R.string.connect_failed, 10).show();
        }
    }
}
