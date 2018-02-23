package com.shaoyy.runningarcface;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.java.AbsLoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(permissionRequest()) onCreateNext();
    }
    public void onCreateNext(){
        //only if permission is allowed
        Log.i(TAG, "onCreateNext: Next Accessfully");


    }
    public boolean permissionRequest(){
        String[] permissions = new String[] {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for(int i=0;i<permissions.length;i++)
            if( this.checkPermission(permissions[i] , Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED)
                allowed=allowed&(1<<i);
        if(allowed<Math.pow(2,permissions.length)-1)
            this.requestPermissions(permissions, PERMISSION_REQ);
        return (allowed==Math.pow(2,permissions.length)-1);
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ: {
                if (grantResults.length == permissions.length && 
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    onCreateNext();
                } else {
                    Toast.makeText(this, "权限缺失，运行失败", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onRequestPermissionsResult:Lack permissions. ",new Exception() );
                }
                break;
            }
        }
    }

    class CheckLoop extends Thread{
        private boolean stop;
        public void run() {
            stop=false;
            setup();
            while (!stop) {
                loop();
            }
            over();
        }
        private void setup(){
            engine.AFR_FSDK_InitialEngine()
        }

        private void loop(){

        }

        private void over(){

        }
        public void stopNow(){
            stop=true;
        }

        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result = new AFR_FSDKFace();
        List<FaceDB.FaceRegist> mResgist = ((Application)DetecterActivity.this.getApplicationContext()).mFaceDB.mRegister;
        List<ASAE_FSDKFace> face1 = new ArrayList<>();
        List<ASGE_FSDKFace> face2 = new ArrayList<>();
    }
    private int mWidth, mHeight, mFormat;
    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;
    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    byte[] mImageNV21 = null;

    AFT_FSDKFace mAFT_FSDKFace = null;
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();

    int allowed=0;
    final int PERMISSION_REQ=1;
}
