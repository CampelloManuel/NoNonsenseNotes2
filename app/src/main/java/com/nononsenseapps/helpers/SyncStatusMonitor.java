/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.helpers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.database.MyContentProvider;
import com.nononsenseapps.notepad.prefs.SyncPrefs;
import com.nononsenseapps.notepad.sync.SyncAdapter;

public final class SyncStatusMonitor extends BroadcastReceiver {
	Activity activity;
	OnSyncStartStopListener listener;

	/**
	 * Call this in the activity's onResume
	 */
	public void startMonitoring(Activity activity, OnSyncStartStopListener listener) {
		// in the caller, the activity acts also as the listener, anyway
		this.activity = activity;
		this.listener = listener;

		activity.registerReceiver(this, new IntentFilter(SyncAdapter.SYNC_FINISHED));
		activity.registerReceiver(this, new IntentFilter(SyncAdapter.SYNC_STARTED));

		final String accountName = PreferenceManager.getDefaultSharedPreferences(activity)
				.getString(SyncPrefs.KEY_ACCOUNT, "");
		Account account = null;
		if (accountName != null && !accountName.isEmpty()) {
			account = SyncGtaskHelper.getAccount(AccountManager.get(activity), accountName);
		}
		// Sync state might have changed, make sure we're spinning when
		// we should
		try {
			listener.onSyncStartStop(account != null
					&& ContentResolver.isSyncActive(account, MyContentProvider.AUTHORITY));
		} catch (Exception e) {
			NnnLogger.debug(SyncStatusMonitor.class, e.getLocalizedMessage());
		}
	}

	/**
	 * Call this in the activity's onPause
	 */
	public void stopMonitoring() {
		try {
			activity.unregisterReceiver(this);
		} catch (Exception e) {
			NnnLogger.exception(e);
		}
		try {
			listener.onSyncStartStop(false);
		} catch (Exception e) {
			NnnLogger.exception(e);
		}
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(SyncAdapter.SYNC_STARTED)) {
			activity.runOnUiThread(() -> {
				try {
					listener.onSyncStartStop(true);
				} catch (Exception e) {
					NnnLogger.exception(e);
				}
			});
		} else { //if (intent.getAction().equals(SyncAdapter.SYNC_FINISHED)) {
			activity.runOnUiThread(() -> {
				try {
					listener.onSyncStartStop(false);
				} catch (Exception e) {
					NnnLogger.exception(e);
				}
			});
			Bundle b = intent.getExtras();
			if (b == null) {
				b = Bundle.EMPTY;
			}
			tellUser(context, b.getInt(SyncAdapter.SYNC_RESULT, SyncAdapter.SUCCESS));
		}
	}

	private void tellUser(Context context, int result) {
		int text;
		switch (result) {
			case SyncAdapter.ERROR:
				text = R.string.sync_failed;
				break;
			case SyncAdapter.LOGIN_FAIL:
				text = R.string.sync_login_failed;
				break;
			case SyncAdapter.SUCCESS:
			default:
				return;
		}

		NnnLogger.debug(SyncStatusMonitor.class, "SYNC: " + result);
		Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		toast.show();
	}

	public interface OnSyncStartStopListener {
		/**
		 * This is always called on the activity's UI thread.
		 */
		void onSyncStartStop(final boolean ongoing);
	}
}