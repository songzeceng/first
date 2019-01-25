package com.example.songzeceng.firstjd.ArcAPI;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.songzeceng.firstjd.Face;
import com.example.songzeceng.firstjd.FaceAPI.FaceEngineManager;
import com.example.songzeceng.firstjd.LivenessActivity;
import com.example.songzeceng.firstjd.utils.DrawUtils;
import com.example.songzeceng.firstjd.utils.ImageUtils;

import java.util.List;

public class ArcFaceCamera implements SurfaceHolder.Callback {

	SurfaceView surfce_preview, surfce_rect;
	SurfaceHolder holder;
	private Camera camera;
	private Activity activity;
	private CameraPreviewListener cameraPreviewListener;
	//	FaceTracker faceTrackService;
	private FaceEngineManager mFaceEngineManager;

	public static int previewSizeX, previewSizeY;

	//相机的位置
	private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
	//相机的方向
	private int cameraOri = 90;

	public void init(int cameraId) {
		this.cameraId = cameraId;
	}

	private ArcFaceCamera() {

	}


	public void openCamera(Activity activity, SurfaceView surfacePreview, SurfaceView
			surfaceViewRect) {
		this.activity = activity;
		surfce_preview = surfacePreview;
		surfce_rect = surfaceViewRect;
		surfce_preview.getHolder().addCallback(this);
		surfce_rect.setZOrderMediaOverlay(true);
		surfce_rect.getHolder().setFormat(PixelFormat.TRANSLUCENT);
//		faceTrackService = new FaceTracker();
		if (mFaceEngineManager == null) {
			mFaceEngineManager = new FaceEngineManager(activity);
		}
		mFaceEngineManager.initEngine(activity);
	}


	public static ArcFaceCamera getInstance() {
		return SingletonHolder.INSTANCE;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		this.holder = holder;
		start();
	}


	private void start() {
		//选择摄像头ID
		camera = Camera.open(cameraId);
		try {
			DisplayMetrics metrics = new DisplayMetrics();
			activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
			Camera.Parameters parameters = camera.getParameters();
			Camera.Size previewSize = getBestSupportedSize(parameters.getSupportedPreviewSizes(),
					metrics);
            /*previewSize.width = 800;
            previewSize.height = 600;*/
			parameters.setPreviewSize(previewSize.width, previewSize.height);
			parameters.setPreviewFormat(ImageFormat.NV21);
			camera.setParameters(parameters);
			previewSizeX = previewSize.width;
			previewSizeY = previewSize.height;
//			faceTrackService.setSize(previewSize.width, previewSize.height);
			if (cameraPreviewListener != null) {
				cameraPreviewListener.onPreviewSize(previewSize.width, previewSize.height);
			}
			mFaceEngineManager.setSize(previewSize.width, previewSize.height);
			//camera.setDisplayOrientation(cameraOri);

			setCameraDisplayOrientation(activity, cameraId, camera);

			camera.setPreviewDisplay(holder);
			camera.setPreviewCallback(new Camera.PreviewCallback() {
				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					//获取人脸的位置信息
//					List<FaceInfo> fsdkFaces = faceTrackService.getFtfaces(data);
					List<Face> faces = mFaceEngineManager.detectFaces(data);
					//画出人脸的位置
					drawFaceRect(faces);
					//输出数据进行其他处理
					if ((cameraPreviewListener != null && faces.size() > 0) || LivenessActivity
							.flag == 0) {
						cameraPreviewListener.onPreviewData(data.clone(), faces);
					}
				}
			});
			camera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object[] searchFace(Face target, List<Face> source) {
		return mFaceEngineManager.searchFace(target, source);
	}

	private void drawFaceRect(List<Face> fsdkFaces) {
		//这里只获取最大的人脸
		if (fsdkFaces == null || fsdkFaces.size() <= 0) {
			return;
		}
//		int maxIndex = ImageUtils.findFTMaxAreaFace(fsdkFaces);
		if (surfce_rect != null) {
			Canvas canvas = surfce_rect.getHolder().lockCanvas();
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);

			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setStyle(Paint.Style.FILL);
			paint.setStrokeWidth(5);
			paint.setTextSize(80);

			if (fsdkFaces.size() > 0) {
				for (Face aft_fsdkFace : fsdkFaces) {
					Rect rect = new Rect(aft_fsdkFace.getFaceInfo().getRect());

					//画人脸框
					// faceManager:1280 * 768
					// canvas:720 * 1096
					Rect adjustedRect = DrawUtils.adjustRect(rect, mFaceEngineManager.getmWidth(),
							mFaceEngineManager.getmHeight(),
							canvas.getWidth(), canvas.getHeight(), cameraOri, cameraId);
					// adjustedRect:Rect(-380, 546 - 620, 978)
					DrawUtils.drawFaceRect(canvas, adjustedRect, Color.YELLOW, 4);

					Rect rect1 = DrawUtils.adjustRect(rect, previewSizeX, previewSizeY,
							canvas.getWidth(), canvas.getHeight(), cameraOri, cameraId);
					if (rect1.right < previewSizeX - 100) {
						canvas.drawText("张三", rect1.right + 30, rect1.bottom, paint);
					} else {
						canvas.drawText("张三", rect1.left - 30, rect1.bottom, paint);
					}
				}
			}
			surfce_rect.getHolder().unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		closeCamera();
	}

	private void closeCamera() {
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();
		camera = null;
//		faceTrackService.destoryEngine();
	}

	public void destroyEngine() {
		mFaceEngineManager.destory();
	}

	private static class SingletonHolder {
		public static ArcFaceCamera INSTANCE = new ArcFaceCamera();
	}

	//设置相机预览分辨率，可以自己设置
	private Camera.Size getBestSupportedSize(List<Camera.Size> sizes, DisplayMetrics metrics) {
		Camera.Size bestSize = sizes.get(0);
		float screenRatio = (float) metrics.widthPixels / (float) metrics.heightPixels;
		if (screenRatio > 1) {
			screenRatio = 1 / screenRatio;
		}

		for (Camera.Size s : sizes) {
			if (Math.abs((s.height / (float) s.width) - screenRatio) < Math.abs(bestSize.height /
					(float) bestSize.width - screenRatio)) {
				bestSize = s;
			}
		}
		return bestSize;
	}

	public void setCameraPreviewListener(CameraPreviewListener cameraPreviewListener) {
		this.cameraPreviewListener = cameraPreviewListener;
	}


	//设置相机方向
	private void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware
			.Camera camera) {
		android.hardware.Camera.CameraInfo info =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		cameraOri = result;
		camera.setDisplayOrientation(result);
	}

	public int getPreviewHeight() {
		return surfce_preview.getHeight();
	}

	public int getPreviewWidth() {
		return surfce_preview.getWidth();
	}
}
