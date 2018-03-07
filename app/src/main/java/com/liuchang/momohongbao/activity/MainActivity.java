package com.liuchang.momohongbao.activity;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.liuchang.momohongbao.R;
import com.liuchang.momohongbao.base.App;
import com.liuchang.momohongbao.base.BaseActivity;
import com.liuchang.momohongbao.model.bean.Hongbao;
import com.liuchang.momohongbao.model.bean.MessageEvent;
import com.liuchang.momohongbao.model.db.DaoSession;
import com.liuchang.momohongbao.model.db.HongbaoDao;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity implements AccessibilityManager.AccessibilityStateChangeListener {

    @BindView(R.id.tv_service_status)
    TextView tvServiceStatus;

    private AccessibilityManager accessibilityManager;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Image mImage;
    private String datapath;
    private TessBaseAPI mTess;
    private HongbaoDao dao;

    @Override
    protected int initLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager == null) {
            Toast.makeText(this, "您的手机不支持此助手", Toast.LENGTH_SHORT).show();
            return;
        }
        accessibilityManager.addAccessibilityStateChangeListener(this);
        updateUIStatus();
        EventBus.getDefault().register(this);

        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mMediaProjectionManager == null) {
            Toast.makeText(this, "您的手机不支持红包记录", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 0);

        DaoSession daoSession = App.getInstance().getDaoSession();
        dao = daoSession.getHongbaoDao();

        new Thread() {
            @Override
            public void run() {
                super.run();
                datapath = getFilesDir() + "/tesseract/";
                checkFile(new File(datapath + "tessdata/"));
                String language = "chi_sim";
                mTess = new TessBaseAPI();
                mTess.init(datapath, language);
            }
        }.start();
    }

    @OnClick({R.id.open, R.id.history})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.open://开启服务
                try {
                    Toast.makeText(this, R.string.turn_on_toast, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));//打开系统设置---无障碍
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.history:
                startActivity(new Intent(this, HBListActivity.class));
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final MessageEvent event) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                ImageReader imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, 0x1, 2);
                VirtualDisplay virtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                        metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(), null, null);

                try {
                    Thread.sleep(500);
                    mImage = imageReader.acquireLatestImage();
                    int width = mImage.getWidth();
                    int height = mImage.getHeight();
                    final Image.Plane[] planes = mImage.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    if (event.shouqi) {
                        bitmap = Bitmap.createBitmap(bitmap, width / 10, height / 4, width / 10 * 8, height / 8);
                    } else {
                        bitmap = Bitmap.createBitmap(bitmap, width / 10, height / 5 * 2, width / 10 * 8, height / 12);
                    }

                    String ocrResult;
                    mTess.setImage(bitmap);
                    mTess.setVariable("tessedit_char_whitelist", ".0123456789");
                    ocrResult = mTess.getUTF8Text();
                    Log.d("result", ocrResult);
                    insertData(ocrResult);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mImage.close();
                    virtualDisplay.release();
                }
            }
        }.start();
    }

    private void insertData(String data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String[] time = sdf.format(new Date()).split(" ");
        try {
            double result = Double.parseDouble(data);
            Hongbao hongbao = new Hongbao(time[0], time[1], result);
            dao.insert(hongbao);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/chi_sim.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/chi_sim.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/chi_sim.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIStatus();
    }

    @Override
    protected void onDestroy() {
        accessibilityManager.removeAccessibilityStateChangeListener(this);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        updateUIStatus();
    }

    /**
     * 更新当前 UI 显示状态
     */
    private void updateUIStatus() {
        if (isServiceConnected()) {
            tvServiceStatus.setText(R.string.service_is_connected);
            tvServiceStatus.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        } else {
            tvServiceStatus.setText(R.string.service_un_connected);
            tvServiceStatus.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }

    /**
     * 获取 Service 是否启用状态
     */
    private boolean isServiceConnected() {
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.service.HongbaoService")) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
//        onMessageEvent(new MessageEvent());
    }
}
