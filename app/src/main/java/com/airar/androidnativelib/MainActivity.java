package com.airar.androidnativelib;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.Signal;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.core.content.FileProvider;

public class MainActivity
        extends UnityPlayerActivity {
    @SuppressLint({"SdCardPath"})
    private String TAG = "Unity";
    public Context p_myContext;

    private String unityGameObjectname;
    private String methodName;

    @TargetApi(23)
    public void CallOnResume() {
        //
        Log.i("Unity", "callonresume");
        this.mUnityPlayer.resume();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.p_myContext = this;
    }

    @SuppressLint({"HandlerLeak"})
    private Handler mHandler = new Handler() {
        @SuppressLint({"HandlerLeak"})
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    UnityPlayer.UnitySendMessage(MainActivity.this.unityGameObjectname,
                            "Set3dTexture", "");
                    break;
                case 1:
                    UnityPlayer.UnitySendMessage(MainActivity.this.unityGameObjectname,
                            MainActivity.this.methodName, "Success");
                    break;
                case 2:
                    UnityPlayer.UnitySendMessage(MainActivity.this.unityGameObjectname,
                            MainActivity.this.methodName, "Fail");
            }
        }
    };

    // 갤러리에 사진 저장하기
    public void saveImageToGallery(byte[] imgFile, String fileName, String gameObjectName,
            String methodName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/*");
        // 파일을 write중이라면 다른곳에서 데이터요구를 무시하겠다는 의미입니다.
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver contentResolver = getContentResolver();
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = contentResolver.insert(collection, values);

        try {
            ParcelFileDescriptor pdf = contentResolver.openFileDescriptor(item, "w", null);
            if (pdf == null) {

            } else {
                InputStream inputStream = getImageInputStram(
                        BitmapFactory.decodeByteArray(imgFile, 0, imgFile.length));
                byte[] strToByte = getBytes(inputStream);
                FileOutputStream fos = new FileOutputStream(pdf.getFileDescriptor());
                fos.write(strToByte);
                fos.close();
                inputStream.close();
                pdf.close();
                contentResolver.update(item, values, null, null);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        values.clear();
        // 파일을 모두 write하고 다른곳에서 사용할 수 있도록 0으로 업데이트를 해줍니다.
        values.put(MediaStore.Images.Media.IS_PENDING, 0);
        contentResolver.update(item, values, null, null);

        this.unityGameObjectname = gameObjectName;
        this.methodName = methodName;

        Message msg2 = new Message();
        msg2.what = 1;
        this.mHandler.sendMessage(msg2);
    }

    private InputStream getImageInputStram(Bitmap bmp) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        byte[] bitmapData = bytes.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapData);

        return bs;
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }


    // 갤러리에 파일 추가하기
    private void galleryAddPic(String currentPhotoPath) {
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);

        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }

    // 갤러리에 사진 추가하기
    public void addImageToGallery(String path, String gameObjectName, String methodName) {
        File file = new File(path);
        if (file.exists()) {
            ContentValues values = new ContentValues();
            values.put("datetaken", System.currentTimeMillis());
            values.put("mime_type", "image/jpg");
            values.put("_data", file.getAbsolutePath());

            galleryAddPic(file.getAbsolutePath());

            this.unityGameObjectname = gameObjectName;
            this.methodName = methodName;

            Message msg2 = new Message();
            msg2.what = 1;
            this.mHandler.sendMessage(msg2);
        } else {
            Log.i(TAG, "############# Content values Do not exists " + file.getAbsolutePath());
        }
    }

    // 갤러리에 비디오 추가하기
    public void addVideoToGallery(String path, String gameObjectName, String methodName) {
        File file = new File(path);
        if (file.exists()) {
            ContentValues values = new ContentValues();
            values.put("datetaken", System.currentTimeMillis());

            values.put("mime_type", "video/mp4");
            values.put("_data", file.getAbsolutePath());

            galleryAddPic(file.getAbsolutePath());

            this.unityGameObjectname = gameObjectName;
            this.methodName = methodName;
            Message msg2 = new Message();
            msg2.what = 1;
            this.mHandler.sendMessage(msg2);
        } else {
            Log.w(TAG, "############# Content values Do not exists " + file.getAbsolutePath());
        }
    }

    // 비디오 회전하기
    public void rotateVideo(String path, String gameObjectName, String methodName) {
        String[] objPath = path.split(";");
        String videoPath = objPath[0];
        String OutputPath = objPath[1];
        String _rotate = objPath[2];

        String[] arrayParm = new String[7];
        arrayParm[0] = "-i ";
        arrayParm[1] = videoPath;
        arrayParm[2] = " -metadata:s:v";
        arrayParm[3] = (" rotate=" + _rotate);
        arrayParm[4] = " -codec";
        arrayParm[5] = " copy";
        arrayParm[6] = OutputPath;

        String cmd = "-i " + videoPath + " -map_metadata 0 -metadata:s:v rotate=\"" + _rotate
                + "\" -codec copy " + OutputPath;

        Log.i("Unity", cmd);


        this.unityGameObjectname = gameObjectName;
        this.methodName = methodName;

        commandFFMpeg(this.p_myContext, cmd);
    }

    // FFMpeg 명령어 실행
    void commandFFMpeg(Context myContext, String cmd) {
        Config.ignoreSignal(Signal.SIGXCPU);

        int rc = FFmpeg.execute(cmd);

        if (rc == Config.RETURN_CODE_SUCCESS) {
            Message msg2 = new Message();
            msg2.what = 1;
            this.mHandler.sendMessage(msg2);
        } else if (rc == Config.RETURN_CODE_CANCEL) {
            Message msg2 = new Message();
            msg2.what = 2;
            this.mHandler.sendMessage(msg2);
        } else {
            Message msg2 = new Message();
            msg2.what = 2;
            this.mHandler.sendMessage(msg2);
        }
    }

    public void ShareImage(String imgPath) {
        File files = new File(imgPath);
        if (!files.exists()) {
            return;
        }
        Intent intentSend = new Intent("android.intent.action.SEND");
        intentSend.setType("image/*");

        //Uri uri = FileProvider.getUriForFile(this, "com.airar.trickeye.provider", new File
        // (imgPath));


        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(),
                "com.airar.androidnativelib.fileprovider", files);
        intentSend.putExtra(Intent.EXTRA_STREAM, contentUri);

        /*Uri uri = Uri.fromFile(new File(imgPath));
        File wallpaper_file = new File(uri.getPath());
        Uri contentURI = getImageContentUri(p_myContext, wallpaper_file.getAbsolutePath());*/

        intentSend.putExtra("android.intent.extra.STREAM", contentUri);

        startActivity(Intent.createChooser(intentSend, "공유하기"));
    }

    public void ShareVideo(String imgPath) {
        Intent intentSend = new Intent("android.intent.action.SEND");
        intentSend.setType("video/*");

        //Uri uri = FileProvider.getUriForFile(this, "com.airar.trickeye.provider", new File
        // (imgPath));
        //Uri uri = Uri.fromFile(new File(imgPath));
        Uri uri = Uri.fromFile(new File(imgPath));
        File wallpaper_file = new File(uri.getPath());
        Uri contentURI = getVideoContentUri(p_myContext, wallpaper_file.getAbsolutePath());

        intentSend.putExtra("android.intent.extra.STREAM", contentURI);

        startActivity(Intent.createChooser(intentSend, "공유하기"));
    }

    public void ShareText(String linkUrl) {
        Intent intentSend = new Intent("android.intent.action.SEND");

        intentSend.setType("text/plain");
        intentSend.putExtra("android.intent.extra.TEXT", linkUrl);
        startActivity(Intent.createChooser(intentSend, "공유하기"));
    }

    public static Uri getImageContentUri(Context context, String absPath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                , new String[]{MediaStore.Images.Media._ID}
                , MediaStore.Images.Media.DATA + "=? "
                , new String[]{absPath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    Integer.toString(id));

        } else if (!absPath.isEmpty()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, absPath);
            return context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            return null;
        }
    }

    public static Uri getVideoContentUri(Context context, String absPath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                , new String[]{MediaStore.Video.Media._ID}
                , MediaStore.Video.Media.DATA + "=? "
                , new String[]{absPath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    Integer.toString(id));

        } else if (!absPath.isEmpty()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATA, absPath);
            return context.getContentResolver().insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            return null;
        }
    }


    // 갤러리 이동
    public void OpenGallery() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("content://media/internal/images/media"));
        intent.setType("image/*");
        startActivity(intent);

    }

    // 퍼미션 체크하기
    public void CheckPermission(String _permissionStr, String gameObjectName,
            String gameMethodName) {
        this.unityGameObjectname = gameObjectName;
        this.methodName = gameMethodName;
        if (hasPermission(_permissionStr) == 0) {
            SendRequestResultToUnity("PermissionDenied");
        } else {
            SendRequestResultToUnity("PermissionAllow");
        }
    }

    public int hasPermission(String permissionStr) {
        if (Build.VERSION.SDK_INT < 23) {
            return 101;
        }
        Context context = UnityPlayer.currentActivity.getApplicationContext();
        if (context.checkCallingOrSelfPermission(permissionStr)
                == PackageManager.PERMISSION_GRANTED) {
            return 100;
        }
        return 0;
    }

    // 퍼미션 호출하기
    public void CallPermission(String _permissionStr, String gameObjectName,
            String gameMethodName) {
        this.unityGameObjectname = gameObjectName;
        this.methodName = gameMethodName;
        if (Build.VERSION.SDK_INT < 23) {
            SendRequestResultToUnity("OnVersion");
        } else {
            requestPermission(_permissionStr);
        }
    }

    @TargetApi(23)
    public void requestPermission(String permissionStr) {
        UnityPlayer.currentActivity.requestPermissions(new String[]{permissionStr}, 0);
    }

    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SendRequestResultToUnity("PermissionAllow");
                    //restartApp(this.p_myContext);
                } else if (shouldShowRequestPermissionRationale(permissions[0])) {
                    SendRequestResultToUnity("PermissionDenied");
                } else {
                    SendRequestResultToUnity("PermissionDenied");
                }
                break;
        }
    }

    private void SendRequestResultToUnity(String result) {
        UnityPlayer.UnitySendMessage(this.unityGameObjectname, this.methodName, result);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    public void restartApp(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);

        System.exit(0);
    }
}
