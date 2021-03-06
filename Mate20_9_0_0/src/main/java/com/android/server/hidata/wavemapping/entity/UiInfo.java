package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.wifipro.PortalDbHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class UiInfo {
    private String cluster_num = "";
    private int fg_fingers_num = 20;
    private int finger_batch_num = 0;
    private String jsonResult = "";
    private int knnMaxDist = 200;
    private float maxDist = 100.0f;
    private String preLabel = "";
    private String ssid = "";
    private int stage = 3;
    private String toast = "";

    public UiInfo() {
        ParameterInfo param = ParamManager.getInstance().getParameterInfo();
        this.fg_fingers_num = param.getFg_batch_num();
        this.maxDist = param.getMaxDist();
        this.knnMaxDist = param.getKnnMaxDist();
    }

    public String getToast() {
        return this.toast;
    }

    public void setToast(String toast) {
        this.toast = toast;
    }

    public String getPreLabel() {
        return this.preLabel;
    }

    public void setPreLabel(String preLabel) {
        this.preLabel = preLabel;
    }

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getStage() {
        return this.stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int getFinger_batch_num() {
        return this.finger_batch_num;
    }

    public void setFinger_batch_num(int finger_batch_num) {
        this.finger_batch_num = finger_batch_num;
    }

    public String getCluster_num() {
        return this.cluster_num;
    }

    public void setCluster_num(String cluster_num) {
        this.cluster_num = cluster_num;
    }

    public int getFg_fingers_num() {
        return this.fg_fingers_num;
    }

    public void setFg_fingers_num(int fg_fingers_num) {
        this.fg_fingers_num = fg_fingers_num;
    }

    public float getMaxDist() {
        return this.maxDist;
    }

    public void setMaxDist(float maxDist) {
        this.maxDist = maxDist;
    }

    public int getKnnMaxDist() {
        return this.knnMaxDist;
    }

    public void setKnnMaxDist(int knnMaxDist) {
        this.knnMaxDist = knnMaxDist;
    }

    public String toJsonStr() {
        JSONObject jsonParam = new JSONObject();
        try {
            jsonParam.put("stage", this.stage);
            jsonParam.put("finger_batch_num", this.finger_batch_num);
            jsonParam.put("cluster_num", this.cluster_num);
            jsonParam.put("fg_fingers_num", this.fg_fingers_num);
            jsonParam.put("maxDist", (double) this.maxDist);
            jsonParam.put("knnMaxDist", this.knnMaxDist);
            jsonParam.put(PortalDbHelper.ITEM_SSID, this.ssid);
            jsonParam.put("preLabel", this.preLabel);
            jsonParam.put("toast", this.toast);
        } catch (JSONException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("toJsonStr,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        this.jsonResult = jsonParam.toString();
        if (this.jsonResult == null) {
            this.jsonResult = "";
        }
        return this.jsonResult;
    }
}
