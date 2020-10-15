package com.android.server.rms.iaware.srms;

import android.app.mtm.iaware.appmng.AppMngConstant;
import android.content.Context;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.mtm.MultiTaskManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.mtm.iaware.appmng.rule.ListItem;
import com.android.server.mtm.iaware.srms.AwareBroadcastDumpRadar;
import com.android.server.mtm.iaware.srms.AwareBroadcastSend;
import com.android.server.rms.iaware.IRDataRegister;
import com.android.server.rms.iaware.feature.RFeature;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BroadcastExFeature extends RFeature {
    public static final String BR_FILTER_BRAPPBACKLIST = "filter_blackbrapp_list";
    public static final String BR_FILTER_BRAPPWHITELIST = "filter_whitebrapp_list";
    public static final String BR_FILTER_SWITCH = "filterSwitch";
    public static final String BR_FILTER_WHITELIST = "filter_white_list";
    public static final String BR_GOOGLE_APP_LIST = "br_google_app";
    public static final String BR_SEND_SWITCH = "SendSwitch";
    public static final int FILTER_SWITCH = 1;
    private static final int PARSE_LIST_LENGTH = 2;
    public static final int SEND_SWITCH = 2;
    private static final String TAG = "BroadcastExFeature";
    private static final int VERSION = 3;
    private static final ArrayMap<String, ArraySet<String>> mBrFilterBlackApp = new ArrayMap<>();
    private static final HashMap<String, Integer> mBrFilterData = new HashMap<>();
    private static final ArrayMap<String, ArraySet<String>> mBrFilterWhiteApp = new ArrayMap<>();
    private static final ArraySet<String> mBrFilterWhiteList = new ArraySet<>();
    private static final ArraySet<String> mBrGoogleAppList = new ArraySet<>();
    private static boolean mBroadcastFilterEnable = false;
    private static boolean mBroadcastSendEnable = false;
    private static boolean mFeature = false;
    private static AtomicBoolean mIsInitialized = new AtomicBoolean(false);
    private AwareBroadcastDumpRadar mDumpRadar = null;

    public BroadcastExFeature(Context context, AwareConstant.FeatureType featureType, IRDataRegister dataRegister) {
        super(context, featureType, dataRegister);
        initConfig();
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enable() {
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean disable() {
        AwareLog.i(TAG, "BroadcastExFeature disable");
        setEnable(false);
        return true;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean enableFeatureEx(int realVersion) {
        if (realVersion >= 3) {
            AwareLog.i(TAG, "BroadcastExFeature 3.0 enableFeatureEx");
            AwareBroadcastSend.getInstance().updateConfigData();
            setEnable(true);
            return true;
        }
        AwareLog.i(TAG, "enableFeatureEx failed, realVersion: " + realVersion + ", BroadcastExFeature Version: " + 3);
        return false;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public boolean reportData(CollectData data) {
        return false;
    }

    private static void setEnable(boolean enable) {
        mFeature = enable;
    }

    @Override // com.android.server.rms.iaware.feature.RFeature
    public String getDFTDataByVersion(int iawareVer, boolean forBeta, boolean clearData, boolean betaEncode) {
        if (iawareVer < 3) {
            AwareLog.i(TAG, "Feature based on IAware3.0, getBigDataByVersion return null. iawareVer: " + iawareVer);
        } else if (!mFeature) {
            AwareLog.e(TAG, "Broadcast feature is disabled, it is invalid operation to save big data.");
            return null;
        } else if (getDumpRadar() != null) {
            return getDumpRadar().getDFTData(forBeta, clearData, betaEncode);
        }
        return null;
    }

    private AwareBroadcastDumpRadar getDumpRadar() {
        if (MultiTaskManagerService.self() != null) {
            this.mDumpRadar = MultiTaskManagerService.self().getIawareBrRadar();
        }
        return this.mDumpRadar;
    }

    public static boolean isFeatureEnabled(int type) {
        return type == 1 ? mFeature && mBroadcastFilterEnable : type == 2 && mFeature && mBroadcastSendEnable;
    }

    private void initConfig() {
        if (!mIsInitialized.get()) {
            mIsInitialized.set(true);
            updateConfig();
        }
    }

    public static void updateConfig() {
        ArrayList<String> filter = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_FILTER_SWITCH);
        if (filter != null && filter.size() == 1) {
            mBroadcastFilterEnable = switchOn(filter.get(0));
        }
        ArrayList<String> filter2 = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_SEND_SWITCH);
        if (filter2 != null && filter2.size() > 0) {
            mBroadcastSendEnable = switchOn(filter2.get(0));
        }
        getBrList();
        getBrDataPolicy();
    }

    private static boolean switchOn(String value) {
        try {
            if (Integer.parseInt(value) == 1) {
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            AwareLog.e(TAG, "iaware_brEx value format error");
            return false;
        }
    }

    private static void getBrList() {
        ArrayList<String> whiteFilterList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_FILTER_WHITELIST);
        if (whiteFilterList != null) {
            synchronized (mBrFilterWhiteList) {
                mBrFilterWhiteList.clear();
                int size = whiteFilterList.size();
                for (int index = 0; index < size; index++) {
                    mBrFilterWhiteList.add(whiteFilterList.get(index));
                }
            }
        }
        ArrayList<String> backBrAppFilterList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_FILTER_BRAPPBACKLIST);
        if (backBrAppFilterList != null) {
            ArrayMap<String, ArraySet<String>> filterBackBrApp = parseResult(backBrAppFilterList);
            synchronized (mBrFilterBlackApp) {
                mBrFilterBlackApp.clear();
                mBrFilterBlackApp.putAll((ArrayMap<? extends String, ? extends ArraySet<String>>) filterBackBrApp);
            }
        }
        ArrayList<String> whiteBrAppFilterList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_FILTER_BRAPPWHITELIST);
        if (whiteBrAppFilterList != null) {
            ArrayMap<String, ArraySet<String>> filterWhiteBrApp = parseResult(whiteBrAppFilterList);
            synchronized (mBrFilterWhiteApp) {
                mBrFilterWhiteApp.clear();
                mBrFilterWhiteApp.putAll((ArrayMap<? extends String, ? extends ArraySet<String>>) filterWhiteBrApp);
            }
        }
        ArrayList<String> googleAppList = DecisionMaker.getInstance().getRawConfig(AppMngConstant.AppMngFeature.BROADCAST.getDesc(), BR_GOOGLE_APP_LIST);
        if (googleAppList != null) {
            synchronized (mBrGoogleAppList) {
                mBrGoogleAppList.clear();
                int size2 = googleAppList.size();
                for (int index2 = 0; index2 < size2; index2++) {
                    mBrGoogleAppList.add(googleAppList.get(index2));
                }
            }
        }
    }

    private static ArrayMap<String, ArraySet<String>> parseResult(ArrayList<String> results) {
        ArrayMap<String, ArraySet<String>> parseResult = new ArrayMap<>();
        int size = results.size();
        for (int index = 0; index < size; index++) {
            String[] contentArray = results.get(index).split(AwarenessInnerConstants.COLON_KEY);
            if (contentArray.length == 2) {
                ArraySet<String> apps = new ArraySet<>();
                String action = contentArray[0].trim();
                for (String str : contentArray[1].trim().split(";")) {
                    apps.add(str.trim());
                }
                parseResult.put(action, apps);
            } else {
                AwareLog.e(TAG, "iaware_brEx value format error");
            }
        }
        return parseResult;
    }

    private static void getBrDataPolicy() {
        ArrayMap<String, ListItem> filterBrDatas = DecisionMaker.getInstance().getBrListItem(AppMngConstant.AppMngFeature.BROADCAST, AppMngConstant.BroadcastSource.BROADCAST_FILTER);
        if (filterBrDatas != null) {
            synchronized (mBrFilterData) {
                mBrFilterData.clear();
                for (Map.Entry<String, ListItem> ent : filterBrDatas.entrySet()) {
                    String action = ent.getKey();
                    ListItem item = ent.getValue();
                    if (!(action == null || item == null)) {
                        mBrFilterData.put(action, Integer.valueOf(item.getPolicy()));
                    }
                }
            }
        }
    }

    public static boolean isBrFilterWhiteList(String pkgName) {
        boolean contains;
        synchronized (mBrFilterWhiteList) {
            contains = mBrFilterWhiteList.contains(pkgName);
        }
        return contains;
    }

    public static ArraySet<String> getBrFilterWhiteList() {
        ArraySet<String> whiteList = new ArraySet<>();
        synchronized (mBrFilterWhiteList) {
            whiteList.addAll((ArraySet<? extends String>) mBrFilterWhiteList);
        }
        return whiteList;
    }

    public static int getBrFilterPolicy(String action) {
        synchronized (mBrFilterData) {
            Integer policy = mBrFilterData.get(action);
            if (policy == null) {
                return -1;
            }
            return policy.intValue();
        }
    }

    public static boolean isBrFilterBlackApp(String action, String pkgName) {
        synchronized (mBrFilterBlackApp) {
            ArraySet<String> apps = mBrFilterBlackApp.get(action);
            if (apps == null) {
                return false;
            }
            return apps.contains(pkgName);
        }
    }

    public static ArrayMap<String, ArraySet<String>> getBrFilterBlackApp() {
        ArrayMap<String, ArraySet<String>> arrayMap;
        synchronized (mBrFilterBlackApp) {
            arrayMap = new ArrayMap<>(mBrFilterBlackApp);
        }
        return arrayMap;
    }

    public static boolean isBrFilterWhiteApp(String action, String pkgName) {
        synchronized (mBrFilterWhiteApp) {
            ArraySet<String> apps = mBrFilterWhiteApp.get(action);
            if (apps == null) {
                return false;
            }
            return apps.contains(pkgName);
        }
    }

    public static ArrayMap<String, ArraySet<String>> getBrFilterWhiteApp() {
        ArrayMap<String, ArraySet<String>> arrayMap;
        synchronized (mBrFilterWhiteApp) {
            arrayMap = new ArrayMap<>(mBrFilterWhiteApp);
        }
        return arrayMap;
    }

    public static ArraySet<String> getBrGoogleAppList() {
        ArraySet<String> googleAppList = new ArraySet<>();
        synchronized (mBrGoogleAppList) {
            googleAppList.addAll((ArraySet<? extends String>) mBrGoogleAppList);
        }
        return googleAppList;
    }

    public static boolean isBrGoogleApp(String pkgName) {
        boolean contains;
        synchronized (mBrGoogleAppList) {
            contains = mBrGoogleAppList.contains(pkgName);
        }
        return contains;
    }
}
