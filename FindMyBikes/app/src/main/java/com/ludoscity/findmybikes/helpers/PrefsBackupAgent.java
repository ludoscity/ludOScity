package com.ludoscity.findmybikes.helpers;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Created by F8Full on 2015-10-13.
 * Class used to interface with google backup service.
 */
public class PrefsBackupAgent extends BackupAgentHelper {

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper =
                new SharedPreferencesBackupHelper(this, DBHelper.SHARED_PREF_FILENAME);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
