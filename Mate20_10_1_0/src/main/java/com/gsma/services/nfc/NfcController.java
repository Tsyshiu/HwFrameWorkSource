package com.gsma.services.nfc;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.cardemulation.NxpAidGroup;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.gsma.services.utils.InsufficientResourcesException;
import com.nxp.nfc.gsma.internal.NxpHandset;
import com.nxp.nfc.gsma.internal.NxpNfcController;
import com.nxp.nfc.gsma.internal.NxpOffHostService;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NfcController {
    public static final int BATTERY_ALL_STATES = 2;
    private static final int BATTERY_OPERATIONAL_MODE = 146;
    public static final int BATTERY_OPERATIONAL_STATE = 1;
    public static final int PROTOCOL_ISO_DEP = 16;
    public static final int SCREEN_ALL_MODES = 2;
    public static final int SCREEN_ON_AND_LOCKED_MODE = 1;
    static final String TAG = "NfcController";
    public static final int TECHNOLOGY_NFC_A = 1;
    public static final int TECHNOLOGY_NFC_B = 2;
    public static final int TECHNOLOGY_NFC_F = 4;
    private static HashMap<Context, NfcController> sNfcController = new HashMap<>();
    /* access modifiers changed from: private */
    public Callbacks mCb;
    private Context mContext;
    private boolean mIsHceCapable;
    private NxpNfcControllerCallback mNxpCallback;
    private NxpHandset mNxpHandset;
    private NxpNfcController mNxpNfcController;
    private ArrayList<OffHostService> mOffHostServiceList;
    private HashMap<String, OffHostService> mOffhostService;
    private int mUserId;

    public interface Callbacks {
        public static final int CARD_EMULATION_DISABLED = 0;
        public static final int CARD_EMULATION_ENABLED = 1;
        public static final int CARD_EMULATION_ERROR = 256;

        void onCardEmulationMode(int i);

        void onEnableNfcController(boolean z);

        void onGetDefaultController(NfcController nfcController);
    }

    NfcController() {
        this.mOffHostServiceList = new ArrayList<>();
        this.mNxpNfcController = null;
        this.mOffhostService = new HashMap<>();
        this.mContext = null;
        this.mNxpHandset = null;
        this.mNxpCallback = null;
        this.mIsHceCapable = false;
        this.mUserId = UserHandle.myUserId();
        this.mNxpHandset = new NxpHandset();
        this.mNxpCallback = new NxpNfcControllerCallback();
        this.mNxpNfcController = new NxpNfcController();
    }

    NfcController(Context context) {
        this.mOffHostServiceList = new ArrayList<>();
        this.mNxpNfcController = null;
        this.mOffhostService = new HashMap<>();
        this.mContext = null;
        this.mNxpHandset = null;
        this.mNxpCallback = null;
        boolean z = false;
        this.mIsHceCapable = false;
        this.mContext = context;
        this.mNxpHandset = new NxpHandset();
        this.mNxpCallback = new NxpNfcControllerCallback();
        this.mNxpNfcController = new NxpNfcController(context);
        this.mUserId = UserHandle.myUserId();
        PackageManager pm = this.mContext.getPackageManager();
        this.mIsHceCapable = (pm.hasSystemFeature("android.hardware.nfc.hce") || pm.hasSystemFeature("android.hardware.nfc.hcef")) ? true : z;
    }

    public class NxpNfcControllerCallback implements NxpNfcController.NxpCallbacks {
        public NxpNfcControllerCallback() {
        }

        public void onNxpEnableNfcController(boolean success) {
            if (success) {
                NfcController.this.mCb.onEnableNfcController(true);
                Log.d(NfcController.TAG, "NFC Enabled");
                return;
            }
            NfcController.this.mCb.onEnableNfcController(false);
            Log.d(NfcController.TAG, "NFC Not Enabled");
        }
    }

    public static void getDefaultController(Context context, Callbacks callbacks) {
        if (context == null || callbacks == null) {
            throw new IllegalArgumentException("context or NfcController.Callbacks cannot be null");
        }
        callbacks.onGetDefaultController(new NfcController(context));
    }

    public boolean isEnabled() {
        return this.mNxpNfcController.isNxpNfcEnabled();
    }

    public void enableNfcController(Callbacks cb) {
        this.mCb = cb;
        if (isEnabled()) {
            this.mCb.onEnableNfcController(true);
        } else {
            this.mNxpNfcController.enableNxpNfcController(this.mNxpCallback);
        }
    }

    @Deprecated
    public boolean isCardEmulationEnabled() throws Exception {
        throw new InsufficientResourcesException("Host Card Emulation (HCE) is supported");
    }

    @Deprecated
    public void enableCardEmulationMode(Callbacks cb) throws IllegalStateException, SecurityException, InsufficientResourcesException {
        if (!isEnabled()) {
            throw new IllegalStateException("Nfc is not enabled");
        } else if (!this.mIsHceCapable) {
            throw new SecurityException("Can not use this API");
        } else {
            throw new InsufficientResourcesException("Host Card Emulation (HCE) is supported");
        }
    }

    @Deprecated
    public void disableCardEmulationMode(Callbacks cb) throws SecurityException, InsufficientResourcesException {
        if (!this.mIsHceCapable) {
            throw new SecurityException("Can not use this API");
        }
        throw new InsufficientResourcesException("Host Card Emulation (HCE) is supported");
    }

    private String getRandomString() {
        return new String("service" + Integer.toString(new SecureRandom().nextInt(10000) + 10000));
    }

    public OffHostService defineOffHostService(String description, String SEName) throws UnsupportedOperationException, IllegalArgumentException {
        String sSEName;
        if (this.mIsHceCapable) {
            Log.d(TAG, "defineOffHostService description=" + description + " SEName=" + SEName);
            if (description == null) {
                throw new IllegalArgumentException("description cannot be null");
            } else if (SEName == null || (!SEName.startsWith("SIM") && !SEName.startsWith("eSE"))) {
                throw new IllegalArgumentException("SEName error");
            } else {
                if (SEName.equals("SIM")) {
                    sSEName = SEName + Integer.toString(1);
                } else {
                    sSEName = SEName;
                }
                new ArrayList(3);
                List<String> secureElementList = this.mNxpHandset.getAvailableSecureElements(146);
                if (secureElementList.size() > 0) {
                    int i = 0;
                    while (i < secureElementList.size() && !secureElementList.get(i).equals(sSEName)) {
                        i++;
                    }
                    if (i == secureElementList.size()) {
                        throw new IllegalArgumentException("Invalid SEName provided");
                    }
                }
                NxpOffHostService offHostService = new NxpOffHostService(this.mUserId, description, SEName, this.mContext.getPackageName(), TextUtils.isEmpty(description) ? getRandomString() : description, true);
                Log.d(TAG, "defineOffHostService, new offHostService is " + offHostService.toString());
                offHostService.setContext(this.mContext);
                offHostService.setNxpNfcController(this.mNxpNfcController);
                return new OffHostService(offHostService);
            }
        } else {
            throw new UnsupportedOperationException("HCE is not supported");
        }
    }

    public void deleteOffHostService(OffHostService service) throws IllegalArgumentException, UnsupportedOperationException {
        String packageName = this.mContext.getPackageName();
        if (service == null) {
            throw new IllegalArgumentException("Invalid service provided");
        } else if (!this.mNxpNfcController.isStaticOffhostService(this.mUserId, packageName, convertToNxpOffhostService(service))) {
            this.mNxpNfcController.deleteOffHostService(this.mUserId, packageName, convertToNxpOffhostService(service));
        } else {
            throw new UnsupportedOperationException("Service has been defined in Manifest and cannot be deleted");
        }
    }

    public OffHostService[] getOffHostServices() {
        String packageName = this.mContext.getPackageName();
        Log.d(TAG, "getOffHostServices packageName=" + packageName);
        ArrayList<NxpOffHostService> mNxpOffhost = this.mNxpNfcController.getOffHostServices(this.mUserId, packageName);
        if (mNxpOffhost == null || mNxpOffhost.isEmpty()) {
            return null;
        }
        ArrayList<OffHostService> mOffHostList = new ArrayList<>();
        Iterator<NxpOffHostService> it = mNxpOffhost.iterator();
        while (it.hasNext()) {
            mOffHostList.add(new OffHostService(it.next()));
        }
        return (OffHostService[]) mOffHostList.toArray(new OffHostService[mOffHostList.size()]);
    }

    public OffHostService getDefaultOffHostService() {
        Log.d(TAG, "getDefaultOffHostService begin");
        NxpOffHostService service = this.mNxpNfcController.getDefaultOffHostService(this.mUserId, this.mContext.getPackageName());
        if (service != null) {
            Log.d(TAG, "service != null getDefaultOffHostService end");
            return new OffHostService(service);
        }
        Log.d(TAG, "getDefaultOffHostService service is null");
        return null;
    }

    private ArrayList<NxpAidGroup> convertToCeAidGroupList(List<AidGroup> mAidGroups) {
        ArrayList<NxpAidGroup> mApduAidGroupList = new ArrayList<>();
        new ArrayList();
        for (AidGroup mGroup : mAidGroups) {
            if (!mGroup.getAidList().isEmpty()) {
                NxpAidGroup mCeAidGroup = new NxpAidGroup(mGroup.getCategory(), mGroup.getDescription());
                List<String> aidList = mCeAidGroup.getAids();
                for (String aid : mGroup.getAidList()) {
                    aidList.add(aid);
                }
                mApduAidGroupList.add(mCeAidGroup);
            }
        }
        return mApduAidGroupList;
    }

    private NxpOffHostService convertToNxpOffhostService(OffHostService service) {
        ArrayList<NxpAidGroup> mAidGroupList = convertToCeAidGroupList(service.mAidGroupList);
        NxpOffHostService mNxpOffHostService = new NxpOffHostService(service.mUserId, service.mDescription, service.mSEName, service.mPackageName, service.mServiceName, service.mModifiable);
        mNxpOffHostService.setBanner(service.mBanner);
        mNxpOffHostService.setContext(this.mContext);
        mNxpOffHostService.setBannerId(service.mBannerResId);
        mNxpOffHostService.mNxpAidGroupList.addAll(mAidGroupList);
        return mNxpOffHostService;
    }
}
