package com.example.musicsaround.speaker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.musicsaround.R;
import com.example.musicsaround.SongsManager;
import com.example.musicsaround.Timer;
import com.example.musicsaround.Utilities;

public class SpeakerMusicFragment extends Fragment
{

	// Json object for client server communication
	private final static String TAG = "Speaker Music Player";
	private String isbtnPlay = "no";

	private String songTitle = "default SongTitle";
	private String songInfo = "Sorry, web service is steamming now....";
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	// Media Player
	private MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;
	private SongsManager songManager;
	private Utilities utils;
	private Timer musicTimer = null;
	private SpeakerActivity mActivity = null;
	private SpeakerMusicFragment mFragment = null;
	private View mContentView = null;
	private int currentPlayPosition = 0;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		mActivity = (SpeakerActivity) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
		mFragment = this;

		mContentView = inflater.inflate(R.layout.fragment_speakermusic, null);

		// All player buttons
		songProgressBar = (SeekBar) mContentView
				.findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) mContentView.findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) mContentView
				.findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) mContentView
				.findViewById(R.id.songTotalDurationLabel);

		// Mediaplayer
		mp = new MediaPlayer();
		songManager = new SongsManager();
		utils = new Utilities();

		// get music info on speaker mode
		ImageButton button = (ImageButton) mContentView
				.findViewById(R.id.btnPlaylist);
		button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				// Perform action on click
				AlertDialog alertDialog = new AlertDialog.Builder(mActivity,
						AlertDialog.THEME_HOLO_DARK).create();
				alertDialog.setTitle("Music Info from Spotify");
				alertDialog.setMessage(songInfo);
				alertDialog.show(); // <-- See This!
			}
		});

		// should be put at asynctask

		// String url =
		// "http://192.168.49.1:9002/Justin Bieber Beauty & A Beat.mp3";
		// String temp[] = url.split("/");
		// songTitle = temp[3].split("\\.")[0];
		// try {
		// songTitle = URLEncoder.encode(songTitle, "utf-8");
		// } catch (UnsupportedEncodingException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// AsynWrap();

		return mContentView;
	}

	// @Override
	// public void onBackPressed(){
	//
	// new AlertDialog.Builder(this)
	// .setTitle("Really Exit?")
	// .setMessage("Are you sure you want to exit?")
	// .setNegativeButton(android.R.string.no, null)
	// .setPositiveButton(android.R.string.yes, new OnClickListener() {
	//
	// public void onClick(DialogInterface arg0, int arg1) {
	// SpeakerModeActivity.super.onBackPressed();
	// }
	// }).create().show();
	//
	// }

	/**
	 * t Function to play a song
	 * 
	 * @param songIndex
	 *            - index of song
	 */

	// for DJ, input is index
	// for speaker, input is Url
	public void playSong(String url, long startTime, int startPos)
	{
		// This part of the code is time sensitive, it should be done as fast as
		// possible to avoid the delay in the music
		try
		{
			mp.reset();
			mp.setDataSource(url);

			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

			mp.prepare();
			// TODO: make sure we have buffered REALLY
			// buffered the music, currently this is a big
			// HACK and takes a lot of time. We can do
			// better!
			mp.start();
			mp.pause();
			mp.start();
			mp.pause();
			mp.start();
			mp.pause();
			mp.start();
			mp.pause();
			mp.start();
			mp.pause();

			musicTimer = mActivity.retrieveTimer();

			// let the music timer determine when to play the future playback
			musicTimer.playFutureMusic(mp, startTime, startPos);

			// Changing Button Image to pause image
			// btnPlay.setImageResource(R.drawable.btn_pause);

			// set Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);

			// Updating progress bar
			updateProgressBar();
			// parsing songTitle

			String temp[] = url.split("/");
			String tempTitle[] = temp[temp.length - 1].split("\\.");

			// initialize songTitle
			songTitle = "";

			// song title could have "." in the name, so let's replace them with
			// space we also igmore then last "." because that signifies the
			// file extension
			for (int i = 0; i < tempTitle.length - 1; i++)
			{
				songTitle = songTitle + " " + tempTitle[i];
			}

			// set the song title after playing the music
			songTitleLabel.setText("Now Playing: " + songTitle);

			try
			{
				// filter out all numbers and strange characters
				songTitle = songTitle.replaceAll("[0-9-_(){}]", " ");
				songTitle = URLEncoder.encode(songTitle, "utf-8");
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e(TAG, e.getMessage());
			}
			AsynWrap();
		}
		catch (IllegalArgumentException e)
		{
			Log.e(TAG, "IllegalArgumentException");
		}
		catch (IllegalStateException e)
		{
			Log.e(TAG, "illeagalStateException");
		}
		catch (IOException e)
		{
			Log.e(TAG, "IOexception");
		}
	}

	public void stopMusic()
	{
		if (mp != null)
		{
			mp.pause();
		}
	}

	/**
	 * Update timer on seekbar
	 */
	public void updateProgressBar()
	{
		// initialize the bar
		long totalDuration = mp.getDuration();

		// Displaying Total Duration time
		songTotalDurationLabel.setText(""
				+ utils.milliSecondsToTimer(totalDuration));
		// Displaying time completed playing
		songCurrentDurationLabel.setText(""
				+ utils.milliSecondsToTimer(currentPlayPosition));

		// Updating progress bar
		int progress = (int) (utils.getProgressPercentage(currentPlayPosition,
				totalDuration));
		songProgressBar.setProgress(progress);

		mHandler.postDelayed(mUpdateTimeTask, 100);
	}

	/**
	 * Background Runnable thread for song progress
	 */
	private Runnable mUpdateTimeTask = new Runnable()
	{
		public void run()
		{
			if (mp == null)
			{
				return;
			}

			// only update the progress if music is playing
			if (mp.isPlaying())
			{
				long totalDuration = mp.getDuration();
				currentPlayPosition = mp.getCurrentPosition();

				// Displaying Total Duration time
				songTotalDurationLabel.setText(""
						+ utils.milliSecondsToTimer(totalDuration));
				// Displaying time completed playing
				songCurrentDurationLabel.setText(""
						+ utils.milliSecondsToTimer(currentPlayPosition));

				// Updating progress bar
				int progress = (int) (utils.getProgressPercentage(
						currentPlayPosition, totalDuration));
				songProgressBar.setProgress(progress);
			}

			// Running this thread after 100 milliseconds
			mHandler.postDelayed(this, 100);
		}
	};

	// /**
	// * On Song Playing completed
	// * if repeat is ON play same song again
	// * if shuffle is ON play random song
	// * */
	// @Override
	// public void onCompletion(MediaPlayer arg0) {
	//
	// }

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mp.release();
		mp = null;
	}

	// modify it to speaker asynctask
	public void AsynWrap()
	{
		// //handling asynctask
		AsyncTask<String, String, String> sendSource = new SpeakerAsynctask();
		String urlString = "http://ws.spotify.com/search/1/track?q="
				+ songTitle;
		sendSource.execute(urlString);
	}

	private class SpeakerAsynctask extends AsyncTask<String, String, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			String request = params[0];
			String responsesString = "default http response";
			HttpClient httpclient = new DefaultHttpClient();

			// Prepare a request object
			HttpGet httpget = new HttpGet(request);

			HttpResponse response;
			try
			{
				response = httpclient.execute(httpget);
				// Examine the response status
				Log.i("Http Response", response.getStatusLine().toString());

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				// If the response does not enclose an entity, there is no need
				// to worry about connection release

				if (entity != null)
				{
					// A Simple JSON Response Read
					InputStream instream = entity.getContent();
					// xml stream data
					DocumentBuilderFactory dbf = DocumentBuilderFactory
							.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					Document doc = db.parse(instream);

					// find artist, album,popularity
					String name = "Title: ";
					String artists = "Artist: ";
					String album = "Album: ";
					String popularity = "Popularity: ";

					// get all the track nodes s
					NodeList trackNodes = doc.getElementsByTagName("track");
					if (trackNodes == null)
					{
						// if music cannot be found
						songInfo = "Sorry, no information found on Spotify.";
					}
					else
					{
						// get all the children of track nodes
						NodeList childrenNodes = trackNodes.item(0)
								.getChildNodes();
						for (int i = 0; i < childrenNodes.getLength(); i++)
						{

							if (childrenNodes.item(i).getNodeName()
									.equals("name"))
							{
								name = name
										+ childrenNodes.item(i)
												.getTextContent();
							}
							if (childrenNodes.item(i).getNodeName()
									.equals("artist"))
							{

								NodeList artistNodes = childrenNodes.item(i)
										.getChildNodes();
								if (artistNodes.item(1).getNodeName()
										.equals("name"))
								{
									artists = artists
											+ artistNodes.item(1)
													.getTextContent();
								}

							}
							if (childrenNodes.item(i).getNodeName()
									.equals("album"))
							{
								NodeList AlbumNodes = childrenNodes.item(i)
										.getChildNodes();
								if (AlbumNodes.item(1).getNodeName()
										.equals("name"))
								{
									album = album
											+ AlbumNodes.item(1)
													.getTextContent();
								}
							}
							if (childrenNodes.item(i).getNodeName()
									.equals("popularity"))
							{
								popularity = popularity
										+ childrenNodes.item(i)
												.getTextContent();
								try
								{
									double popularityDouble = Double
											.parseDouble(popularity.split(" ")[1]) * 100;
									int popularityPercentage = (int) popularityDouble;
									popularity = "Popularity: "
											+ String.valueOf(popularityPercentage)
											+ "%";
								}
								// if we cannot convert popularity to a
								// percentage, then leave it to the original
								// text
								catch (NumberFormatException e)
								{
									Log.e(TAG, "Cannot convert popularity to %");
								}
							}
						}

						songInfo = name + "\n" + artists + "\n" + album + "\n"
								+ popularity + "\n";

						// songInfo
						// songInfo = convertStreamToString(instream);

						// now you have the string representation of the HTML
						// request
						instream.close();
					}
				}
				else
				{
					// if music cannot be found
					songInfo = "Sorry, no information found on Spotify.";
				}

				return responsesString;

			}
			catch (Exception e)
			{
				Log.e(TAG, request + " returned null data.", e);
				// if music cannot be found
				songInfo = "Sorry, no information found on Spotify.";
			}
			return responsesString;
		}

		// deal with stream json string
		private String convertStreamToString(InputStream is)
		{
			/*
			 * To convert the InputStream to String we use the
			 * BufferedReader.readLine() method. We iterate until the
			 * BufferedReader return null which means there's no more data to
			 * read. Each line will appended to a StringBuilder and returned as
			 * String.
			 */
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();

			String line = null;
			int tempCount = 0;
			try
			{
				while ((line = reader.readLine()) != null && tempCount < 4)
				{
					sb.append(line + "\n");
					tempCount++;
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					is.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			return sb.toString();
		}

		protected void onProgressUpdate(Integer... progress)
		{
		}

		protected void onPostExecute(Long result)
		{
		}
	}
}
