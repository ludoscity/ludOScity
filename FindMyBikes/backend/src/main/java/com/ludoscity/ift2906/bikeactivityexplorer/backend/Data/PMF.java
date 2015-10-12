package com.ludoscity.ift2906.bikeactivityexplorer.backend.Data;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;

/**
 * Created by F8Full on 2015-02-14.
 * This file is part of BixiTracksExplorer--Backend
 * Singleton class to interact with datastore through JDO
 */
public final class PMF {
    private static final PersistenceManagerFactory pmfInstance =
            JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {}

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
}
//test 1 mehdi