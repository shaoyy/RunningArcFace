package com.shaoyy.runningarcface;

import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sim on 2018/2/23.
 */

public class FaceDB {
    private static final String TAG = "FaceDB";
    public static String appid = "5dqPwCSobkoR5gxSxeZV26W9tzoLdZwvqGm9sTMh1Lzr";
    public static String ft_key = "6Svi9j6Q6T5whMoX76JJ1jS4dng7EQdmwbTEeh6TYCSK";
    public static String fd_key = "6Svi9j6Q6T5whMoX76JJ1jSBoBwKt6zWVKTLt7q2UQRG";
    public static String fr_key = "6Svi9j6Q6T5whMoX76JJ1jSZHPiqCQoSKHAGnoHTqHv2";
    public static String age_key = "6Svi9j6Q6T5whMoX76JJ1jSvmbWMxzt8uX82rwUoCmdv";
    public static String gender_key = "6Svi9j6Q6T5whMoX76JJ1jT3vzmXoWgE5LLjxKWG8PK5";

    String mDBPath;
    List<FaceRegist> mRegister;
    AFR_FSDKEngine mFREngine;
    AFR_FSDKVersion mFRVersion;
    boolean mUpgrade;

    class FaceRegist {
        String mName;
        List<AFR_FSDKFace> mFaceList;

        public FaceRegist(String name) {
            mName = name;
            mFaceList = new ArrayList<>();
        }
    }

    public FaceDB(String path) {
        mDBPath = path;
        mRegister = new ArrayList<>();
        mFRVersion = new AFR_FSDKVersion();
        mUpgrade = false;
        mFREngine = new AFR_FSDKEngine();
        AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        if (error.getCode() != AFR_FSDKError.MOK) {
            Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
        } else {
            mFREngine.AFR_FSDK_GetVersion(mFRVersion);
            Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
        }
    }

    public void destroy() {
        if (mFREngine != null) {
            mFREngine.AFR_FSDK_UninitialEngine();
        }
    }
}
