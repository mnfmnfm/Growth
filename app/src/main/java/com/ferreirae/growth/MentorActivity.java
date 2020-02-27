package com.ferreirae.growth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

public class MentorActivity extends AppCompatActivity {

    int mentorIndex;
    String mentorName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentor);

        mentorIndex = getIntent().getIntExtra("mentorIndex", MyProfileActivity.mentors.size() - 1);
        mentorName = MyProfileActivity.mentors.get(mentorIndex);

        TextView textView = findViewById(R.id.textView5);
        textView.setText(mentorName);
    }

    public void goToNextMentor(View v) {
        if(v.getId() == R.id.button3) {
            // it's the connect button! make a connection
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Set<String> mentors = p.getStringSet("mentors", new HashSet<String>());
            mentors.add(mentorName);
            SharedPreferences.Editor e = p.edit();
            e.putStringSet("mentors", mentors);
            e.apply();
            Log.i("mnf", "adding " + mentorName + "to mentors");
        } else {
            Log.i("mnf", "not adding " + mentorName + " to mentors");
        }
        if(mentorIndex == MyProfileActivity.mentors.size() -1) {
            // don't go to next mentor, just finish
            finish();
        } else {
            Intent i = new Intent(this, MentorActivity.class);
            i.putExtra("mentorIndex", this.mentorIndex + 1);
            startActivity(i);
            finish();
        }
    }
}
