package com.android.server;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;

public class HwAutoUpdate {
    private static final String COMMOND_FILE = "/cache/recovery/command";
    private static final int SAFE_BATTERY_LEVEL = 50;
    private static final String TAG = "HwAutoUpdate";
    private static final String UPDATE_POWEROFF = "update_poweroff";
    private static final String UPDATE_REBOOT = "update_reboot";
    private static HwAutoUpdate mInstance = null;

    public static HwAutoUpdate getInstance() {
        if (mInstance == null) {
            mInstance = new HwAutoUpdate();
        }
        return mInstance;
    }

    public boolean isAutoSystemUpdate(Context context, boolean reboot) {
        int automatic = Settings.Global.getInt(context.getContentResolver(), "ota_disable_automatic_update", 0);
        Log.i(TAG, "automatic switch " + automatic);
        if (automatic != 0) {
            return false;
        }
        if (getBatteryLevelValue(context) > 50) {
            AutoupdateCommand autoupdateCommand = getCommand(COMMOND_FILE);
            Log.d(TAG, "autoupdateCommand : " + autoupdateCommand.toString());
            if (autoupdateCommand.isAutoUpdate() && isUserFreeSizeEnough(autoupdateCommand)) {
                StringBuilder reboot_reason = new StringBuilder();
                reboot_reason.append("--reboot");
                reboot_reason.append("=");
                reboot_reason.append(reboot ? UPDATE_REBOOT : UPDATE_POWEROFF);
                writeRebootReason(COMMOND_FILE, reboot_reason.toString());
                return true;
            }
        }
        Log.d(TAG, "current batteryLevel: " + getBatteryLevelValue(context) + " <= safe value(50%), return");
        return false;
    }

    private static class AutoupdateCommand {
        static final String KEY_AUTOUPDATE = "--autoupdate";
        static final String KEY_REBOOT = "--reboot";
        static final String KEY_UPDATEPACKAGE = "--update_package";
        static final String KEY_USER_FREESIZE = "--user_freesize";
        private String autoupdate;
        private String reboot;
        private String update_package;
        private long user_freesize;

        private AutoupdateCommand() {
            this.user_freesize = 0;
        }

        public void setUpdatePackage(String update_package2) {
            this.update_package = update_package2.trim();
        }

        public void setAutoDate(String autoupdate2) {
            this.autoupdate = autoupdate2.trim();
        }

        public void setReboot(String reboot2) {
            this.reboot = reboot2.trim();
        }

        public void setUserFreeSize(String user_freesize2) {
            try {
                this.user_freesize = Long.parseLong(user_freesize2.trim());
            } catch (NumberFormatException e) {
                Log.i(HwAutoUpdate.TAG, "auto update command parse user_freesize error");
                this.user_freesize = -1;
            }
        }

        public String getUpdatePackage() {
            return this.update_package;
        }

        public boolean isAutoUpdate() {
            String str = this.autoupdate;
            if (str == null) {
                return false;
            }
            return "TRUE".equals(str.toUpperCase(Locale.ENGLISH));
        }

        public String getReboot() {
            return this.reboot;
        }

        public long getUserFreeSize() {
            return this.user_freesize;
        }

        public String toString() {
            StringBuilder command = new StringBuilder();
            if (!TextUtils.isEmpty(this.update_package)) {
                command.append(KEY_UPDATEPACKAGE);
                command.append("=");
                command.append(this.update_package);
                command.append("\r\n");
            }
            if (!TextUtils.isEmpty(this.autoupdate)) {
                command.append(KEY_AUTOUPDATE);
                command.append("=");
                command.append(this.autoupdate);
                command.append("\r\n");
            }
            if (!TextUtils.isEmpty(this.reboot)) {
                command.append(KEY_REBOOT);
                command.append("=");
                command.append(this.reboot);
                command.append("\r\n");
            }
            command.append(KEY_USER_FREESIZE);
            command.append("=");
            command.append(this.user_freesize);
            command.append("\r\n");
            return command.toString();
        }
    }

    private AutoupdateCommand getCommand(String filePath) {
        return getCommondFormFile(filePath);
    }

    private AutoupdateCommand getCommondFormFile(String filePath) {
        BufferedReader reader = null;
        FileInputStream in = null;
        AutoupdateCommand autoupdateCommand = new AutoupdateCommand();
        try {
            FileInputStream in2 = new FileInputStream(filePath);
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(in2, "UTF-8"));
            while (true) {
                String line = reader2.readLine();
                if (line != null) {
                    parseCommand(line, autoupdateCommand);
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        Log.w(TAG, "getCommondFormFile FileInputStream close error");
                    }
                }
            }
            in2.close();
            try {
                reader2.close();
            } catch (IOException e2) {
                Log.w(TAG, "getCommondFormFile BufferedReader close error");
            }
        } catch (IOException e3) {
            Log.e(TAG, "getCommondFormFile ", e3);
            if (0 != 0) {
                try {
                    in.close();
                } catch (IOException e4) {
                    Log.w(TAG, "getCommondFormFile FileInputStream close error");
                }
            }
            if (0 != 0) {
                reader.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    in.close();
                } catch (IOException e5) {
                    Log.w(TAG, "getCommondFormFile FileInputStream close error");
                }
            }
            if (0 != 0) {
                try {
                    reader.close();
                } catch (IOException e6) {
                    Log.w(TAG, "getCommondFormFile BufferedReader close error");
                }
            }
            throw th;
        }
        return autoupdateCommand;
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x003b, code lost:
        if (r4.equals("--update_package") != false) goto L_0x0049;
     */
    private void parseCommand(String command, AutoupdateCommand autoupdateCommand) {
        if (!TextUtils.isEmpty(command)) {
            String[] kv = command.split("=");
            if (2 == kv.length) {
                boolean z = false;
                String str = kv[0];
                switch (str.hashCode()) {
                    case -1453042472:
                        if (str.equals("--autoupdate")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case -22157200:
                        break;
                    case 653154497:
                        if (str.equals("--user_freesize")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    case 1465075013:
                        if (str.equals("--reboot")) {
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    default:
                        z = true;
                        break;
                }
                if (!z) {
                    autoupdateCommand.setUpdatePackage(kv[1]);
                } else if (z) {
                    autoupdateCommand.setAutoDate(kv[1]);
                } else if (z) {
                    autoupdateCommand.setReboot(kv[1]);
                } else if (!z) {
                    Log.w(TAG, "can not parse command");
                } else {
                    autoupdateCommand.setUserFreeSize(kv[1]);
                }
            } else {
                Log.w(TAG, "Error command");
            }
        }
    }

    private void writeRebootReason(String filePath, String reason) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filePath), true), "UTF-8"));
            writer.write(reason);
            try {
                writer.close();
            } catch (Exception e) {
                Log.e(TAG, "writeRebootReason writer close error");
            }
        } catch (IOException e2) {
            Log.e(TAG, "writeRebootReason error : " + e2);
            if (writer != null) {
                writer.close();
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e3) {
                    Log.e(TAG, "writeRebootReason writer close error");
                }
            }
            throw th;
        }
    }

    private int getBatteryLevelValue(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService("batterymanager");
        if (batteryManager == null) {
            Log.e(TAG, "batteryManager is null error");
            return -1;
        }
        int batteryLevel = batteryManager.getIntProperty(4);
        if (batteryLevel >= 0) {
            return batteryLevel;
        }
        Log.e(TAG, "getBatteryLevelValue failed, batteryLevel: " + batteryLevel);
        return -1;
    }

    private boolean isUserFreeSizeEnough(AutoupdateCommand autoupdateCommand) {
        long needUserFreeSize = autoupdateCommand.getUserFreeSize();
        if (-1 == needUserFreeSize) {
            return false;
        }
        if (0 == needUserFreeSize || getAvailableSize(Environment.getDataDirectory().getPath()) >= needUserFreeSize) {
            return true;
        }
        Log.i(TAG, "free size not enough");
        return false;
    }

    private long getAvailableSize(String path) {
        try {
            StatFs state = new StatFs(path);
            long blocksize = (long) state.getBlockSize();
            long availableBlocks = (long) state.getAvailableBlocks();
            long availableSize = blocksize * availableBlocks;
            Log.d(TAG, "getAvailableSize path : " + path + " availableSize =  (" + blocksize + "*" + availableBlocks + ") = " + availableSize);
            return availableSize;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getAvailableSize error : " + path, e);
            return 0;
        }
    }
}
