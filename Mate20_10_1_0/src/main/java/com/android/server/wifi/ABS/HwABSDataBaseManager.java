package com.android.server.wifi.ABS;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import com.android.server.wifi.hwUtil.StringUtilEx;
import java.util.ArrayList;
import java.util.List;

public class HwABSDataBaseManager {
    private static final int DATA_BASE_MAX_NUM = 500;
    private static final String TAG = "DataBaseManager";
    private static HwABSDataBaseManager mHwABSDataBaseManager = null;
    private SQLiteDatabase mDatabase;
    private HwABSDataBaseHelper mHelper;
    private final Object mLock = new Object();

    private HwABSDataBaseManager(Context context) {
        HwABSUtils.logD(false, "HwABSDataBaseManager()", new Object[0]);
        try {
            this.mHelper = new HwABSDataBaseHelper(context);
            this.mDatabase = this.mHelper.getWritableDatabase();
        } catch (SQLiteCantOpenDatabaseException e) {
            this.mDatabase = null;
        }
    }

    public static HwABSDataBaseManager getInstance(Context context) {
        if (mHwABSDataBaseManager == null) {
            mHwABSDataBaseManager = new HwABSDataBaseManager(context);
        }
        return mHwABSDataBaseManager;
    }

    public void closeDB() {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    HwABSUtils.logD(false, "HwABSDataBaseManager closeDB()", new Object[0]);
                    this.mDatabase.close();
                }
            }
        }
    }

    public void addOrUpdateApInfos(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (data != null) {
                    if (getApInfoByBssid(data.mBssid) == null) {
                        HwABSUtils.logD(false, "addOrUpdateApInfos inlineAddApInfo", new Object[0]);
                        checkIfAllCaseNumSatisfy();
                        inlineAddApInfo(data);
                    } else {
                        HwABSUtils.logD(false, "addOrUpdateApInfos", new Object[0]);
                        inlineUpdateApInfo(data);
                    }
                }
            }
        }
    }

    public List<HwABSApInfoData> getApInfoBySsid(String ssid) {
        synchronized (this.mLock) {
            List<HwABSApInfoData> lists = new ArrayList<>();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                return lists;
            }
            Cursor c = null;
            try {
                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where ssid like ?", new String[]{ssid});
                while (c2.moveToNext()) {
                    lists.add(new HwABSApInfoData(c2.getString(c2.getColumnIndex("bssid")), c2.getString(c2.getColumnIndex("ssid")), c2.getInt(c2.getColumnIndex("switch_mimo_type")), c2.getInt(c2.getColumnIndex("switch_siso_type")), c2.getInt(c2.getColumnIndex("auth_type")), c2.getInt(c2.getColumnIndex("in_black_list")), c2.getInt(c2.getColumnIndex("reassociate_times")), c2.getInt(c2.getColumnIndex("failed_times")), c2.getInt(c2.getColumnIndex("continuous_failure_times")), c2.getLong(c2.getColumnIndex("last_connect_time"))));
                }
                c2.close();
                return lists;
            } catch (SQLException e) {
                HwABSUtils.logE(false, "getApInfoBySsid:%{public}s", e.getMessage());
                if (0 != 0) {
                    c.close();
                }
                return null;
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
        }
    }

    public HwABSApInfoData getApInfoByBssid(String bssid) {
        synchronized (this.mLock) {
            HwABSApInfoData data = null;
            if (bssid == null) {
                return null;
            }
            Cursor c = null;
            try {
                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where bssid like ?", new String[]{bssid});
                if (c2.moveToNext()) {
                    data = new HwABSApInfoData(c2.getString(c2.getColumnIndex("bssid")), c2.getString(c2.getColumnIndex("ssid")), c2.getInt(c2.getColumnIndex("switch_mimo_type")), c2.getInt(c2.getColumnIndex("switch_siso_type")), c2.getInt(c2.getColumnIndex("auth_type")), c2.getInt(c2.getColumnIndex("in_black_list")), c2.getInt(c2.getColumnIndex("reassociate_times")), c2.getInt(c2.getColumnIndex("failed_times")), c2.getInt(c2.getColumnIndex("continuous_failure_times")), c2.getLong(c2.getColumnIndex("last_connect_time")));
                }
                c2.close();
                return data;
            } catch (SQLException e) {
                HwABSUtils.logE(false, "getApInfoByBssid:%{public}s", e.getMessage());
                if (0 != 0) {
                    c.close();
                }
                return null;
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
        }
    }

    public List<HwABSApInfoData> getApInfoInBlackList() {
        synchronized (this.mLock) {
            List<HwABSApInfoData> lists = new ArrayList<>();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                return lists;
            }
            Cursor c = null;
            try {
                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable where in_black_list like ?", new String[]{"1"});
                while (c2.moveToNext()) {
                    lists.add(new HwABSApInfoData(c2.getString(c2.getColumnIndex("bssid")), c2.getString(c2.getColumnIndex("ssid")), c2.getInt(c2.getColumnIndex("switch_mimo_type")), c2.getInt(c2.getColumnIndex("switch_siso_type")), c2.getInt(c2.getColumnIndex("auth_type")), c2.getInt(c2.getColumnIndex("in_black_list")), c2.getInt(c2.getColumnIndex("reassociate_times")), c2.getInt(c2.getColumnIndex("failed_times")), c2.getInt(c2.getColumnIndex("continuous_failure_times")), c2.getLong(c2.getColumnIndex("last_connect_time"))));
                }
                c2.close();
                return lists;
            } catch (SQLException e) {
                HwABSUtils.logE(false, "getApInfoByBssid:%{public}s", e.getMessage());
                if (0 != 0) {
                    c.close();
                }
                return lists;
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
        }
    }

    public void deleteAPInfosByBssid(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (this.mDatabase != null && this.mDatabase.isOpen()) {
                if (data != null) {
                    inlineDeleteApInfoByBssid(data.mBssid);
                }
            }
        }
    }

    public void deleteAPInfosBySsid(HwABSApInfoData data) {
        synchronized (this.mLock) {
            if (data != null) {
                inlineDeleteApInfoBySsid(data.mSsid);
            }
        }
    }

    private void inlineDeleteApInfoBySsid(String ssid) {
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase != null && sQLiteDatabase.isOpen() && ssid != null) {
            this.mDatabase.delete(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, "ssid like ?", new String[]{ssid});
        }
    }

    private void inlineDeleteApInfoByBssid(String bssid) {
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase != null && sQLiteDatabase.isOpen() && bssid != null) {
            this.mDatabase.delete(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, "bssid like ?", new String[]{bssid});
        }
    }

    private void inlineAddApInfo(HwABSApInfoData data) {
        if (data.mBssid != null) {
            this.mDatabase.execSQL("INSERT INTO MIMOApInfoTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{data.mBssid, data.mSsid, Integer.valueOf(data.mSwitch_mimo_type), Integer.valueOf(data.mSwitch_siso_type), Integer.valueOf(data.mAuth_type), Integer.valueOf(data.mIn_black_List), 0, Integer.valueOf(data.mReassociate_times), Integer.valueOf(data.mFailed_times), Integer.valueOf(data.mContinuous_failure_times), Long.valueOf(data.mLast_connect_time), 0});
        }
    }

    private void inlineUpdateApInfo(HwABSApInfoData data) {
        SQLiteDatabase sQLiteDatabase = this.mDatabase;
        if (sQLiteDatabase != null && sQLiteDatabase.isOpen() && data.mBssid != null) {
            HwABSUtils.logD(false, "inlineUpdateApInfo ssid = %{public}s", StringUtilEx.safeDisplaySsid(data.mSsid));
            ContentValues values = new ContentValues();
            values.put("bssid", data.mBssid);
            values.put("ssid", data.mSsid);
            values.put("switch_mimo_type", Integer.valueOf(data.mSwitch_mimo_type));
            values.put("switch_siso_type", Integer.valueOf(data.mSwitch_siso_type));
            values.put("auth_type", Integer.valueOf(data.mAuth_type));
            values.put("in_black_list", Integer.valueOf(data.mIn_black_List));
            values.put("in_vowifi_black_list", (Integer) 0);
            values.put("reassociate_times", Integer.valueOf(data.mReassociate_times));
            values.put("failed_times", Integer.valueOf(data.mFailed_times));
            values.put("continuous_failure_times", Integer.valueOf(data.mContinuous_failure_times));
            values.put("last_connect_time", Long.valueOf(data.mLast_connect_time));
            this.mDatabase.update(HwABSDataBaseHelper.MIMO_AP_TABLE_NAME, values, "bssid like ?", new String[]{data.mBssid});
        }
    }

    private void checkIfAllCaseNumSatisfy() {
        List<HwABSApInfoData> lists = getAllApInfo();
        long last_connect_time = 0;
        String bssid = null;
        boolean isDeleteRecord = false;
        HwABSUtils.logD(false, "checkIfAllCaseNumSatisfy lists.size() = %{public}d", Integer.valueOf(lists.size()));
        if (lists.size() >= 500) {
            isDeleteRecord = true;
            for (HwABSApInfoData data : lists) {
                long current_connect_time = data.mLast_connect_time;
                if (last_connect_time == 0 || last_connect_time > current_connect_time) {
                    last_connect_time = current_connect_time;
                    bssid = data.mBssid;
                }
            }
        }
        if (isDeleteRecord) {
            synchronized (this.mLock) {
                inlineDeleteApInfoByBssid(bssid);
            }
        }
    }

    public List<HwABSApInfoData> getAllApInfo() {
        synchronized (this.mLock) {
            Cursor c = null;
            List<HwABSApInfoData> lists = new ArrayList<>();
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                return lists;
            }
            try {
                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM MIMOApInfoTable", null);
                while (c2.moveToNext()) {
                    lists.add(new HwABSApInfoData(c2.getString(c2.getColumnIndex("bssid")), c2.getString(c2.getColumnIndex("ssid")), c2.getInt(c2.getColumnIndex("switch_mimo_type")), c2.getInt(c2.getColumnIndex("switch_siso_type")), c2.getInt(c2.getColumnIndex("auth_type")), c2.getInt(c2.getColumnIndex("in_black_list")), c2.getInt(c2.getColumnIndex("reassociate_times")), c2.getInt(c2.getColumnIndex("failed_times")), c2.getInt(c2.getColumnIndex("continuous_failure_times")), c2.getLong(c2.getColumnIndex("last_connect_time"))));
                }
                c2.close();
                return lists;
            } catch (SQLException e) {
                HwABSUtils.logE(false, "getAllApInfo:%{public}s", e.getMessage());
                if (0 != 0) {
                    c.close();
                }
                return null;
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
        }
    }

    public HwABSCHRStatistics getCHRStatistics() {
        synchronized (this.mLock) {
            HwABSCHRStatistics statistics = null;
            if (this.mDatabase == null || !this.mDatabase.isOpen()) {
                return null;
            }
            Cursor c = null;
            try {
                Cursor c2 = this.mDatabase.rawQuery("SELECT * FROM StatisticsTable", null);
                if (c2.moveToNext()) {
                    statistics = new HwABSCHRStatistics();
                    statistics.long_connect_event = c2.getInt(c2.getColumnIndex("long_connect_event"));
                    statistics.short_connect_event = c2.getInt(c2.getColumnIndex("short_connect_event"));
                    statistics.search_event = c2.getInt(c2.getColumnIndex("search_event"));
                    statistics.antenna_preempted_screen_on_event = c2.getInt(c2.getColumnIndex("antenna_preempted_screen_on_event"));
                    statistics.antenna_preempted_screen_off_event = c2.getInt(c2.getColumnIndex("antenna_preempted_screen_off_event"));
                    statistics.mo_mt_call_event = c2.getInt(c2.getColumnIndex("mo_mt_call_event"));
                    statistics.siso_to_mimo_event = c2.getInt(c2.getColumnIndex("siso_to_mimo_event"));
                    statistics.ping_pong_times = c2.getInt(c2.getColumnIndex("ping_pong_times"));
                    statistics.max_ping_pong_times = c2.getInt(c2.getColumnIndex("max_ping_pong_times"));
                    statistics.siso_time = (long) c2.getInt(c2.getColumnIndex("siso_time"));
                    statistics.mimo_time = (long) c2.getInt(c2.getColumnIndex("mimo_time"));
                    statistics.mimo_screen_on_time = (long) c2.getInt(c2.getColumnIndex("mimo_screen_on_time"));
                    statistics.siso_screen_on_time = (long) c2.getInt(c2.getColumnIndex("siso_screen_on_time"));
                    statistics.last_upload_time = c2.getLong(c2.getColumnIndex("last_upload_time"));
                    statistics.mRssiL0 = c2.getInt(c2.getColumnIndex("rssiL0"));
                    statistics.mRssiL1 = c2.getInt(c2.getColumnIndex("rssiL1"));
                    statistics.mRssiL2 = c2.getInt(c2.getColumnIndex("rssiL2"));
                    statistics.mRssiL3 = c2.getInt(c2.getColumnIndex("rssiL3"));
                    statistics.mRssiL4 = c2.getInt(c2.getColumnIndex("rssiL4"));
                }
                c2.close();
                return statistics;
            } catch (SQLException e) {
                HwABSUtils.logE(false, "getCHRStatistics: %{public}s", e.getMessage());
                if (0 != 0) {
                    c.close();
                }
                return null;
            } catch (Throwable th) {
                if (0 != 0) {
                    c.close();
                }
                throw th;
            }
        }
    }

    public void inlineAddCHRInfo(HwABSCHRStatistics data) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    this.mDatabase.execSQL("INSERT INTO StatisticsTable VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{Integer.valueOf(data.long_connect_event), Integer.valueOf(data.short_connect_event), Integer.valueOf(data.search_event), Integer.valueOf(data.antenna_preempted_screen_on_event), Integer.valueOf(data.antenna_preempted_screen_off_event), Integer.valueOf(data.mo_mt_call_event), Integer.valueOf(data.siso_to_mimo_event), Integer.valueOf(data.ping_pong_times), Integer.valueOf(data.max_ping_pong_times), Long.valueOf(data.mimo_time), Long.valueOf(data.siso_time), Long.valueOf(data.mimo_screen_on_time), Long.valueOf(data.siso_screen_on_time), Long.valueOf(data.last_upload_time), Integer.valueOf(data.mRssiL0), Integer.valueOf(data.mRssiL1), Integer.valueOf(data.mRssiL2), Integer.valueOf(data.mRssiL3), Integer.valueOf(data.mRssiL4), 0});
                }
            }
        }
    }

    public void inlineUpdateCHRInfo(HwABSCHRStatistics data) {
        synchronized (this.mLock) {
            if (this.mDatabase != null) {
                if (this.mDatabase.isOpen()) {
                    HwABSUtils.logD(false, "inlineUpdateCHRInfo ", new Object[0]);
                    ContentValues values = new ContentValues();
                    values.put("long_connect_event", Integer.valueOf(data.long_connect_event));
                    values.put("short_connect_event", Integer.valueOf(data.short_connect_event));
                    values.put("search_event", Integer.valueOf(data.search_event));
                    values.put("antenna_preempted_screen_on_event", Integer.valueOf(data.antenna_preempted_screen_on_event));
                    values.put("antenna_preempted_screen_off_event", Integer.valueOf(data.antenna_preempted_screen_off_event));
                    values.put("mo_mt_call_event", Integer.valueOf(data.mo_mt_call_event));
                    values.put("siso_to_mimo_event", Integer.valueOf(data.siso_to_mimo_event));
                    values.put("ping_pong_times", Integer.valueOf(data.ping_pong_times));
                    values.put("max_ping_pong_times", Integer.valueOf(data.max_ping_pong_times));
                    values.put("mimo_time", Long.valueOf(data.mimo_time));
                    values.put("siso_time", Long.valueOf(data.siso_time));
                    values.put("mimo_screen_on_time", Long.valueOf(data.mimo_screen_on_time));
                    values.put("siso_screen_on_time", Long.valueOf(data.siso_screen_on_time));
                    values.put("last_upload_time", Long.valueOf(data.last_upload_time));
                    values.put("rssiL0", Integer.valueOf(data.mRssiL0));
                    values.put("rssiL1", Integer.valueOf(data.mRssiL1));
                    values.put("rssiL2", Integer.valueOf(data.mRssiL2));
                    values.put("rssiL3", Integer.valueOf(data.mRssiL3));
                    values.put("rssiL4", Integer.valueOf(data.mRssiL4));
                    this.mDatabase.update(HwABSDataBaseHelper.STATISTICS_TABLE_NAME, values, "_id like ?", new String[]{"1"});
                }
            }
        }
    }
}
