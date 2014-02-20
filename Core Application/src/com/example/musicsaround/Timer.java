package com.example.musicsaround;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

public class Timer
{
	private long currTime;
	// for debugging purposes
	TextView mView = null;
	// Handler mHandler = null;

	private CountDownTimer timer;
	private long mPrecision;
	private long futurePlayTime;
	private long mPlayPosition;
	private MediaPlayer mPlayer = null;

	// use the system time to check how much time has actually elapsed
	private long mReferenceTime;

	// minimum timer precision is 10 milliseconds
	public static final long MIN_TIMER_PRECISION = 10;

	// default timer precision, units in milliseconds
	public static final long DEFAULT_TIMER_PRECISION = 25;

	/**
	 * Creates a timer that another thread can receive call back messages, and
	 * it can count the time from any presision larger than 1 milliseconds
	 * 
	 * @param handler
	 *            - the handler to to receive call back messages from this timer
	 * @param timerPrecision
	 *            - the precision of the timer, units in milliseconds. e.g. if
	 *            set to 10 milliseconds, then the timer can count time up to 10
	 *            ms of precision
	 */
	public Timer(long timerPrecision)
	{
		mView = null;
		// mHandler = handler;

		// get the system time by default
		setCurrTime(System.currentTimeMillis());
		mReferenceTime = System.currentTimeMillis();

		if (timerPrecision < MIN_TIMER_PRECISION)
		{
			mPrecision = MIN_TIMER_PRECISION;
		}
		else
		{
			mPrecision = timerPrecision;
		}
	}

	// debugging constructor
	public Timer(TextView view, long timerPrecision)
	{
		mView = view;
		// mHandler = handler;

		// get the system time by default
		setCurrTime(System.currentTimeMillis());
		mReferenceTime = System.currentTimeMillis();

		if (timerPrecision < MIN_TIMER_PRECISION)
		{
			mPrecision = MIN_TIMER_PRECISION;
		}
		else
		{
			mPrecision = timerPrecision;
		}
	}

	public void startTimer()
	{
		// create a timer that will never expire, until we signal it to stop
		timer = new CountDownTimer(Long.MAX_VALUE, mPrecision)
		{

			/*
			 * Count the timer at the user defined precision interval. This call
			 * back method is synchronized so if content of the method takes too
			 * long, it will not throw off the timer
			 * 
			 * (non-Javadoc)
			 * 
			 * @see android.os.CountDownTimer#onTick(long)
			 */
			@Override
			public void onTick(long millisUntilFinished)
			{
				// we may not be able to meet the precision time :(
				// so check how much time has actually elapsed
				setCurrTime(currTime
						+ (System.currentTimeMillis() - mReferenceTime));

				mReferenceTime = System.currentTimeMillis();

				if (mPlayer != null)
				{
					if (futurePlayTime < currTime)
					{
						// Log.d("Music Timer", "Future time: " + futurePlayTime
						// + ", curr time: " + currTime);
						// NOTE: this media player needs to be able to play the
						// song ASAP! Meaning the music has to be bufferred and
						// ready to go. It also has to be cached, not just
						// MediaPlayer.prepare()
						// NOTE 2: The future play time may have already passed,
						// so we must catch up!
						mPlayer.seekTo((int) (currTime - futurePlayTime + mPlayPosition));
						mPlayer.start();
						// after we play the music, we have nothing to do with
						// the media player, so release its reference
						mPlayer = null;
					}
				}

				// if (mView != null)
				// {
				// Date date = new Date(getCurrTime());
				// SimpleDateFormat timeFormat = new SimpleDateFormat(
				// "hh:mm:ss.SSS");
				//
				// mView.setText(timeFormat.format(date));
				// }
			}

			/*
			 * We should never reach here... we need the timer to keep track of
			 * time till the user calls cancel
			 * 
			 * (non-Javadoc)
			 * 
			 * @see android.os.CountDownTimer#onFinish()
			 */
			@Override
			public void onFinish()
			{
				// we should never reach here...
				Log.e(this.getClass().getName(), "Timer unexpectedly stopped!");
			}
		};

		mReferenceTime = System.currentTimeMillis();

		timer.start();
	}

	public void stopTimer()
	{
		if (timer != null)
		{
			timer.cancel();
		}
	}

	// Getters and Setters
	// WARNING: outside this class, we are not able to retrieve the current time
	// to the precision we want. You will see a delay from the real time by
	// almost 1000 ms, so there is no point accessing this for precise time
	public long getCurrTime()
	{
		return currTime;
	}

	/**
	 * Must be synchronized to prevent multiple threads changing the time
	 * 
	 * @param currTime
	 *            - update the current time, units in milliseconds
	 */
	public synchronized void setCurrTime(long currTime)
	{
		if (currTime < 0)
		{
			this.currTime = 0;
		}
		else
		{
			this.currTime = currTime;
		}
	}

	/**
	 * The media player must be in good shape to play music!
	 * 
	 * @param mp
	 *            - a media player ready to play music
	 * @param futureTime
	 *            - the future time to play music
	 */
	public void playFutureMusic(MediaPlayer mp, long futureTime,
			long playPosition)
	{
		// we assume the media player is in a good state!
		futurePlayTime = futureTime;
		mPlayPosition = playPosition;

		// but don't play the music if we are near the end of the music
		if (currTime - futureTime < mp.getDuration() - 100)
		{
			mPlayer = mp;
		}
	}
}
