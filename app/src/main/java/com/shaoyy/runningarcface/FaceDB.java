package com.shaoyy.runningarcface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.util.Log;
import android.widget.Toast;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by shaoyy on 2018/2/23.
 */

public class FaceDB {
    private static final String TAG = "FaceDB";
    public static String appid = "5dqPwCSobkoR5gxSxeZV26W9tzoLdZwvqGm9sTMh1Lzr";
    public static String ft_key = "6Svi9j6Q6T5whMoX76JJ1jS4dng7EQdmwbTEeh6TYCSK";
    public static String fd_key = "6Svi9j6Q6T5whMoX76JJ1jSBoBwKt6zWVKTLt7q2UQRG";
    public static String fr_key = "6Svi9j6Q6T5whMoX76JJ1jSZHPiqCQoSKHAGnoHTqHv2";
    public static String age_key = "6Svi9j6Q6T5whMoX76JJ1jSvmbWMxzt8uX82rwUoCmdv";
    public static String gender_key = "6Svi9j6Q6T5whMoX76JJ1jT3vzmXoWgE5LLjxKWG8PK5";

    public static String checkNameId = "^\\d{8}\\s.*\\x2ejpg"; //用于检测学号姓名的正则表达式
    String mDBPath;
    List<FaceRegist> mRegister;
    boolean mUpgrade;
    AFD_FSDKEngine engineFD;
    List<AFD_FSDKFace> lsFD = new ArrayList<>();

    public AFR_FSDKEngine engineFR = new AFR_FSDKEngine();
    byte [] mImageNV21;

    public FaceDB(String path) {
        mDBPath = path;
        mRegister = new ArrayList<>();
        mUpgrade = false;
        engineFD = new AFD_FSDKEngine();
        engineFD.AFD_FSDK_InitialFaceEngine(FaceDB.appid,FaceDB.fd_key,AFD_FSDKEngine.AFD_FOC_0,MainActivity.MIN_FACE_SIZE,1);
        engineFR = new AFR_FSDKEngine();
        engineFR.AFR_FSDK_InitialEngine(FaceDB.appid,FaceDB.fr_key);

        File rootDir = new File(mDBPath);
        if(!rootDir.exists()) rootDir.mkdir();
        checkImages();
    }

    public void checkImages(){
        File reloaded = new File(mDBPath+"/"+".reloaded");
        if(!reloaded.exists()) reloadImage();
        else loadLists();
    }

    public void reloadImage(){
        Log.i(TAG, "reloadImage: start");
        List<File> fileList = new ArrayList<>();
        File imgDir = new File(mDBPath+"/"+"img");
        if(imgDir.exists()&&imgDir.isDirectory()){
            Log.i(TAG, "reloadImage: finding files");
            File files[] = imgDir.listFiles();
            if(files.length == 0) {
                Log.i(TAG, "reloadImage: No img files find");
                return;
            }
            for(File f : files){
                String fname = f.getName();
                Log.i(TAG, "reloadImage: file "+fname+" finded");
                int width=1,height=1;
                if(Pattern.matches(FaceDB.checkNameId,fname)){
                    Log.i(TAG, "reloadImage: file "+fname+" qualified");
                    try{
                        FileInputStream fstream = new FileInputStream(f);
                        Bitmap bmp = BitmapFactory.decodeStream(fstream);
                        width = bmp.getWidth();
                        height = bmp.getHeight();
                        mImageNV21 = new byte[width*height*3/2];
                        ImageHelper.getBitmap2NV21(bmp,mImageNV21);
                        AFD_FSDKError errorFD = engineFD.AFD_FSDK_StillImageFaceDetection(mImageNV21,width,height,AFD_FSDKEngine.CP_PAF_NV21,lsFD);
                        Log.i(TAG, "reloadImage: "+errorFD.getCode());
                    }catch (Exception e){}
                        if(lsFD != null&&!lsFD.isEmpty()){
                            Log.i(TAG, "reloadImage: recognize success"+fname);
                            AFD_FSDKFace faceFD = lsFD.get(0);
                            AFR_FSDKFace faceFR = new AFR_FSDKFace();
                            AFR_FSDKError errorFR = engineFR.AFR_FSDK_ExtractFRFeature(mImageNV21,width,height,AFR_FSDKEngine.CP_PAF_NV21,faceFD.getRect(),
                                    faceFD.getDegree(),faceFR);
                            if(errorFR.getCode()==0){
                                addPerson(fname,faceFR);
                            }
                        }else{
                            Log.i(TAG, "reloadImage: recognize fail"+fname);
                        }
                }
            }
            saveLists();
            File reloaded = new File(mDBPath+"/"+".reloaded");
            try{
                if(!reloaded.exists()) reloaded.createNewFile();
            }catch (Exception e){
                Log.e(TAG, "reloadImage: createFileFailed",e);
            }

        }else{
            imgDir.mkdir();
        }
    }
    void addPerson(String s,AFR_FSDKFace face){
        StringBuffer stringBuffer = new StringBuffer(s);
        String id = stringBuffer.substring(0,8);
        int index1 = stringBuffer.indexOf(" ");
        int index2 = stringBuffer.indexOf(".jpg");
        String name = stringBuffer.substring(index1+1, index2);
        FaceRegist fr = new FaceRegist(name,id);
        fr.setFace(face);
        mRegister.add(fr);
        Log.i(TAG, "addPerson: "+id+" "+name+" succeed");
    }

    @SuppressWarnings("unchecked")
    public void loadLists(){
        File facedataFile = new File(mDBPath+"/"+"face.dat");
        if(facedataFile.exists()){
            try{
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(facedataFile));
                mRegister = (List<FaceRegist>) ois.readObject();
            }catch (Exception e){}
        }else{
            Log.i(TAG, "loadLists: face.dat not find");
        }
    }

    public void saveLists(){
        File facedataFile = new File(mDBPath+"/"+"face.dat");
        try{
            if(!facedataFile.exists()) facedataFile.createNewFile();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(facedataFile));
            oos.writeObject(mRegister);
        }catch (Exception e){}
    }

    public void deleteReloaded(){
        File reloaded = new File(mDBPath+"/"+".reloaded");
        if(reloaded.exists()&&reloaded.canWrite()) reloaded.delete();
    }
    class FaceRegist {
        String mName;
        String mID;
        AFR_FSDKFace mFace = null;

        public FaceRegist(String name,String ID) {
            mName = name;
            mID=ID;
        }
        public AFR_FSDKFace getFace(){
            return mFace;
        }
        public void setFace(AFR_FSDKFace face){
            mFace = face.clone();
        }

        @Override
        public boolean equals(Object obj) {
            return mID.equals(((FaceRegist)obj).mID);
        }
    }
}
