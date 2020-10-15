package com.android.server.hidata.wavemapping.chr.entity;

public class HistAppQoeChrInfo extends ChrInfo {
    public int appName = 0;
    public int dataRx = 0;
    public int dataTx = 0;
    public int duration = 0;
    public int goodCnt = 0;
    public int modelVerAll = 0;
    public int modelVerCell = 0;
    public int modelVerMain = 0;
    public short netFreq = 0;
    public int netIdCnt = 0;
    public String netName = "UNKNOWN";
    public byte netType = 0;
    public int poorCnt = 0;
    public short recordDays = 0;
    public short spaceIdAll = 0;
    public short spaceIdCell = 0;
    public short spaceIdMain = 0;

    public void setSpaceInfo(short spaceIdOfAll, int modelVerOfAll, short spaceIdOfMain, int modelVerOfMain, short spaceIdOfCell, int modelVerOfCell) {
        this.spaceIdAll = spaceIdOfAll;
        this.modelVerAll = modelVerOfAll;
        this.spaceIdMain = spaceIdOfMain;
        this.modelVerMain = modelVerOfMain;
        this.spaceIdCell = spaceIdOfCell;
        this.modelVerCell = modelVerOfCell;
    }

    public void setNetInfo(int netId, String name, short freq, byte type) {
        this.netIdCnt = netId;
        this.netName = name;
        this.netFreq = freq;
        this.netType = type;
    }

    public void setRecords(short days, int dur, int good, int poor, int rx, int tx) {
        this.recordDays = days;
        this.duration = dur;
        this.goodCnt = good;
        this.poorCnt = poor;
        this.dataRx = rx;
        this.dataTx = tx;
    }

    public void setAppName(int app) {
        this.appName = app;
    }

    public String toString() {
        return "HistAppQoeChrInfo{spaceIdAll=" + ((int) this.spaceIdAll) + ", modelVerAll=" + this.modelVerAll + ", spaceIdMain=" + ((int) this.spaceIdMain) + ", modelVerMain=" + this.modelVerMain + ", spaceIdCell=" + ((int) this.spaceIdCell) + ", modelVerCell=" + this.modelVerCell + ", netIdCnt=" + this.netIdCnt + ", netName=" + this.netName + ", netFreq=" + ((int) this.netFreq) + ", netType=" + ((int) this.netType) + ", recordDays=" + ((int) this.recordDays) + ", duration=" + this.duration + ", goodCnt=" + this.goodCnt + ", poorCnt=" + this.poorCnt + ", dataRx=" + this.dataRx + ", dataTx=" + this.dataTx + '}';
    }
}
