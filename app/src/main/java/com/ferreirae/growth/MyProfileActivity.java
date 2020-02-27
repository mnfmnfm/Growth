package com.ferreirae.growth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.amplify.generated.graphql.UpdateMenteeMutation;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.w3c.dom.Text;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import type.UpdateMenteeInput;

public class MyProfileActivity extends AppCompatActivity {

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

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String username = p.getString("username", null);
        currentMentors = p.getStringSet("mentors", new HashSet<String>());
        TextView textView = findViewById(R.id.textView3);
        textView.setText(username);

        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();
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

            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageURI(selectedImage);

//            String[] filePathColumn = { MediaStore.Images.Media.DATA };
//
//            Cursor cursor = getContentResolver().query(selectedImage,
//                    filePathColumn, null, null, null);
//            cursor.moveToFirst();
//
//            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//            String picturePath = cursor.getString(columnIndex);
//            cursor.close();

            // String picturePath contains the path of selected Image
        }
    }

    public void pickImage(View v) {
        Log.d("mnf", "button clicked");
        Intent i = new Intent(
                Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, 777);
    }

    public void goToMentorActivity(View v) {
        Intent i = new Intent(this, MentorActivity.class);
        i.putExtra("mentorIndex", 0);
        startActivity(i);
    }
}
