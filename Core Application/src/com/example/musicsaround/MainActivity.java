package com.example.musicsaround;

import com.example.musicsaround.dj.DJActivity;
import com.example.musicsaround.speaker.SpeakerActivity;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity
{
	// public key for other activities to access to figure out the mode
	public final static String MODE = "MODE";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onBtnDJ(View view)
	{
		Intent intent = new Intent(this, DJActivity.class);
		intent.putExtra(MODE, DJActivity.DJ_MODE);
		startActivity(intent);
	}

	public void onBtnSp(View view)
	{
		Intent intent = new Intent(this, SpeakerActivity.class);
		intent.putExtra(MODE, SpeakerActivity.SPEAKER_MODE);
		startActivity(intent);
	}
}
