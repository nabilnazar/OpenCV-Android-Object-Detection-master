package com.example.akshika.opencvtest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import org.jetbrains.annotations.Nullable;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private final static int ALL_PERMISSIONS_RESULT = 107;
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private static final int PICK_IMAGE = 1;
    private static final String TAG = "sdsds";
    TextView tw;
    ImageView ImgView;
    Button btn;
    FeatureDetector detector;
    DescriptorExtractor descriptor;
    Mat descriptors1;
    Mat img1;
    MatOfKeyPoint keypoints1;
    ApiService apiService;
    boolean location;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermissions();
        initRetrofitClient();


        btn = (Button) findViewById(R.id.upload);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });


    }

    private void initRetrofitClient() {
        final OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        apiService = new Retrofit.Builder().baseUrl("http://06a74c802f47.ngrok.io").client(client).build().create(ApiService.class);
    }

    public void askPermissions() {

        permissions.add(CAMERA);
        permissions.add(WRITE_EXTERNAL_STORAGE);
        permissions.add(READ_EXTERNAL_STORAGE);
        permissionsToRequest = findUnAskedPermissions(permissions);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList<String> findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList<String> result = new ArrayList<String>();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;

    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "nabeeeeeeeeeeeeeeeeeeeeeeel OpenCV loaded");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();


            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (bitmap != null) {
                    img1 = new Mat();
                    Utils.bitmapToMat(bitmap, img1);

                    descriptors1 = new Mat();
                    keypoints1 = new MatOfKeyPoint();


                    detector = FeatureDetector.create(FeatureDetector.ORB);
                    descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);

                    detector.detect(img1, keypoints1);
                    descriptor.compute(img1, keypoints1, descriptors1);


                    tw = (TextView) findViewById(R.id.text);
                    tw.setText(String.valueOf(descriptors1));



                    File filesDir = getApplicationContext().getFilesDir();
                    File file = new File(filesDir, "image" + ".png");


                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
                    byte[] bitmapdata = bos.toByteArray();


                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"),file);
                    MultipartBody.Part body = MultipartBody.Part.createFormData("upload", file.getName(), reqFile);
                    MultipartBody.Part descriptors = MultipartBody.Part.createFormData("descriptors",(String.valueOf(descriptors1)));
                    RequestBody name = RequestBody.create(MediaType.parse("text/plain"), "upload");

                    Call<ResponseBody> req = apiService.postImage(body,name,descriptors);
                    req.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                            if (response.code() == 200) {
                                Toast.makeText(getApplicationContext(), "Successfully uploaded", Toast.LENGTH_LONG).show();

                            }

                            Toast.makeText(getApplicationContext(), response.code() + " ", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getApplicationContext(), "failed uploaded", Toast.LENGTH_LONG).show();
                            Toast.makeText(getApplicationContext(), "Request failed", Toast.LENGTH_SHORT).show();
                            t.printStackTrace();
                        }
                    });

                } else {
                    Toast.makeText(getApplicationContext(), "bitmap value is null", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            ImgView = (ImageView) findViewById(R.id.imageview);
            ImgView.setImageBitmap(bitmap);

        }
    }


    }

