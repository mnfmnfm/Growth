package com.ferreirae.growth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.amplify.generated.graphql.UpdateMenteeMutation;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import type.UpdateMenteeInput;

public class MyProfileActivity extends AppCompatActivity {

    static String TAG = "mnf";
    public static List<String> mentors;
    static {
        mentors = new LinkedList<>();
        mentors.add("Quang");
        mentors.add("Merry");
        mentors.add("James");
        mentors.add("Jon");
        mentors.add("Sharina");
    }
    AWSAppSyncClient mAWSAppSyncClient;
    private Set<String> currentMentors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String username = p.getString("username", null);
        currentMentors = p.getStringSet("mentors", new HashSet<String>());
        TextView textView = findViewById(R.id.textView3);
        textView.setText(username);

        // Get the intent that started this activity
        Intent intent = getIntent();

        // Figure out what to do based on the intent type
        String type = intent.getType();
        if (type != null && type.contains("image/")) {
            // Handle intents with image data ...
            Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                imagePicked(imageUri);
            }

        }
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                Log.i(TAG, "AWSMobileClient initialized. User State is " + userStateDetails.getUserState());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Initialization error.", e);
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Set<String> mentors = p.getStringSet("mentors", new HashSet<String>());
        Log.i("mnf", mentors.toString());
        Log.i("mnf", currentMentors.toString());
        UpdateMenteeInput input = UpdateMenteeInput.builder()
                .mentors(new LinkedList<String>(mentors))
                .id(p.getString("id", null))
                .build();
        mAWSAppSyncClient.mutate(UpdateMenteeMutation.builder()
            .input(input)
            .build())
                .enqueue(new GraphQLCall.Callback<UpdateMenteeMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<UpdateMenteeMutation.Data> response) {
                        currentMentors = mentors;
                        Log.i("mnf", response.toString());
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e("mnf", e.toString());
                    }
                });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 777 && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            imagePicked(selectedImage);

        }
    }

    private void imagePicked(Uri uri) {
        ImageView imageView = findViewById(R.id.imageView);
        imageView.setImageURI(uri);

        // try to upload that image

        uploadWithTransferUtility(uri);
    }

    public void pickImage(View v) {
        Log.d("mnf", "button clicked");

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            Intent i = new Intent(
                    Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, 777);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        if(requestCode != 0) {
            return;
        }
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(
                    Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, 777);
        }
    }

    public void goToMentorActivity(View v) {
        Intent i = new Intent(this, MentorActivity.class);
        i.putExtra("mentorIndex", 0);
        startActivity(i);
    }

    public void uploadWithTransferUtility(Uri uri) {

        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = getContentResolver().query(uri,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        // String picturePath contains the path of selected Image
        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                        .build();

        final String uuid = UUID.randomUUID().toString();
        TransferObserver uploadObserver =
                transferUtility.upload(
                        "public/" + uuid,
                        new File(picturePath), CannedAccessControlList.PublicRead);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    UpdateMenteeInput input = UpdateMenteeInput.builder()
                            .id(p.getString("id", null))
                            .pictureUrl("public/" + uuid)
                            .build();
                    mAWSAppSyncClient.mutate(UpdateMenteeMutation.builder().input(input).build())
                            .enqueue(new GraphQLCall.Callback<UpdateMenteeMutation.Data>() {
                                @Override
                                public void onResponse(@Nonnull Response<UpdateMenteeMutation.Data> response) {
                                    Log.i("mnf", "did the update");
                                }

                                @Override
                                public void onFailure(@Nonnull ApolloException e) {

                                }
                            });
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d(TAG, "Bytes Transferred: " + uploadObserver.getBytesTransferred());
        Log.d(TAG, "Bytes Total: " + uploadObserver.getBytesTotal());
    }
}
