package com.ecorunner.myapplication;

import android.app.Activity;
import android.os.Bundle;

public class GameActivity extends Activity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
    }
}