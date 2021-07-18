package com.example.youtubedownloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.material.textfield.TextInputLayout;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.io.File;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.io.FileUtils;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private com.yausername.ffmpeg.FFmpeg fFmpeg;
    private Button button;
    private ProgressBar Loading;
    private TextView DLStatus;
    private boolean downloading0 = false;
    private boolean downloading= false;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private DownloadProgressCallback callback = new DownloadProgressCallback() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onProgressUpdate(float progress, long etaInSeconds) {
            runOnUiThread(() -> {
                        // Download Status Text
                        downloading0 = true;
                        DLStatus.setVisibility(View.VISIBLE);
                        DLStatus.setText(progress + "% (ETA " + etaInSeconds + " seconds)");
                    }
            );
        }
    };
    private static final String TAG = "YouTube DL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FileUtils.deleteQuietly(this.getCacheDir());
        deleteCache(this);
        System.out.println("PERFECTTTTTTTTTT");
        // Setting up Variables
//        com.yausername.ffmpeg.FFmpeg fFmpeg;
//        try {
//            ffmpeg.loadBinary(new  LoadBinaryResponseHandler() {
//                @Override
//                public void onStart() {
//                    Log.d(TAG,"ffmpeg load binary started...");
//                }
//
//                @Override
//                public void onFailure() {
//                    Log.e(TAG,"ffmpeg load binary failure...");
//                }
//
//                @Override
//                public void onSuccess() {
//                    Log.d(TAG,"ffmpeg load binary success...");
//                }
//
//                @Override
//                public void onFinish() {
//                    Log.d(TAG,"ffmpeg load binary finish...");
//                }
//            });
//        } catch (FFmpegNotSupportedException e) {
//            // Handle if FFmpeg is not supported by device
//        }

        // initializing YouTubeDL
        try {
            YoutubeDL.getInstance().init(getApplication());
            FFmpeg.getInstance().init(getApplication());
            Thread thread = new Thread(() -> {
                try  {
                    // trying to update to the newest version
                    YoutubeDL.getInstance().updateYoutubeDL(getApplication());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        } catch (YoutubeDLException e) {
            Toast.makeText(this, String.format("%s: failed to initialize %s", TAG ,e), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "failed to initialize", e);
        }

        initViews();
        initListeners();


    }

    private void initViews() {
        // Accessing The Ui
        button = findViewById((R.id.DButton));
        DLStatus = findViewById(R.id.DownloadStatus);
        Loading = findViewById(R.id.progressStatus);
    }
    private void initListeners() {
        button.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.DButton) {
            try {
                startDownload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void startDownload() {
        try {
            FileUtils.deleteQuietly(this.getCacheDir());
            deleteCache(this);

        } catch (Exception e) {
            Log.e(TAG, "Error");
        }
        if (downloading) {
            Toast.makeText(this, "Cannot Start Download. A download Is Already In progress,", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isStoragePermissionGranted()) {
            Toast.makeText(this, "Grant Storage Permission And Retry", Toast.LENGTH_LONG).show();
            return;
        }
        TextInputLayout textInputLayout = findViewById(R.id.textInputLayout);
        Editable Url = Objects.requireNonNull(textInputLayout.getEditText()).getText();
        if (TextUtils.isEmpty(Url.toString())) {
            Toast.makeText(this, "Put Url Please", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Download Options")
                .setMessage("Which Type Of The Video You Want To Download?")
                .setIcon(R.drawable.ytdownload)
                .setNegativeButton("          Mp4 (video)",
                        (dialog, id) -> {
                                YoutubeDLRequest request = new YoutubeDLRequest(Url.toString());
                                File youtubeDLDir = getDownloadLocation();
                                request.addOption("-f", "best");
                                request.addOption("--verbose");
                                request.addOption("-o",youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");


                                showStart();

                                downloading = true;
                                Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(youtubeDLResponse -> {
                                            Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                            Loading.setVisibility(View.GONE);
                                            DLStatus.setVisibility(View.GONE);
                                            downloading = false;
                                            downloading0 = false;
                                            }, e -> {
                                                if(BuildConfig.DEBUG) Log.e(TAG,  "Failed To Download", e);
                                                if (downloading0) {
                                                    Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Failed to Download: Not a valid URL", Toast.LENGTH_LONG).show();
                                                }
                                                Loading.setVisibility(View.GONE);
                                                DLStatus.setVisibility(View.GONE);
                                                downloading = false;
                                                downloading0 = false;
                                            });
                                        compositeDisposable.add(disposable);
                                dialog.cancel();
                        })

                .setNeutralButton("          Thumbnail (Preview Image)",
                        (dialog, id) -> {
                            YoutubeDLRequest request = new YoutubeDLRequest(Url.toString());
                            File youtubeDLDir = getDownloadLocation();
                            request.addOption("--write-thumbnail");
                            request.addOption("--skip-download");
                            request.addOption("--verbose");
                            request.addOption("-o",youtubeDLDir.getAbsolutePath() + "/%(title)s.%(ext)s");


                            showStart();

                            downloading = true;
                            Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(youtubeDLResponse -> {
                                        Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                        Loading.setVisibility(View.GONE);
                                        DLStatus.setVisibility(View.GONE);
                                        downloading = false;
                                        downloading0 = false;
                                    }, e -> {
                                        if(BuildConfig.DEBUG) Log.e(TAG,  "Failed To Download", e);
                                        if (downloading0) {
                                            Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Failed to Download: Not a valid URL", Toast.LENGTH_LONG).show();
                                        }
                                        Loading.setVisibility(View.GONE);
                                        DLStatus.setVisibility(View.GONE);
                                        downloading = false;
                                        downloading0 = false;
                                    });
                            compositeDisposable.add(disposable);
                            dialog.cancel();
                        })
                .setPositiveButton("          Mp3 (Audio)",
                        (dialog, id) -> {
                            YoutubeDLRequest request = new YoutubeDLRequest(Url.toString());
                            File youtubeDLDir = getDownloadLocation();
                            //        request.addOption("--write-thumbnail");
                            //        request.addOption("--skip-download");
                            request.addOption("--extract-audio");
                            request.addOption("--audio-format");
                            request.addOption("mp3");
                            //        request.addOption("--format");
                            //        request.addOption("mp4");
                            request.addOption("-o",youtubeDLDir.getAbsolutePath() + "/%(title)s.%(id)s.%(ext)s");


                            showStart();

                            downloading = true;
                            Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, callback))
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(youtubeDLResponse -> {
                                        Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                        Loading.setVisibility(View.GONE);
                                        DLStatus.setVisibility(View.GONE);
                                        downloading = false;
                                        downloading0 = false;
                                    }, e -> {
                                        if(BuildConfig.DEBUG) Log.e(TAG,  "Failed To Download", e);
                                        if (downloading0) {
                                            Toast.makeText(MainActivity.this, "The Video Downloaded Successfully!", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "Failed to Download: Not a valid URL", Toast.LENGTH_LONG).show();
                                        }
                                        Loading.setVisibility(View.GONE);
                                        DLStatus.setVisibility(View.GONE);
                                        downloading = false;
                                        downloading0 = false;
                                    });
                            compositeDisposable.add(disposable);

                            dialog.cancel();
                        }).show();
    }



    private File getDownloadLocation() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File youtubeDLDir = new File(downloadsDir, "YouTubeDL");
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir();
        return youtubeDLDir;
    }
    private void showStart() {
        Toast.makeText(this, "The Video Started Downloading...", Toast.LENGTH_SHORT).show();
        Loading.setVisibility(View.VISIBLE);
    }
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }
    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception ignored) {}
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}





