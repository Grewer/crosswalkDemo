package com.example.apple.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ValueCallback;

//import com.mingle.widget.LoadingView;

import com.github.jlmd.animatedcircleloadingview.AnimatedCircleLoadingView;

import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private AnimatedCircleLoadingView loading;
    private ValueCallback mUploadMessage;
    private final int FILE_SELECTED = 10000;
    private XWalkView mWebView;

    String url = "http://www.baidu.com";
//    String url = "https://crosswalk-project.org/";

    private String mCameraPhotoPath;

    private boolean isLoading = false;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        XWalkPreferences.setValue("enable-javascript", true);
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
        XWalkPreferences.setValue(XWalkPreferences.ALLOW_UNIVERSAL_ACCESS_FROM_FILE, true);
        XWalkPreferences.setValue(XWalkPreferences.SUPPORT_MULTIPLE_WINDOWS, true);

        mWebView = findViewById(R.id.xw);
        loading = findViewById(R.id.circle_loading_view);


        XWalkSettings mWebSettings = mWebView.getSettings();
        mWebSettings.setSupportZoom(true);//支持缩放
        mWebSettings.setBuiltInZoomControls(true);//可以任意缩放
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setUseWideViewPort(true);////将图片调整到适合webview的大小
        mWebSettings.setLoadsImagesAutomatically(true);

        mWebView.loadUrl(url);

        mWebView.setResourceClient(new XWalkResourceClient(mWebView) {
            @Override
            public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onLoadStarted(XWalkView view, String url) {
                super.onLoadStarted(view, url);
                if(isLoading) return;
                loading.startDeterminate();
                isLoading = true;
                Log.e("loading", "开始");
            }
            @Override
            public void onLoadFinished(XWalkView view, String url) {
                super.onLoadFinished(view, url);
            }

            @Override
            public void onProgressChanged(XWalkView view, int progressInPercent) {
                super.onProgressChanged(view, progressInPercent);

                loading.setPercent(progressInPercent);
                if (progressInPercent == 100) {
                    loading.removeAllViews();
                    mWebView.setVisibility(mWebView.VISIBLE);
                }
                Log.e("loading", "进度"+ progressInPercent);

            }

            @Override
            public void onReceivedLoadError(XWalkView view, int errorCode, String description, String failingUrl) {
                super.onReceivedLoadError(view, errorCode, description, failingUrl);
                view.loadDataWithBaseURL(null, "<span>页面加载失败,请确认网络是否连接</span>",
                        "text/html", "utf-8", null);

            }

        });

        mWebView.setUIClient(new XWalkUIClient(mWebView) {
            @Override
            public void openFileChooser(XWalkView view, ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                super.openFileChooser(view, uploadFile, acceptType, capture);
                if (mUploadMessage != null) return;
                mUploadMessage = uploadFile;


                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;

                    try {
                        //设置MediaStore.EXTRA_OUTPUT路径,相机拍照写入的全路径
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (Exception ex) {
                        // Error occurred while creating the File
                        Log.e("WebViewSetting", "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));

                    } else {
                        takePictureIntent = null;

                    }
                }

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, i);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "请选择图片");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                MainActivity.this.startActivityForResult(chooserIntent, FILE_SELECTED);
            }
        });

    }

    //在sdcard卡创建缩略图
    //createImageFileInSdcard
    @SuppressLint("SdCardPath")
    private File createImageFile() {
        File file=new File(Environment.getExternalStorageDirectory()+"/","tmp.png");
        mCameraPhotoPath=file.getAbsolutePath();
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_SELECTED&&mUploadMessage != null) {
            Uri result = null;
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        result = Uri.parse(mCameraPhotoPath);
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        result = Uri.parse(dataString);
                    }
                }
            }


            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
