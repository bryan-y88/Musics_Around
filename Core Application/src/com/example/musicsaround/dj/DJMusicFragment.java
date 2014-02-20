package com.example.musicsaround.dj;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
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
import android.widget.Toast;

import com.example.musicsaround.R;
import com.example.musicsaround.SongsManager;
import com.example.musicsaround.Timer;
import com.example.musicsaround.Utilities;

public class DJMusicFragment extends Fragment implements OnCompletionListener,
		SeekBar.OnSeekBarChangeListener
{
	// Json object for client server communication
	private String isbtnPlay = "no";

	private ImageButton btnPlay;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist;
	private ImageButton btnRepeat;
	private ImageButton btnShuffle;
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	private ProgressDialog syncProgress;

	// Media Player
	private MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;
	private SongsManager songManager;
	private Utilities utils;
	private int currentSongIndex = 0;
	// for resuming music
	private int currentPlayPosition = PLAY_FROM_BEGINNING;
	private boolean isShuffle = false;
	private boolean isRepeat = false;
	private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();

	private DJActivity mActivity = null;
	private View mContentView = null;
	private Timer musicTimer = null;
	private final static long DELAY = 4500;
	private final static int PLAY_FROM_BEGINNING = 0;
	private final String TAG = "DJ Music Player";

	private String[] musicList = { "9129", "9231", "9232" };
	protected String selectMusic = new String();

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		mActivity = (DJActivity) activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);
		mContentView = inflater.inflate(R.layout.fragment_dj_music, null);

		// All player buttons
		btnPlay = (ImageButton) mContentView.findViewById(R.id.btnPlay);
		btnNext = (ImageButton) mContentView.findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) mContentView.findViewById(R.id.btnPrevious);
		btnPlaylist = (ImageButton) mContentView.findViewById(R.id.btnPlaylist);
		btnRepeat = (ImageButton) mContentView.findViewById(R.id.btnRepeat);
		btnShuffle = (ImageButton) mContentView.findViewById(R.id.btnShuffle);
		songProgressBar = (SeekBar) mContentView
				.findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) mContentView.findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) mContentView
				.findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) mContentView
				.findViewById(R.id.songTotalDurationLabel);

		// prepare for a progress bar dialog
		syncProgress = new ProgressDialog(mActivity,
				AlertDialog.THEME_HOLO_DARK);
		syncProgress.setCancelable(false);
		syncProgress.setInverseBackgroundForced(true);
		syncProgress.setMessage("Get Ready to Enjoy Your Awesome Music!");
		syncProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		// Mediaplayer
		mp = new MediaPlayer();
		songManager = new SongsManager();
		utils = new Utilities();

		// Listeners
		songProgressBar.setOnSeekBarChangeListener(this); // Important
		mp.setOnCompletionListener(this); // Important

		// Getting all songs list
		songsList = songManager.getPlayList();

		// By default play first song
		// add time synchronize here()

		/**
		 * Play button click event plays a song and changes button to pause
		 * image pauses a song and changes button to play image
		 */
		btnPlay.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				// check for already playing, if it is, this acts as a pause
				// button
				if (mp.isPlaying())
				{
					if (mp != null)
					{
						// pause music play, and save the current playing
						// position
						mp.pause();
						currentPlayPosition = mp.getCurrentPosition();

						((DJActivity) mActivity).stopRemoteMusic();

						// Changing button image to play button
						btnPlay.setImageResource(R.drawable.btn_play);
					}
				}
				else
				{
					// Resume song
					if (mp != null)
					{
						// resume music play
						playSong(currentSongIndex, currentPlayPosition);

						// Changing button image to pause button
						btnPlay.setImageResource(R.drawable.btn_pause);
					}
				}

			}
		});

		/**
		 * Next button click event Plays next song by taking currentSongIndex +
		 * 1
		 */
		btnNext.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				if (isShuffle)
				{
					// shuffle is on - play a random song
					Random rand = new Random();
					currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
					playSong(currentSongIndex, PLAY_FROM_BEGINNING);
				}
				else
				{
					// check if next song is there or not
					if (currentSongIndex < (songsList.size() - 1))
					{
						playSong(currentSongIndex + 1, PLAY_FROM_BEGINNING);
						currentSongIndex = currentSongIndex + 1;
					}
					else
					{
						// play first song
						playSong(0, PLAY_FROM_BEGINNING);
						currentSongIndex = 0;
					}
				}
			}
		});

		/**
		 * Back button click event Plays previous song by currentSongIndex - 1
		 */
		btnPrevious.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				if (isShuffle)
				{
					// shuffle is on - play a random song
					Random rand = new Random();
					currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
					playSong(currentSongIndex, PLAY_FROM_BEGINNING);
				}
				else
				{
					if (currentSongIndex > 0)
					{
						playSong(currentSongIndex - 1, PLAY_FROM_BEGINNING);
						currentSongIndex = currentSongIndex - 1;
					}
					else
					{
						// play last song
						playSong(songsList.size() - 1, PLAY_FROM_BEGINNING);
						currentSongIndex = songsList.size() - 1;
					}
				}
			}
		});

		/**
		 * Button Click event for Repeat button Enables repeat flag to true
		 */
		btnRepeat.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				if (isRepeat)
				{
					isRepeat = false;
					Toast.makeText(mContentView.getContext(), "Repeat is OFF",
							Toast.LENGTH_SHORT).show();
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}
				else
				{
					// make repeat to true
					isRepeat = true;
					Toast.makeText(mContentView.getContext(), "Repeat is ON",
							Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isShuffle = false;
					btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}
			}
		});

		/**
		 * Button Click event for Shuffle button Enables shuffle flag to true
		 */
		btnShuffle.setOnClickListener(new View.OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				if (isShuffle)
				{
					isShuffle = false;
					Toast.makeText(mContentView.getContext(), "Shuffle is OFF",
							Toast.LENGTH_SHORT).show();
					btnShuffle.setImageResource(R.drawable.btn_shuffle);
				}
				else
				{
					// make repeat to true
					isShuffle = true;
					Toast.makeText(mContentView.getContext(), "Shuffle is ON",
							Toast.LENGTH_SHORT).show();
					// make shuffle to false
					isRepeat = false;
					btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
					btnRepeat.setImageResource(R.drawable.btn_repeat);
				}
			}
		});

		/**
		 * Button Click event for Play list click event Launches list activity
		 * which displays list of songs
		 */
		btnPlaylist.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				// Intent i = new Intent(mContentView.getContext(),
				// PlayListActivity.class);
				// startActivityForResult(i, 100);

				AlertDialog.Builder builder = new AlertDialog.Builder(
						mActivity, AlertDialog.THEME_HOLO_DARK);

				musicList = getMusicList().toArray(
						new String[getMusicList().size()]);
				builder.setTitle("Select Your Favorite Music!");
				builder.setSingleChoiceItems(musicList, -1,
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int item)
							{
								currentSongIndex = item;
							}
						});

				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int position)
							{
								// play the user selected music
								playSong(currentSongIndex, PLAY_FROM_BEGINNING);
							}
						});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		});

		return mContentView;
	}

	// get array list from sd card
	private ArrayList<String> getMusicList()
	{
		ArrayList<HashMap<String, String>> songsListData = new ArrayList<HashMap<String, String>>();
		ArrayList<String> musicList = new ArrayList<String>();
		SongsManager plm = new SongsManager();
		// get all songs from sdcard
		this.songsList = plm.getPlayList();

		// looping through playlist
		for (int i = 0; i < songsList.size(); i++)
		{
			// creating new HashMap
			HashMap<String, String> song = songsList.get(i);
			// adding HashList to ArrayList
			songsListData.add(song);
		}

		for (int i = 0; i < songsListData.size(); i++)
		{
			// creating new HashMap
			musicList.add(songsListData.get(i).get("songTitle"));
			// adding HashList to ArrayList
		}

		return musicList;
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
	// DJModeActivity.super.onBackPressed();
	// }
	// }).create().show();
	//
	// }
	// /**
	// * Receiving song index from playlist view
	// * and play the song
	// * */
	// @Override
	// protected void onActivityResult(int requestCode,
	// int resultCode, Intent data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// if(resultCode == 100){
	// currentSongIndex = data.getExtras().getInt("songIndex");
	// // play selected song
	// playSong(currentSongIndex);
	// }
	// }

	/**
	 * Function to play a song
	 * 
	 * @param songIndex
	 *            - index of song
	 */

	public void startSyncDialog()
	{
		syncProgress.show();

		new Thread(new Runnable()
		{
			public void run()
			{

				try
				{
					Thread.sleep(DELAY);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// close the progress bar dialog
				syncProgress.dismiss();
			}
		}).start();
	}

	// for DJ, input is index
	// for speaker, input is Url
	public void playSong(int songIndex, int playPosition)
	{
		// Play song
		try
		{
			// show the spinner and stop all user actions
			startSyncDialog();

			// first stop the remote music
			((DJActivity) mActivity).stopRemoteMusic();

			if (songsList.isEmpty())
			{
				Toast.makeText(mContentView.getContext(), "Empty Playlist",
						Toast.LENGTH_SHORT).show();
				return;
			}
			else if (songsList.get(songIndex) == null)
			{
				Toast.makeText(mContentView.getContext(),
						"Cannot play this song", Toast.LENGTH_SHORT).show();
				return;
			}

			String musicFPath = songsList.get(songIndex).get("songPath");
			// Displaying Song title
			String songTitle = songsList.get(songIndex).get("songTitle");
			mp.reset();

			// get the music timer
			musicTimer = mActivity.getTimer();

			// music path : SD for DJ mode, UrI for speaker mode
			mp.setDataSource(musicFPath);
			// mp.setDataSource("http://www.andrew.cmu.edu/user/yisongw/apptest/HungarianDance.mp3");
			songTitleLabel.setText("Now Playing: " + songTitle);

			// Changing Button Image to pause image
			btnPlay.setImageResource(R.drawable.btn_pause);
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

			// playRemoteMusic, time sensitive
			long futurePlayTime = musicTimer.getCurrTime() + DELAY;

			((DJActivity) mActivity).playRemoteMusic(musicFPath,
					futurePlayTime, playPosition);

			// let the music timer determine when to play the future playback
			musicTimer.playFutureMusic(mp, futurePlayTime, playPosition);

			// set the song Progress bar values
			songProgressBar.setProgress(0);
			songProgressBar.setMax(100);

			// Updating the song progress bar
			updateProgressBar();
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
				// long currentDuration = mp.getCurrentPosition();
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

	/**
	 * 
	 * */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromTouch)
	{

	}

	/**
	 * When user starts moving the progress handler
	 */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	/**
	 * When user stops moving the progress handler
	 */
	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		// mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = mp.getDuration();

		// get the new playing position
		currentPlayPosition = utils.progressToTimer(seekBar.getProgress(),
				totalDuration);

		playSong(currentSongIndex, currentPlayPosition);
	}

	/**
	 * On Song Playing completed if repeat is ON play same song again if shuffle
	 * is ON play random song
	 */
	@Override
	public void onCompletion(MediaPlayer arg0)
	{
		// check for repeat is ON or OFF
		if (isRepeat)
		{
			// repeat is on play same song again
			playSong(currentSongIndex, PLAY_FROM_BEGINNING);
		}
		else if (isShuffle)
		{
			// shuffle is on - play a random song
			Random rand = new Random();
			currentSongIndex = rand.nextInt((songsList.size() - 1) - 0 + 1) + 0;
			playSong(currentSongIndex, PLAY_FROM_BEGINNING);
		}
		else
		{
			// no repeat or shuffle ON - play next song
			if (currentSongIndex < (songsList.size() - 1))
			{
				playSong(currentSongIndex + 1, PLAY_FROM_BEGINNING);
				currentSongIndex = currentSongIndex + 1;
			}
			else
			{
				// play first song
				playSong(0, PLAY_FROM_BEGINNING);
				currentSongIndex = 0;
			}
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		mp.release();
		mp = null;
	}

	public void AsynWrap()
	{

	}
}
