package com.sandyclock.plugins.BarcodeScanner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.content.Context;
import android.content.res.Configuration;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.graphics.Rect;
import java.io.ByteArrayOutputStream;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.Surface;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.Result;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class BarcodeScanner extends CordovaPlugin
{
    private final static String TAG = "BarcodeScanner";
    private final static String kLensOrientationKey = "cameraPosition";
    private final static String kWidthKey = "width";
    private final static String kHeightKey = "height";

    private Activity mActivity;
    private CallbackContext mCallbackContext;
    private TextureView mTextureView = null;
    private Camera mCamera;
    private QRCodeReader mReader;
    private int mCameraId = 0;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private int mPreviewFormat;
    private boolean mJustScanned = false;
    private static final ScheduledExecutorService worker =
        Executors.newSingleThreadScheduledExecutor();

    private int mLensOrientation;
    private int mWidth = 352;
    private int mHeight = 288;
    private int mRotation = 0;

    @Override
    public void onResume(boolean multitasking) {
        if (mTextureView != null) {
            initPreviewSurface();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCameraDisplayOrientation();
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException
    {
        mActivity = this.cordova.getActivity();

        if ("startCapture".equals(action)) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    startCapture(args, callbackContext);
                }
            });
            return true;
        } else if ("stopCapture".equals(action)) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    stopCapture(args, callbackContext);
                }
            });
            return true;
        }

        return false;
    }

    private void startCapture(JSONArray args, CallbackContext callbackContext)
    {
        String cameraPosition = "";

        // init parameters - default values
        mLensOrientation = Camera.CameraInfo.CAMERA_FACING_BACK;

        // parse options
        try {
            JSONObject jsonData = args.getJSONObject(0);
            getOptions(jsonData);
        } catch(Exception e) {
            Log.e(TAG, "Parsing options error: " + e.getMessage());
        }

        if (checkCameraHardware(mActivity)) {
            mCallbackContext = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);

            initPreviewSurface();
        } else {
            Log.e(TAG, "No camera detected");
        }
    }

    private void stopCapture(JSONArray args, CallbackContext callbackContext)
    {
        try {
            ((ViewGroup)mTextureView.getParent()).removeView(mTextureView);
            mTextureView = null;
        } catch (Exception e) {
            Log.e(TAG, "Camera can't be stopped: " + e.getMessage());
        }
    }

    private void initPreviewSurface() {
        mTextureView = new TextureView(mActivity);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        addViewToLayout(mTextureView);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mCamera = getCameraInstance();
            setCameraDisplayOrientation();

            try {
                mReader = new QRCodeReader();

                setPreviewParameters(mCamera);

                mCamera.setPreviewTexture(surface);
                mCamera.setPreviewCallback(mCameraPreviewCallback);
                mCamera.setErrorCallback(mCameraErrorCallback);

                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Failed to init preview: " + e.getLocalizedMessage());
            }

            // mTextureView.setVisibility(View.INVISIBLE);
            // mTextureView.setAlpha(0.5f);


            webView.getView().setBackgroundColor(0x00000000);
            webView.getView().bringToFront();
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Ignored, Camera does all the work for us
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            stopCamera();
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Invoked every time there's a new Camera preview frame
        }
    };

    private Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mJustScanned) return;

            LuminanceSource source = new PlanarYUVLuminanceSource(data, mPreviewWidth, mPreviewHeight,
                                                                    0, 0, mPreviewWidth, mPreviewHeight, false);

            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                Result decodeResult = mReader.decode(binaryBitmap);
                mJustScanned = true;

                Runnable task = new Runnable() {
                    public void run() {
                        mJustScanned = false;
                    }
                };
                worker.schedule(task, 3, TimeUnit.SECONDS);

                Log.d(TAG, "Decoded string: " + decodeResult.getText());

                PluginResult result = new PluginResult(PluginResult.Status.OK, decodeResult.getText());
                result.setKeepCallback(true);
                mCallbackContext.sendPluginResult(result);
            } catch(Exception e) {
                // do nothing - qr-code have not found
            }

        }
    };

    private Camera.ErrorCallback mCameraErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int error, Camera camera) {
            Log.e(TAG, "on camera error: " + error);

            try {
                stopCamera();
                initPreviewSurface();
            } catch(Exception e) {
                Log.e(TAG, "something happened while stopping camera: " + e.getMessage());
            }
        }
    };

    public void setCameraDisplayOrientation() {
        Camera.CameraInfo info =  new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
             result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private void setPreviewParameters(Camera camera) {
        Camera.Parameters params = camera.getParameters();

        Camera.Size previewSize = getOptimalPictureSize(params);
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        params.setPreviewSize(mPreviewWidth, mPreviewHeight);

        String focusMode = getOptimalFocusMode(params);
        params.setFocusMode(focusMode);

        camera.setParameters(params);

        mPreviewFormat = params.getPreviewFormat();
    }

    private String getOptimalFocusMode(Camera.Parameters params) {
        String result;
        List<String> focusModes = params.getSupportedFocusModes();

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            result = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            result = Camera.Parameters.FOCUS_MODE_AUTO;
        } else {
            result = params.getSupportedFocusModes().get(0);
        }

        return result;
    }

    private Camera.Size getOptimalPictureSize(Camera.Parameters params) {
        Camera.Size bigEnough = params.getSupportedPreviewSizes().get(0);

        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            if (size.width >= mWidth && size.height >= mHeight
                && size.width < bigEnough.width
                && size.height < bigEnough.height
            ) {
                bigEnough = size;
            }
        }

        return bigEnough;
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            return false;
        }
    }

    private Camera getCameraInstance() {
        if (mCamera != null) {
            return mCamera;
        }

        Camera camera = null;
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();
            int cameraId;

            for (cameraId = 0; cameraId < cameraCount; cameraId++) {
                Camera.getCameraInfo(cameraId, cameraInfo);
                if (cameraInfo.facing == mLensOrientation) {
                    try {
                        mCameraId = cameraId;
                        camera = Camera.open(cameraId);
                        break;
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Camera failed to open: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e){
            Log.e(TAG, "Camera is not available: " + e.getMessage());
        }

        return camera;
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            mCameraId = 0;
        }
    }

    private void addViewToLayout(View view) {
        WindowManager mW = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
		int screenWidth = mW.getDefaultDisplay().getWidth();
		int screenHeight = mW.getDefaultDisplay().getHeight();

        mActivity.addContentView(view, new ViewGroup.LayoutParams(screenWidth, screenHeight));
    }

    private void getOptions(JSONObject jsonData) throws Exception
    {
        if (jsonData == null) {
            return;
        }

        // lens orientation
        if (jsonData.has(kLensOrientationKey)) {
            String orientation = jsonData.getString(kLensOrientationKey);
            if (orientation.equals("front")) {
                mLensOrientation = Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                mLensOrientation = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
        }
    }

}
