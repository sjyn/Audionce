package ai.com.audionce;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.github.rahatarmanahmed.cpv.CircularProgressView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

//TODO -- Hide status bar on all camera updates
//TODO -- disable buttons on save
@SuppressWarnings({"deprecation"})
public class CameraActivity extends AppCompatActivity {
    private Camera camera;
    private CameraPreview cPre;
    private final String SAVE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES) + "/picture.png";
    private boolean cameraFacingFront = false;
    private ImageButton switchView, resView, qui;
    private Button save, capture;
    private CircularProgressView cpv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_camera);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        switchView = (ImageButton) findViewById(R.id.imageButton);
        resView = (ImageButton) findViewById(R.id.imageButton2);
        qui = (ImageButton) findViewById(R.id.cancel_button);
        save = (Button) findViewById(R.id.save_button);
        capture = (Button) findViewById(R.id.capture_button);
        cpv = (CircularProgressView) findViewById(R.id.progress_view);
        cpv.setVisibility(View.GONE);
        camera = getCameraInstance();
        initializeCamera();
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, picCallback);
            }
        });
    }

    private void initializeCamera() {
        cPre = new CameraPreview(this, camera);
        FrameLayout fl = (FrameLayout) findViewById(R.id.camera_preview);
        fl.removeAllViews();
        fl.addView(cPre);
        fl.addView(switchView);
        fl.addView(resView);
        fl.addView(qui);
    }

    public void onSavePressed(View v) {
        new AsyncTask<Void, Void, Boolean>() {
            private File file;

            @Override
            public void onPreExecute() {
                super.onPreExecute();
                file = new File(SAVE_PATH);
                cpv.setVisibility(View.VISIBLE);
                cpv.startAnimation();

            }

            @Override
            public Boolean doInBackground(Void... v) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    Bitmap map = BitmapFactory.decodeStream(fis);
                    fis.close();
                    Matrix mat = new Matrix();
                    if (!cameraFacingFront)
                        mat.postRotate(90);
                    else
                        mat.postRotate(270);
                    map = Bitmap.createBitmap(map, 0, 0, map.getWidth(), map.getHeight(), mat, true);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    map.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(baos.toByteArray());
                    fos.close();
                } catch (Exception ex) {
                    return false;
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                cpv.clearAnimation();
                cpv.setVisibility(View.GONE);
                if (aBoolean) {
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    Intent intent = new Intent();
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            }
        }.execute();
    }

    private void onFailure() {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    Camera.PictureCallback picCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera cameraa) {
            capture.setText("Take New Picture");
            capture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        camera.stopPreview();
                    } catch (Exception ignored) {
                    }
                    camera.release();
                    camera = Camera.open(cameraFacingFront ?
                            Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
                    initializeCamera();
                    capture.setText("Capture");
                    capture.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            camera.takePicture(null, null, picCallback);
                        }
                    });
                }
            });
            try {
                File file = new File(SAVE_PATH);
                if (!file.exists())
                    file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
            } catch (Exception ex) {
                onFailure();
            }
        }
    };

    private Camera getCameraInstance() {
        Camera cam = null;
        try {
            cam = Camera.open();
        } catch (Exception ex) {
        }
        return cam;
    }

    public void toggleCameraLense(View v) {
        try {
            camera.stopPreview();
        } catch (Exception ignored) {
        }
        camera.release();
        if (cameraFacingFront) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } else {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        cameraFacingFront = !cameraFacingFront;
        initializeCamera();
    }

    public void adjustResolution(View v) {
        final Camera.Parameters params = camera.getParameters();
        final List<Camera.Size> sizes = params.getSupportedPictureSizes();
        sizes.remove(0);
        CharSequence[] array = new CharSequence[sizes.size()];
        for (int i = 0; i < array.length; i++) {
            Camera.Size s = sizes.get(i);
            array[i] = "" + s.width + " x " + s.height;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Change Resolution")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Camera.Size size = sizes.get(which);
                        camera.stopPreview();
                        camera.release();
                        camera = Camera.open(cameraFacingFront ?
                                Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK);
                        Camera.Parameters paramss = camera.getParameters();
                        paramss.setPictureSize(size.width, size.height);
                        camera.setParameters(paramss);
                        initializeCamera();
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    public void onCancelClick(View v) {
        setResult(RESULT_CANCELED, new Intent());
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder sh;
        private Camera camera;

        public CameraPreview(Context c, Camera cam) {
            super(c);
            camera = cam;
            sh = getHolder();
            sh.addCallback(this);
            sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.setDisplayOrientation(90);
                camera.startPreview();
            } catch (Exception ignored) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                camera.stopPreview();
            } catch (Exception ignored) {
            }
            camera.release();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            try {
                camera.setPreviewDisplay(holder);
                camera.setDisplayOrientation(90);
                camera.startPreview();
            } catch (Exception ignored) {
            }
        }

    }
}
