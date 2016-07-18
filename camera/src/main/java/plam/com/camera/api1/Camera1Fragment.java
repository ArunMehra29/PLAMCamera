package plam.com.camera.api1;

import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;

import plam.com.camera.R;
import plam.com.camera.Utils.BitmapCache;
import plam.com.camera.Utils.BitmapFilters;
import plam.com.camera.Utils.CameraUtils;
import plam.com.camera.Utils.GeneralUtils;
import plam.com.camera.listener.ICameraIO;

import static plam.com.camera.Utils.BitmapFilters.*;
import static plam.com.camera.Utils.CameraUtils.*;
import static plam.com.camera.Utils.GeneralUtils.*;


/**
 * Created by PunK _|_ RuLz on 01/07/16.
 */
public class Camera1Fragment extends Fragment implements GestureDetector.OnGestureListener {

    public static final String LOG_TAG = Camera1Fragment.class.getSimpleName();

    private Camera mCamera;
    private CameraPreview mCameraView;
    private FrameLayout mCameraPreviewContainer;
    private RelativeLayout mImagePreviewLayout;
    private RelativeLayout mCameraActionsLayout;
    private ImageButton mCameraFlashImageButton;
    private ImageButton mCameraCaptureImageButton;
    private ImageButton mSwitchCameraImageButton;
    private ImageButton mAcceptImageButton;
    private ImageView mCapturedImageView;

    private ICameraIO mCameraIOListener;
    private OrientationEventListener mOrientationListener;

    private Bitmap mCurrentBitmap;
    private Bitmap mPreviousBitmap;
    private Bitmap mNextBitmap;
    private Bitmap mResultBitmap;
    private Filters mCurrentFilter;
    private Filters mPreviousFilter;
    private Filters mNextFilter;
    private Canvas mImageCanvas;
    private BitmapCache mBitmapCache;

    private ScreenOrientation mCurrentOrientation;
    private CameraFlashState mCameraFlashState;
    private GestureDetector mGestureDetector;
    private ScrollDirection mCurrentScrollDirection;

    private int mTouchX;
    private boolean mIsFlingFired;

    public static Camera1Fragment getCamera1FragmentInstance() {
        Camera1Fragment fragment = new Camera1Fragment();
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mCamera) {
            mCamera.startPreview();
        } else {
            if (mCurrentCameraId == -1) {
                startCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                startCamera(mCurrentCameraId);
            }
        }
        mOrientationListener = new OrientationEventListener(getActivity()) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (null != getOrientation(orientation)) {
                    mCurrentOrientation = getOrientation(orientation);
                }
            }
        };
        mOrientationListener.enable();
    }

    @Override
    public void onStop() {
        super.onStop();
        removeViewsCallbacks();
        releaseCamera();
        mOrientationListener.disable();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_api1_layout, container, false);

        //reference views
        mCameraPreviewContainer = (FrameLayout) view.findViewById(R.id.camera_preview_container);
        mCameraFlashImageButton = (ImageButton) view.findViewById(R.id.camera_flash_image_button);
        mCameraCaptureImageButton = (ImageButton) view.findViewById(R.id.capture_camera_image_button);
        mSwitchCameraImageButton = (ImageButton) view.findViewById(R.id.switch_camera_image_button);
        mCapturedImageView = (ImageView) view.findViewById(R.id.captured_image_view);
        mCameraActionsLayout = (RelativeLayout) view.findViewById(R.id.camera_action_layout);
        mImagePreviewLayout = (RelativeLayout) view.findViewById(R.id.image_preview_layout);
        mImagePreviewLayout.setVisibility(View.GONE);
        mAcceptImageButton = (ImageButton) view.findViewById(R.id.accept_image_button);

        //click listeners
        mCapturedImageView.setOnTouchListener(imageTouchListener);
        mCameraFlashImageButton.setOnClickListener(cameraFlashListener);
        mCameraCaptureImageButton.setOnClickListener(captureImageListener);
        mSwitchCameraImageButton.setOnClickListener(switchCameraListener);
        mAcceptImageButton.setOnClickListener(acceptCapturedImaged);

        mGestureDetector = new GestureDetector(getActivity(), Camera1Fragment.this);
        mCurrentScrollDirection = ScrollDirection.NONE;
        mCurrentFilter = Filters.NONE;

        mBitmapCache = BitmapCache.getInstance();
        mBitmapCache.initializeCache();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCameraRotation(getActivity(), mCurrentCameraId);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mIsFlingFired) {
            mIsFlingFired = false;
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mCurrentScrollDirection.ordinal() == ScrollDirection.NONE.ordinal()) {
            if (distanceX > 0) {
                mCurrentScrollDirection = ScrollDirection.LEFT;
            } else {
                mCurrentScrollDirection = ScrollDirection.RIGHT;
            }
        }
        mTouchX = (int) e2.getX();
        overlayBitmaps(mTouchX);
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        switch (mCurrentScrollDirection) {
            case LEFT: {
                overlayNextBitmap(mCurrentBitmap.getWidth() - 1);
                mCapturedImageView.setImageDrawable(new BitmapDrawable(getResources(), mNextBitmap));
                shuffleBitmap(true);
                break;
            }
            case RIGHT: {
                overlayPreviousBitmap(1);
                mCapturedImageView.setImageDrawable(new BitmapDrawable(getResources(), mPreviousBitmap));
                shuffleBitmap(false);
                break;
            }
        }
        mIsFlingFired = true;
        mCurrentScrollDirection = ScrollDirection.NONE;
        return false;
    }

    private void shuffleBitmap(boolean isSwipeLeft) {
        if (isSwipeLeft) {
            mPreviousBitmap = mCurrentBitmap;
            mPreviousFilter = mCurrentFilter;
            mCurrentBitmap = mNextBitmap;
            mCurrentFilter = mNextFilter;
            mNextFilter = mCurrentFilter.next();
            //note next bitmap can be null if not found in cache
            mNextBitmap = mBitmapCache.getBitmapCache(mNextFilter.name());
        } else {
            mNextBitmap = mCurrentBitmap;
            mNextFilter = mCurrentFilter;
            mCurrentFilter = mPreviousFilter;
            mCurrentBitmap = mPreviousBitmap;
            mPreviousFilter = mCurrentFilter.previous();
            //note previous bitmap can be null if not found in cache
            mPreviousBitmap = mBitmapCache.getBitmapCache(mPreviousFilter.name());
        }
    }

    View.OnTouchListener imageTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (!mIsFlingFired) {
                        resetToCurrentFilter();
                    }
                    break;
                }
            }
            return true;
        }
    };

    View.OnClickListener cameraFlashListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCameraFlashState = mCameraFlashState.getNext();
            switch (mCameraFlashState) {
                case ON: {
                    setCameraFlashOn();
                    break;
                }
                case OFF: {
                    setCameraFlashOff();
                    break;
                }
            }
        }
    };

    View.OnClickListener acceptCapturedImaged = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO accept image logic
        }
    };

    View.OnClickListener switchCameraListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                startFrontCamera();
            } else {
                startBackCamera();
            }
        }
    };

    View.OnClickListener captureImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            manuallyTurnOnFlash();
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (null != data) {
                        loadCapturedImage(data, mCurrentOrientation);
                        new LoadImageTask().
                                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        restartCamera();
                    }
                    mCamera.setPreviewCallback(null);
                    manuallyTurnOffFlash();
                }
            });
        }
    };

    private void resetToCurrentFilter() {
        mCapturedImageView.setImageDrawable(new BitmapDrawable(getResources(), mCurrentBitmap));
        mIsFlingFired = false;
        mCurrentScrollDirection = ScrollDirection.NONE;
    }

    private void overlayBitmaps(int coordinateX) {

        switch (mCurrentScrollDirection) {
            case NONE: {
                //do nothing here
                break;
            }
            case LEFT: {
                overlayNextBitmap(coordinateX);
                break;
            }
            case RIGHT: {
                overlayPreviousBitmap(coordinateX);
                break;
            }
        }
    }

    private void overlayPreviousBitmap(int coordinateX) {
        mImageCanvas.save();

        Bitmap OSBitmap = Bitmap.createBitmap(mCurrentBitmap, coordinateX, 0, mCurrentBitmap.getWidth() - coordinateX, mCurrentBitmap.getHeight());
        mImageCanvas.drawBitmap(OSBitmap, coordinateX, 0, null);

        Bitmap FSBitmap = Bitmap.createBitmap(mPreviousBitmap, 0, 0, coordinateX, mCurrentBitmap.getHeight());
        mImageCanvas.drawBitmap(FSBitmap, 0, 0, null);

        mImageCanvas.restore();

        mCapturedImageView.setImageDrawable(new BitmapDrawable(getResources(), mResultBitmap));
    }

    private void overlayNextBitmap(int coordinateX) {
        mImageCanvas.save();

        Bitmap OSBitmap = Bitmap.createBitmap(mCurrentBitmap, 0, 0, coordinateX, mCurrentBitmap.getHeight());
        mImageCanvas.drawBitmap(OSBitmap, 0, 0, null);

        Bitmap FSBitmap = Bitmap.createBitmap(mNextBitmap, coordinateX, 0, mCurrentBitmap.getWidth() - coordinateX, mCurrentBitmap.getHeight());
        mImageCanvas.drawBitmap(FSBitmap, coordinateX, 0, null);

        mImageCanvas.restore();

        mCapturedImageView.setImageDrawable(new BitmapDrawable(getResources(), mResultBitmap));
    }

    private void startBackCamera() {
        mSwitchCameraImageButton.setImageDrawable(ContextCompat.
                getDrawable(getActivity(), R.drawable.ic_camera_front_white_24dp));
        startCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    private void startFrontCamera() {
        mSwitchCameraImageButton.setImageDrawable(ContextCompat.
                getDrawable(getActivity(), R.drawable.ic_camera_rear_white_24dp));
        startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    private void manuallyTurnOffFlash() {
        if (mCameraFlashState.ordinal() == CameraFlashState.ON.ordinal()) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
        }
    }

    private void manuallyTurnOnFlash() {
        if (mCameraFlashState.ordinal() == CameraFlashState.ON.ordinal()) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCameraView.handleFocus(parameters);
            mCamera.setParameters(parameters);
        }
    }

    private void setCameraFlashOff() {
        mCameraFlashImageButton.setImageDrawable(
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_flash_off_white_24dp));
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
    }

    private void setCameraFlashOn() {
        mCameraFlashImageButton.setImageDrawable(
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_flash_on_white_24dp));
        Camera.Parameters params = mCamera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        mCamera.setParameters(params);
    }

    public void setCameraIOListener(ICameraIO listener) {
        this.mCameraIOListener = listener;
    }

    private void removeViewsCallbacks() {
        mCameraPreviewContainer.removeAllViews();
        if (null != mCameraView) {
            mCameraView.getHolder().removeCallback(mCameraView);
            mCameraView = null;
        }
    }

    private void restartCamera() {
        mCamera.startPreview();
    }

    private void recycleBitmap() {
        if (mCurrentBitmap != null) {
            mCurrentBitmap = null;
        }
        if (mPreviousBitmap != null) {
            mPreviousBitmap = null;
        }
        if (mNextBitmap != null) {
            mNextBitmap = null;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startCamera(int cameraId) {
        if (null != mCamera) {
            releaseCamera();
        }
        if (null != mCameraView) {
            mCameraPreviewContainer.removeAllViews();
            mCameraView = null;
        }
        mCamera = getCameraInstance(getActivity(), cameraId);
        if (null != mCamera) {
            if (null == mCameraView && null != mCamera) {
                mCameraView = new CameraPreview(getActivity(), mCamera);
                mCameraPreviewContainer.addView(mCameraView);
            }
            if (isCameraFlashAvailable()) {
                mCameraFlashImageButton.setVisibility(View.VISIBLE);
            } else {
                mCameraFlashImageButton.setVisibility(View.GONE);
            }
            mCameraFlashState = CameraFlashState.OFF;
        } else {
            if (null != mCameraIOListener) {
                mCameraIOListener.onCameraOpenFailure();
            }
        }
    }

    public boolean isImagePreviewVisible() {
        return View.VISIBLE == mImagePreviewLayout.getVisibility();
    }

    public void resumeCamera() {
        mCapturedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImagePreviewLayout.setVisibility(View.GONE);
        mCameraActionsLayout.setVisibility(View.VISIBLE);
        mCameraPreviewContainer.setVisibility(View.VISIBLE);
        mCurrentScrollDirection = ScrollDirection.NONE;
        mBitmapCache.clearCache();
        recycleBitmap();
        restartCamera();
    }

    private void loadCapturedImage(byte[] data, ScreenOrientation screenOrientation) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, info);
        Camera.Parameters parameters = mCamera.getParameters();
        int imageFormat = parameters.getPreviewFormat();
        if (ImageFormat.NV21 == imageFormat) {
            Rect rect = new Rect(0, 0, parameters.getPreviewSize().width,
                    parameters.getPreviewSize().height);
            ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
            YuvImage img = new YuvImage(data, ImageFormat.NV21,
                    parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
            img.compressToJpeg(rect, 80, BAOS);
            byte[] bytes = BAOS.toByteArray();
            mCurrentBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            Matrix matrix = new Matrix();

            int rotationValue = getImageRotationValue(screenOrientation, info);
            if (screenOrientation.ordinal() == ScreenOrientation.PORTRAIT.ordinal() ||
                    screenOrientation.ordinal() == ScreenOrientation.PORTRAIT_UPSIDE_DOWN.ordinal()) {
                mCapturedImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            }
            if (rotationValue != 0) {
                matrix.postRotate(rotationValue);
                mCurrentBitmap = Bitmap.createBitmap(mCurrentBitmap, 0, 0, mCurrentBitmap.getWidth(),
                        mCurrentBitmap.getHeight(), matrix, false);
            }
            mCurrentFilter = Filters.NONE;
            mBitmapCache.addBitmapCache(mCurrentFilter.name(), mCurrentBitmap);
            mCameraPreviewContainer.setVisibility(View.GONE);
            mCameraActionsLayout.setVisibility(View.GONE);
            mImagePreviewLayout.setVisibility(View.VISIBLE);
            mCapturedImageView.setImageBitmap(mCurrentBitmap);
        }
    }

    private int getImageRotationValue(ScreenOrientation screenOrientation,
                                      Camera.CameraInfo info) {
        int rotationValue = 0;
        //either the application is using the back camera
        //if the application is using the front camera and it's in either portrait or upside portrait mode
        //for application using front camera and orientation is left landscape. It works fine automatically.
        if ((mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) ||
                (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT &&
                        (screenOrientation.ordinal() == ScreenOrientation.PORTRAIT.ordinal() ||
                                screenOrientation.ordinal() == ScreenOrientation.PORTRAIT_UPSIDE_DOWN.ordinal()))) {

            //this is for reverse landscape for phones like Nexus 5X.
            if (info.orientation == DEGREE_TWO_SEVENTY) {
                rotationValue = -DEGREE_NINETY;
            } else {
                rotationValue = DEGREE_NINETY;
            }
            switch (screenOrientation) {
                case LEFT_LANDSCAPE: {
                    rotationValue -= DEGREE_NINETY;
                    break;
                }
                case RIGHT_LANDSCAPE: {
                    rotationValue += DEGREE_NINETY;
                    break;
                }
                case PORTRAIT_UPSIDE_DOWN: {
                    rotationValue += DEGREE_ONE_EIGHTY;
                    break;
                }
            }
        } else if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT &&
                screenOrientation.ordinal() == ScreenOrientation.RIGHT_LANDSCAPE.ordinal()) {
            rotationValue += DEGREE_ONE_EIGHTY;
        }
        return rotationValue;
    }

    private class LoadImageTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            mNextBitmap = Bitmap.createBitmap(mCurrentBitmap);
            mNextBitmap = getGreyScaledBitmap(mNextBitmap);
            mNextFilter = mCurrentFilter.next();
            mBitmapCache.addBitmapCache(mNextFilter.name(), mNextBitmap);
            mPreviousBitmap = Bitmap.createBitmap(mCurrentBitmap);
            mPreviousBitmap = getTintedBitmap(mPreviousBitmap, Color.RED);
            mPreviousFilter = mCurrentFilter.previous();
            mBitmapCache.addBitmapCache(mPreviousFilter.name(), mPreviousBitmap);
            mResultBitmap = Bitmap.createBitmap(mCurrentBitmap.getWidth(), mCurrentBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mImageCanvas = new Canvas(mResultBitmap);

            return false;
        }


    }
}
