package com.android.server.rms.iaware.appmng;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import com.android.internal.os.SomeArgs;
import com.android.server.hidata.wavemapping.modelservice.ModelBaseService;
import com.android.server.rms.iaware.appmng.AwareWakeUpManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AlarmManagerDumpRadar {
    public static final int EVENT_CONTROLED = 2;
    public static final int EVENT_OVERLOAD = 1;
    public static final int EVENT_WAKEUP = 0;
    private static final int MSG_ALARM_EVENT = 1;
    private static final String TAG = "AlarmManagerDumpRadar";
    private static final int USER_OTHER = 1;
    private static final boolean isBetaUser = (AwareConstant.CURRENT_USER_TYPE == 3);
    private static volatile AlarmManagerDumpRadar mAlarmManagerDumpRadar;
    private HashMap<Integer, HashMap<String, PackageInfo>> mAlarmWakeupInfo = new HashMap<>();
    private long mCleanupTime = System.currentTimeMillis();
    private Handler mHandler;
    private AtomicInteger mSystemWakeupCount = new AtomicInteger(0);
    private AtomicInteger mSystemWakeupOverloadCount = new AtomicInteger(0);

    public static AlarmManagerDumpRadar getInstance() {
        if (mAlarmManagerDumpRadar == null) {
            synchronized (AlarmManagerDumpRadar.class) {
                if (mAlarmManagerDumpRadar == null) {
                    mAlarmManagerDumpRadar = new AlarmManagerDumpRadar();
                }
            }
        }
        return mAlarmManagerDumpRadar;
    }

    public void setHandler(Handler handler) {
        if (handler != null) {
            this.mHandler = new AlarmRadarHandler(handler.getLooper());
        }
    }

    private final class AlarmRadarHandler extends Handler {
        protected AlarmRadarHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                SomeArgs args = (SomeArgs) msg.obj;
                String tag = (String) args.arg2;
                AwareWakeUpManager.ControlType policy = (AwareWakeUpManager.ControlType) args.arg3;
                int type = args.argi1;
                int uid = args.argi2;
                AlarmManagerDumpRadar.this.handleAlarmEvent(type, uid, (String) args.arg1, tag, policy);
            }
        }
    }

    protected static class PackageInfo {
        protected int mOverloadCount = 0;
        protected String mPkg;
        protected HashMap<String, TagInfo> mTagMap = new HashMap<>();
        protected int mWakeupCount = 0;

        protected PackageInfo(String pkg) {
            this.mPkg = pkg;
        }
    }

    protected static class TagInfo {
        protected int mDecideOverloadCount = 0;
        protected int mExtendCount = 0;
        protected int mMuteCount = 0;
        protected int mOverloadCount = 0;
        protected int mPerceptibleCount = 0;
        protected String mTag;
        protected int mTopNCount = 0;
        protected int mUnknownCount = 0;
        protected int mWakeupCount = 0;
        protected int mWhiteListCount = 0;

        protected TagInfo(String tag) {
            this.mTag = tag;
        }
    }

    /* JADX INFO: Multiple debug info for r7v5 org.json.JSONArray: [D('pkgData' org.json.JSONArray), D('sysTitle' org.json.JSONArray)] */
    public String saveBigData(boolean clear) {
        StringBuilder data = new StringBuilder();
        data.append("\n[iAwareAlarmManager_Start]");
        data.append("\nstartTime: ");
        data.append(String.valueOf(this.mCleanupTime));
        data.append("\n");
        JSONObject bigData = new JSONObject();
        long currentTime = System.currentTimeMillis();
        synchronized (AlarmManagerDumpRadar.class) {
            try {
                JSONObject title = new JSONObject();
                JSONArray sysTitle = new JSONArray();
                sysTitle.put("wakeup_count");
                sysTitle.put("overload_count");
                title.put("sys", sysTitle);
                JSONArray pkgTitle = new JSONArray();
                pkgTitle.put("userid");
                pkgTitle.put("wakeup_count");
                pkgTitle.put("overload_count");
                title.put("pkg", pkgTitle);
                JSONArray tagTitle = new JSONArray();
                tagTitle.put("wakeup_count");
                tagTitle.put("overload_count");
                tagTitle.put("extend_count");
                tagTitle.put("mute_count");
                tagTitle.put("perceptible");
                tagTitle.put(ModelBaseService.UNKONW_IDENTIFY_RET);
                tagTitle.put("topn");
                tagTitle.put("decide_overload");
                tagTitle.put("white_list");
                title.put("tag", tagTitle);
                bigData.put("title", title);
                JSONArray systemData = new JSONArray();
                systemData.put(this.mSystemWakeupCount);
                systemData.put(this.mSystemWakeupOverloadCount);
                bigData.put("sys", systemData);
                JSONArray alarmData = new JSONArray();
                for (Map.Entry<Integer, HashMap<String, PackageInfo>> userEntry : this.mAlarmWakeupInfo.entrySet()) {
                    for (Map.Entry<String, PackageInfo> pkgEntry : userEntry.getValue().entrySet()) {
                        JSONObject alarmDataItem = new JSONObject();
                        JSONArray pkgData = new JSONArray();
                        pkgData.put(userEntry.getKey());
                        PackageInfo packageInfo = pkgEntry.getValue();
                        pkgData.put(packageInfo.mWakeupCount);
                        pkgData.put(packageInfo.mOverloadCount);
                        alarmDataItem.put(packageInfo.mPkg, pkgData);
                        for (Iterator<Map.Entry<String, TagInfo>> it = packageInfo.mTagMap.entrySet().iterator(); it.hasNext(); it = it) {
                            TagInfo tagInfo = it.next().getValue();
                            JSONArray tagData = new JSONArray();
                            tagData.put(tagInfo.mWakeupCount);
                            tagData.put(tagInfo.mOverloadCount);
                            tagData.put(tagInfo.mExtendCount);
                            tagData.put(tagInfo.mMuteCount);
                            tagData.put(tagInfo.mPerceptibleCount);
                            tagData.put(tagInfo.mUnknownCount);
                            tagData.put(tagInfo.mTopNCount);
                            tagData.put(tagInfo.mDecideOverloadCount);
                            tagData.put(tagInfo.mWhiteListCount);
                            alarmDataItem.put(tagInfo.mTag, tagData);
                            packageInfo = packageInfo;
                            pkgData = pkgData;
                        }
                        alarmData.put(alarmDataItem);
                        title = title;
                        pkgTitle = pkgTitle;
                        tagTitle = tagTitle;
                        sysTitle = sysTitle;
                    }
                }
                bigData.put("alarm", alarmData);
            } catch (JSONException e) {
                AwareLog.e(TAG, "saveBigdata failed! catch JSONException e :" + e.toString());
            }
            if (!AwareWakeUpManager.getInstance().isDebugMode() && clear) {
                this.mAlarmWakeupInfo = new HashMap<>();
                this.mSystemWakeupOverloadCount.set(0);
                this.mSystemWakeupCount.set(0);
                this.mCleanupTime = currentTime;
            }
        }
        data.append(bigData.toString());
        data.append("\nendTime: ");
        data.append(String.valueOf(currentTime));
        data.append("\n[iAwareAlarmManager_End]");
        return data.toString();
    }

    public void reportSystemEvent(int type) {
        if (isBetaUser && !AwareWakeUpManager.getInstance().isScreenOn() && this.mHandler != null) {
            if (type == 0) {
                this.mSystemWakeupCount.incrementAndGet();
            } else if (type == 1) {
                this.mSystemWakeupOverloadCount.incrementAndGet();
            }
        }
    }

    public void reportAlarmEvent(int type, int uid, String pkg, String tag, AwareWakeUpManager.ControlType policy) {
        Handler handler;
        if (isBetaUser && !AwareWakeUpManager.getInstance().isScreenOn() && (handler = this.mHandler) != null) {
            Message msg = handler.obtainMessage();
            msg.what = 1;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = pkg;
            args.arg2 = tag;
            args.arg3 = policy;
            args.argi1 = type;
            args.argi2 = uid;
            msg.obj = args;
            this.mHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public void handleAlarmEvent(int type, int uid, String pkg, String tag, AwareWakeUpManager.ControlType policy) {
        int userId = UserHandle.getUserId(uid);
        if (userId != 0) {
            userId = 1;
        }
        synchronized (AlarmManagerDumpRadar.class) {
            HashMap<String, PackageInfo> packageInfos = this.mAlarmWakeupInfo.get(Integer.valueOf(userId));
            if (packageInfos == null) {
                packageInfos = new HashMap<>();
            }
            PackageInfo packageInfo = packageInfos.get(pkg);
            if (packageInfo == null) {
                packageInfo = new PackageInfo(pkg);
            }
            TagInfo tagInfo = null;
            if (tag != null && (tagInfo = packageInfo.mTagMap.get(tag)) == null) {
                tagInfo = new TagInfo(tag);
            }
            updateData(type, packageInfo, tagInfo, policy);
            if (tagInfo != null) {
                packageInfo.mTagMap.put(tag, tagInfo);
            }
            packageInfos.put(pkg, packageInfo);
            this.mAlarmWakeupInfo.put(Integer.valueOf(userId), packageInfos);
        }
    }

    private void updateData(int type, PackageInfo packageInfo, TagInfo tagInfo, AwareWakeUpManager.ControlType policy) {
        if (type == 0) {
            packageInfo.mWakeupCount++;
            if (tagInfo != null) {
                tagInfo.mWakeupCount++;
            }
        } else if (type != 1) {
            if (type == 2 && tagInfo != null) {
                switch (policy) {
                    case PERCEPTIBLE:
                        tagInfo.mPerceptibleCount++;
                        return;
                    case UNKNOWN:
                        tagInfo.mUnknownCount++;
                        return;
                    case EXTEND:
                        tagInfo.mExtendCount++;
                        return;
                    case EXTEND_TOPN:
                        tagInfo.mTopNCount++;
                        return;
                    case EXTEND_AND_MUTE:
                        tagInfo.mMuteCount++;
                        return;
                    case DECIDE_OVERLOAD:
                        tagInfo.mDecideOverloadCount++;
                        return;
                    case IMPORTANT:
                        tagInfo.mWhiteListCount++;
                        return;
                    default:
                        return;
                }
            }
        } else if (tagInfo != null) {
            tagInfo.mOverloadCount++;
        } else {
            packageInfo.mOverloadCount++;
        }
    }
}
