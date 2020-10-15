package com.android.server.security.fileprotect;

import android.content.Context;
import android.util.Slog;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PackStoragePolicy {
    private static final String TAG = "PackStoragePolicy";
    public String packageName;
    public List<PathPolicy> policies = new ArrayList();

    PackStoragePolicy() {
    }

    public static List<PackStoragePolicy> parse(Context context, String fname) {
        int pkgListLen;
        JSONArray packageList;
        JSONObject json;
        String str;
        int pkgListLen2;
        JSONArray packageList2;
        String path;
        String str2 = "dir";
        List<PackStoragePolicy> packPolicyList = new ArrayList<>();
        try {
            JSONObject json2 = new JSONObject(readFileString(context, fname));
            JSONArray packageList3 = json2.getJSONArray(HwSecDiagnoseConstant.ANTIMAL_APK_LIST);
            int pkgListLen3 = packageList3.length();
            int i = 0;
            while (i < pkgListLen3) {
                JSONObject jsonObj = packageList3.getJSONObject(i);
                String pack = jsonObj.getString("name");
                PackStoragePolicy oneApp = new PackStoragePolicy();
                oneApp.packageName = pack;
                if (jsonObj.has(str2)) {
                    JSONArray dirList = jsonObj.getJSONArray(str2);
                    int dirListLen = dirList.length();
                    int j = 0;
                    while (j < dirListLen) {
                        JSONObject dirObj = (JSONObject) dirList.get(j);
                        String path2 = dirObj.getString("name");
                        String st = dirObj.getString("StorageType");
                        if ("true".equals(dirObj.getString("traversal"))) {
                            packageList2 = packageList3;
                            pkgListLen2 = pkgListLen3;
                            path = pack;
                            oneApp.policies.add(new PathPolicy(path2, st, 17));
                        } else {
                            packageList2 = packageList3;
                            pkgListLen2 = pkgListLen3;
                            path = pack;
                            oneApp.policies.add(new PathPolicy(path2, st, 16));
                        }
                        j++;
                        str2 = str2;
                        pack = path;
                        json2 = json2;
                        packageList3 = packageList2;
                        pkgListLen3 = pkgListLen2;
                    }
                    str = str2;
                    json = json2;
                    packageList = packageList3;
                    pkgListLen = pkgListLen3;
                } else {
                    str = str2;
                    json = json2;
                    packageList = packageList3;
                    pkgListLen = pkgListLen3;
                }
                if (jsonObj.has("file")) {
                    JSONArray fileList = jsonObj.getJSONArray("file");
                    int fileListLen = fileList.length();
                    for (int j2 = 0; j2 < fileListLen; j2++) {
                        JSONObject fileObj = (JSONObject) fileList.get(j2);
                        oneApp.policies.add(new PathPolicy(fileObj.getString("name"), fileObj.getString("StorageType"), 0));
                    }
                }
                packPolicyList.add(oneApp);
                i++;
                str2 = str;
                json2 = json;
                packageList3 = packageList;
                pkgListLen3 = pkgListLen;
            }
        } catch (JSONException e) {
            packPolicyList.clear();
            Slog.e(TAG, e.getMessage());
        }
        return packPolicyList;
    }

    private static String readFileString(Context context, String fname) {
        StringBuilder builder = new StringBuilder(5120);
        BufferedReader reader = null;
        int totalSize = 0;
        try {
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(context.getAssets().open(fname), "UTF-8"));
            while (true) {
                String line = reader2.readLine();
                if (line != null) {
                    totalSize += line.length();
                    if (totalSize < 5120) {
                        builder.append(line);
                    } else {
                        Slog.e(TAG, "line size is too larger than EXPECTED_BUFFER_SIZE");
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        Slog.e(TAG, e.getMessage());
                    }
                }
            }
            reader2.close();
        } catch (IOException e2) {
            Slog.e(TAG, e2.getMessage());
            if (0 != 0) {
                reader.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Slog.e(TAG, e3.getMessage());
                }
            }
            throw th;
        }
        return builder.toString();
    }
}
