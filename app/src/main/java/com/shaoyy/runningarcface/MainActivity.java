package com.shaoyy.runningarcface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraCaptureSession;
import android.content.Context;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File debugDelete = new File(Environment.getExternalStorageDirectory()+MainActivity.DATAPATH+"/.reloaded");
        if(debugDelete.exists()&&debugDelete.canWrite()) {
            debugDelete.delete();
            Log.i(TAG, "onCreate: .reloaded deleted");
        }
        
        
        //readImageHandlerThread=new HandlerThread("readImage");
        cameraHandlerThread=new HandlerThread("camera");
        mainHandler=new Handler(getMainLooper());
        nameidHandler= new Handler(getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what){
                    case NAMEID:
                        addPerson(msg.getData().getString("name"),msg.getData().getString("id"));
                        break;
                    case 456:
                        imageView.setImageBitmap(bmp);
                        if(bmp == null) Log.i(TAG, "handleMessage: failed");
                }
            }
        };
        //readImageHandlerThread.start();
        cameraHandlerThread.start();
        checkloop = new CheckLoop(nameidHandler);
        checkloop.start();
        //readImageHandler= new Handler(readImageHandlerThread.getLooper());
        cameraHandler= new Handler(cameraHandlerThread.getLooper());




        if(permissionRequest()) onCreateNext();
    }
    public void onCreateNext(){
        //only if permission is allowed
        Log.i(TAG, "onCreateNext: Next Accessfully");
        initRegists();
        if(faceDB == null) Log.w(TAG, "onCreateNext: ", new Exception("faceDB is null"));
        /*mImageReader = ImageReader.newInstance(MainActivity.IMAGE_WIDTH, MainActivity.IMAGE_HEIGHT, ImageFormat.JPEG,2);//读取照片参数设置
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                ByteBuffer buffer=image.getPlanes()[0].getBuffer();
                byte b[]  = new byte[buffer.remaining()];
                buffer.get(b);


                //mImageNV21 = ImageHelper.getDataFromImage(image,ImageHelper.COLOR_FormatNV21);
                /*
                byte[] data = mImageNV21;
                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, MainActivity.IMAGE_WIDTH, MainActivity.IMAGE_HEIGHT, null);
                ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0,0,0,0), 80, ops);
                bitmap = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);

                Message msg = new Message();
                msg.what = 456;
                MainActivity.this.nameidHandler.sendMessage(msg);
                */
                /*

                ImageConverter converter= new ImageConverter();
                mImageNV21 = new byte[bitmap.getWidth() * bitmap.getHeight() * 3 /2];
                converter.initial(bitmap.getWidth(), bitmap.getHeight(), ImageConverter.CP_PAF_NV21);
                if(converter.convert(bitmap,mImageNV21)) Log.i(TAG, "onImageAvailable: Convert successfully!");
                */
                /*
                image.close();
                Log.i(TAG, "onImageAvailable: ");

            }
        },readImageHandler);*/
        initView();
        if(faceDB.mRegister.size() == 0){
            Toast.makeText(this,"未检测到存储在本地的人脸信息",Toast.LENGTH_LONG).show();
        }
        //mSurfaceView.setVisibility(View.VISIBLE);

    }

    public void initRegists(){
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
            faceDB = new FaceDB(Environment.getExternalStorageDirectory().getPath()+MainActivity.DATAPATH);
        }else{
            Log.i(TAG, "initRegists: Failed permisson deny");
        }
    }

    public void initView(){
        imageView =  findViewById(R.id.imageView);
        mTextureView =  findViewById(R.id.textureView);
        button[0] =  findViewById(R.id.button0);
        button[1] =  findViewById(R.id.button1);
        button[2] = findViewById(R.id.button2);
        button[3] = findViewById(R.id.button3);
        for(Button bt:button){
            bt.setOnClickListener(buttonListener);
            bt.setVisibility(View.INVISIBLE);
        }
        //mSurfaceTexture = new SurfaceTexture(123,true);
        //mSurfaceTexture.setDefaultBufferSize(mTextureView.getWidth(),mTextureView.getHeight());
        //mTextureView.setSurfaceTexture(mSurfaceTexture);

        mTextureView.setKeepScreenOn(true);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureAvailable: 1");
                initCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if ( mCameraDevice!=null){
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                Log.i(TAG, "surfaceDestroyed: ");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        /*mSurfaceView=(SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                //SurfaceView创建 // 初始化Camera
                Log.i(TAG, "surfaceCreated: 1");
                initCamera();
            }
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                //SurfaceView销毁,释放Camera
                if ( mCameraDevice!=null){
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                Log.i(TAG, "surfaceDestroyed: ");
            }
        });*/
    }
    public void initCamera(){
        Log.i(TAG, "initCamera: ");
        if(this.checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            mCameraID = "" + CameraCharacteristics.LENS_FACING_BACK;//用于修改摄像头前置或后置

            try{
                mCameraManager.openCamera(mCameraID,stateCallback, mainHandler);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            try{
                mTextureView.getSurfaceTexture().setDefaultBufferSize(mTextureView.getWidth(),mTextureView.getHeight());
                mSurface = new Surface(mTextureView.getSurfaceTexture());

                final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
                //previewRequestBuilder.addTarget(mImageReader.getSurface());
                previewRequestBuilder.addTarget(mSurface);
                mCameraDevice.createCaptureSession(Arrays.asList(mSurface),new CameraCaptureSession.StateCallback(){
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mCameraCaptureSession = cameraCaptureSession;

                        try{
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                            CaptureRequest previewRequest = previewRequestBuilder.build();
                            mCameraCaptureSession.setRepeatingRequest(previewRequest,null,cameraHandler);
                        } catch (CameraAccessException e){
                            e.printStackTrace();
                            Log.e(TAG, "onConfigured: Build Failed",e );
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Log.i(TAG, "onConfigureFailed: Failed");
                    }
                },mainHandler);

            }catch (Exception e){
                Log.e(TAG, "onOpened: createCaptureRequestFail",e );
            }


        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            if(mCameraDevice!=null){
                mCameraDevice.close();
                mCameraDevice=null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Toast.makeText(MainActivity.this,"Open Camera Failed",Toast.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener buttonListener = new View.OnClickListener() {//按钮事件监听
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.button_setting:
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("设置")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setItems(new String[]{"重新加载图片"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    settingSelect(which);
                                }
                            })
                            .show();
                    break;
                case R.id.button0:
                case R.id.button1:
                case R.id.button2:
                    Button bt = (Button) v;
                    StringBuffer stringBuffer = new StringBuffer(bt.getText());
                    String id = stringBuffer.substring(0,8);
                    String name = stringBuffer.substring(9,stringBuffer.length());
                    submitMessage(id,name);
                    clearPerson();
                    break;
                case R.id.button3:
                    clearPerson();
                    break;
            }
        }
    };

    int personCount=0;
    public boolean addPerson(String name,String ID){ //UI线程中执行
        if(personCount>=3) return false;
        StringBuffer stringBuffer = new StringBuffer(ID);
        stringBuffer.append('\n');
        stringBuffer.append(name);
        button[personCount].setText(stringBuffer);
        button[personCount++].setVisibility(View.VISIBLE);
        if(personCount == 3) button[3].setVisibility(View.VISIBLE);
        return true;
    }
    public void clearPerson(){
        personCount = 0;
        persons.clear();
        for(Button bt:button) bt.setVisibility(View.INVISIBLE);
    }

    public void settingSelect(int which){
        switch (which){
            case 0://重新加载图片
                faceDB.deleteReloaded();
                faceDB.reloadImage();
                break;
        }
    }

    public boolean permissionRequest(){
        String[] permissions = new String[] {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for(int i=0;i<permissions.length;i++)
            if( this.checkPermission(permissions[i] , Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED)
                allowed=allowed|(1<<i);
        if(allowed<Math.pow(2,permissions.length)-1)
            this.requestPermissions(permissions, PERMISSION_REQ);
        return (allowed==Math.pow(2,permissions.length)-1);
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ: {
                boolean grantedAll = true;
                for(int result:grantResults) if(result == PackageManager.PERMISSION_DENIED) grantedAll = false;
                if (grantedAll) {
                    onCreateNext();
                } else {
                    Toast.makeText(this, "权限缺失，运行失败", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onRequestPermissionsResult:Lack permissions. ",new Exception() );
                }
                break;
            }
        }
    }

    public void submitMessage(String id,String name){
        Toast.makeText(this,"成功提交："+id+name,Toast.LENGTH_SHORT).show();
    }

    class CheckLoop extends Thread{
        private boolean stop;
        private Handler handler;
        public CheckLoop(Handler handler){
            this.handler=handler;
        }
        public void run() {
            stop=false;
            setup();
            while (!stop) {
                synchronized (this){
                    try{
                        this.wait(MainActivity.CHECKTIME);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                loop();
            }
            over();
        }
        private void setup(){
            //engineFD.AFD_FSDK_InitialFaceEngine(FaceDB.appid,FaceDB.fd_key,AFD_FSDKEngine.AFD_FOC_0,MainActivity.MIN_FACE_SIZE,1);
            engineFT.AFT_FSDK_InitialFaceEngine(FaceDB.appid,FaceDB.ft_key,AFD_FSDKEngine.AFD_FOC_0,MainActivity.MIN_FACE_SIZE,1);
        }

        private void loop(){
            if(mTextureView != null) {
                Bitmap textureBitmap = mTextureView.getBitmap();
                bitmap = textureBitmap.copy(textureBitmap.getConfig(), false);
            /*Message bitmapMsg = new Message();
            bitmapMsg.what = 456;
            handler.sendMessage(bitmapMsg);*/
            /*ImageConverter converter= new ImageConverter();
            converter.initial(bitmap.getWidth(), bitmap.getHeight(), ImageConverter.CP_PAF_NV21);*/
                boolean convertSucceed = false;
                mImageNV21 = new byte[bitmap.getWidth() * bitmap.getHeight() * 3 / 2];
                convertSucceed = ImageHelper.getBitmap2NV21(bitmap, mImageNV21);

                Log.i(TAG, "loop: Converted");
                /**-----------------------------------debug 检测图像可靠用
                if (mImageNV21[0] != 0) {
                    mImageNV21[0] = 0;
                }*/

                /**-----------------------------------debug 手动格式转换后预览用
                byte[] data = mImageNV21;
                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, bitmap.getWidth(), bitmap.getHeight(), null);
                ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), 80, ops);
                bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                try {
                    ops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Message bitmapMsg = new Message();
                bitmapMsg.what = 456;
                handler.sendMessage(bitmapMsg);*/

                if (convertSucceed) {
                    Log.i(TAG, "loop: Convert Succeed!");
                    AFT_FSDKError errorFT = engineFT.AFT_FSDK_FaceFeatureDetect(mImageNV21, bitmap.getWidth(), bitmap.getHeight(), AFT_FSDKEngine.CP_PAF_NV21, lsFT);
                    Log.i(TAG, "loop: looping" + errorFT.getCode());
                    if (!lsFT.isEmpty()) {
                        faceFT = lsFT.get(0).clone();
                        lsFT.clear();
                        Log.i(TAG, "loop: face geted FT");
                    }
                    if (faceFT != null) {
                        AFR_FSDKError errorFR = faceDB.engineFR.AFR_FSDK_ExtractFRFeature(mImageNV21, bitmap.getWidth(), bitmap.getHeight(),
                                AFD_FSDKEngine.CP_PAF_NV21, faceFT.getRect(),faceFT.getDegree(), faceFR);
                        Log.i(TAG, "loop: errorFR"+errorFR.getCode());
                        float max = 0;
                        Log.i(TAG, "loop: Anelize succeed");
                        if (!faceDB.mRegister.isEmpty()) {
                            for (FaceDB.FaceRegist fr : faceDB.mRegister) {
                                if (fr.getFace() != null) {
                                    faceDB.engineFR.AFR_FSDK_FacePairMatching(faceFR, fr.getFace(), score);
                            }
                                if (score.getScore() > max) {
                                    max = score.getScore();
                                    maxPerson = fr;
                                }
                            }
                        }

                        if (maxPerson != null) {
                            boolean conclude = false;
                            for(FaceDB.FaceRegist faceRegist:persons){
                                if(faceRegist.equals(maxPerson)) conclude = true;
                            }
                            if(!MainActivity.MOLT_PERSON) conclude = false;
                            if(!conclude) {
                                if(persons.size()<3) persons.add(maxPerson);
                                Message msg = new Message();
                                msg.what = MainActivity.NAMEID;
                                msg.getData().putString("name", maxPerson.mName);
                                msg.getData().putString("id", maxPerson.mID);
                                handler.sendMessage(msg);
                            }
                        }
                        faceFT = null;
                    }
                }
            }
        }

        private void over(){
            engineFT.AFT_FSDK_UninitialFaceEngine();
            engineFT = null;

        }
        public void stopNow(){
            stop=true;
        }


        AFR_FSDKFace faceFR = new AFR_FSDKFace();
        AFR_FSDKMatching score = new AFR_FSDKMatching();

        AFT_FSDKEngine engineFT = new AFT_FSDKEngine();
        AFT_FSDKFace faceFT = null;
        List<AFT_FSDKFace> lsFT = new ArrayList<>();

        FaceDB.FaceRegist maxPerson = null;
    }
    private TextureView mTextureView;
    private Surface mSurface;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private ImageView imageView;
    private CheckLoop checkloop;
    HandlerThread readImageHandlerThread,cameraHandlerThread;
    public Handler mainHandler,readImageHandler,cameraHandler,nameidHandler;
    String mCameraID;
    public byte[] mImageNV21 = null;
    public Bitmap bmp,bitmap;
    private Button button[] = new Button[4];
    private List<FaceDB.FaceRegist> persons = new ArrayList<>();
    public FaceDB faceDB;

    int allowed=0;
    final int PERMISSION_REQ=1;
    public static final int NAMEID=12345;
    public static final int CHECKTIME = 300;
    public static final int MIN_FACE_SIZE = 16;
    public static final float MIN_RECOGNIZE = 0.4f;
    public static final int IMAGE_WIDTH = 600;
    public static final int IMAGE_HEIGHT = 800;
    public static final String DATAPATH = "/FaceArcData";
    public static final boolean MOLT_PERSON = false;
    public static final String VERSION = "BetaV0.7";
}
