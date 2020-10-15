package com.android.server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.drm.DrmErrorEvent;
import android.drm.DrmInfo;
import android.drm.DrmManagerClient;
import android.net.Uri;
import android.util.Log;
import android.util.Slog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class HWDrmDialogsService {
    private static final int MSG_SHOW_CD_NORIGHTS_DLG = 0;
    private static final int MSG_SHOW_NO_SECURITY_DLG = 2;
    private static final int MSG_SHOW_SD_RENEWAL_DLG = 1;
    private static final String TAG = "HWDrmDialogsService";
    private AlertDialog mAlertDlg = null;
    /* access modifiers changed from: private */
    public Context mContext;
    private DrmManagerClient mDrmManagerClient;
    /* access modifiers changed from: private */
    public String mRightsIssuer = "";
    /* access modifiers changed from: private */
    public Toast mToast = null;

    public HWDrmDialogsService(Context context) {
        this.mContext = context;
    }

    public void start() {
        CreateDrmErrorListener();
    }

    private void CreateDrmErrorListener() {
        this.mDrmManagerClient = new DrmManagerClient(this.mContext);
        DrmInfo drmInfo = new DrmInfo(1, new byte[]{0}, "application/vnd.oma.drm.content");
        drmInfo.put("DialogServiceRegister", "true");
        this.mDrmManagerClient.processDrmInfo(drmInfo);
        DrmManagerClient.OnErrorListener errorListener = new DrmManagerClient.OnErrorListener() {
            /* class com.android.server.HWDrmDialogsService.AnonymousClass1 */

            public void onError(DrmManagerClient client, DrmErrorEvent event) {
                String errStr = event.getMessage();
                if (errStr == null) {
                    errStr = "";
                }
                Slog.i(HWDrmDialogsService.TAG, "HWDrmDialogsService  start ........");
                Slog.i(HWDrmDialogsService.TAG, "errStr = " + errStr);
                if (2002 == event.getType()) {
                    Slog.i(HWDrmDialogsService.TAG, "cdNoRights ........");
                    if (errStr.startsWith("showdialog#")) {
                        HWDrmDialogsService.this.showDlg(0);
                    }
                } else if (2001 == event.getType()) {
                    Slog.i(HWDrmDialogsService.TAG, "rightIssuer error ........");
                    if (errStr.startsWith("showdialog#")) {
                        String unused = HWDrmDialogsService.this.mRightsIssuer = errStr.substring("showdialog#".length());
                        Slog.i(HWDrmDialogsService.TAG, "rightIssuer error : rightIssuer = " + HWDrmDialogsService.this.mRightsIssuer);
                        HWDrmDialogsService.this.showDlg(1);
                    }
                } else if (2005 == event.getType() && errStr.startsWith("showdialog#")) {
                    HWDrmDialogsService.this.showDlg(2);
                }
            }
        };
        this.mDrmManagerClient.setOnErrorListener(errorListener);
        Slog.i(TAG, "HWDrmDialogsService  errorListener = " + errorListener);
    }

    /* access modifiers changed from: private */
    public void showDlg(int dialog) {
        if (dialog == 0) {
            Toast toast = this.mToast;
            if (toast == null) {
                this.mToast = Toast.makeText(this.mContext, 33685717, 1);
            } else {
                toast.setText(33685717);
            }
            this.mToast.show();
        } else if (dialog != 1) {
            if (dialog != 2) {
                Log.e(TAG, "showDlg default");
                return;
            }
            Toast toast2 = this.mToast;
            if (toast2 == null) {
                this.mToast = Toast.makeText(this.mContext, 33685719, 1);
            } else {
                toast2.setText(33685719);
            }
            this.mToast.show();
        } else if (!"".equals(this.mRightsIssuer)) {
            AlertDialog alertDialog = this.mAlertDlg;
            if (alertDialog == null || !alertDialog.isShowing()) {
                this.mAlertDlg = new AlertDialog.Builder(this.mContext, 33947691).setCancelable(false).setTitle(17039380).setMessage(33685651).setPositiveButton(17039370, new DialogInterface.OnClickListener() {
                    /* class com.android.server.HWDrmDialogsService.AnonymousClass3 */

                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (HWDrmDialogsService.this.mRightsIssuer.matches("http://.*") || HWDrmDialogsService.this.mRightsIssuer.matches("https://.*")) {
                            HWDrmDialogsService hWDrmDialogsService = HWDrmDialogsService.this;
                            hWDrmDialogsService.startBrowser(hWDrmDialogsService.mRightsIssuer);
                            return;
                        }
                        if (HWDrmDialogsService.this.mToast == null) {
                            HWDrmDialogsService hWDrmDialogsService2 = HWDrmDialogsService.this;
                            Toast unused = hWDrmDialogsService2.mToast = Toast.makeText(hWDrmDialogsService2.mContext, 33685718, 1);
                        } else {
                            HWDrmDialogsService.this.mToast.setText(33685718);
                        }
                        HWDrmDialogsService.this.mToast.show();
                    }
                }).setNegativeButton(17039360, new DialogInterface.OnClickListener() {
                    /* class com.android.server.HWDrmDialogsService.AnonymousClass2 */

                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create();
                Window window = this.mAlertDlg.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.type = 2003;
                    window.setAttributes(lp);
                }
                this.mAlertDlg.show();
                return;
            }
            Log.d(TAG, "MSG_SHOW_SD_RENEWAL_DLG dailog has show once");
        } else {
            Toast toast3 = this.mToast;
            if (toast3 == null) {
                this.mToast = Toast.makeText(this.mContext, 33685716, 1);
            } else {
                toast3.setText(33685716);
            }
            this.mToast.show();
        }
    }

    /* access modifiers changed from: private */
    public void startBrowser(String rightIssuer) {
        Uri rightsUrl = Uri.parse(rightIssuer);
        Intent intent = new Intent();
        intent.setFlags(268435456);
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.BROWSABLE");
        intent.setData(rightsUrl);
        this.mContext.startActivity(intent);
    }
}
