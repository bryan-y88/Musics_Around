package com.example.musicsaround.speaker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import com.example.musicsaround.Timer;
import com.example.musicsaround.dj.GroupOwnerSocketHandler;

public class ClientSocketHandler extends Thread
{
	private static final String TAG = "ClientSocketHandler";
	private Handler handler;
	private InetAddress mAddress;
	private Socket socket;

	// for syncing time with the server
	private Timer timer = null;

	// a time out for connecting to a server, unit is in milliseconds, 0 for
	// never timing out
	private static final int CONN_TIMEOUT = 0;

	private static final int BUFFER_SIZE = 256;

	public static final int EVENT_RECEIVE_MSG = 100;
	public static final int CLIENT_CALLBACK = 101;

	public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress,
			Timer timer)
	{
		this.handler = handler;
		this.mAddress = groupOwnerAddress;
		this.timer = timer;
	}

	@Override
	public void run()
	{
		// let the UI thread control the server
		handler.obtainMessage(CLIENT_CALLBACK, this).sendToTarget();

		// connect the socket first
		connect();

		// thread will stop when disconnect is called, at that point the socket
		// should be closed and nullified
		while (socket != null)
		{
			try
			{
				InputStream iStream = socket.getInputStream();
				OutputStream oStream = socket.getOutputStream();

				// clear the buffer before reading
				byte[] buffer = new byte[BUFFER_SIZE];
				int bytes;

				// Read from the InputStream
				bytes = iStream.read(buffer);
				if (bytes == -1)
				{
					continue;
				}

				// need to handle sync messages
				String recMsg = new String(buffer);

				String[] cmdString = recMsg
						.split(GroupOwnerSocketHandler.CMD_DELIMITER);

				Log.d(TAG, "Command received: " + recMsg);

				// *** Time Sync here ***
				// receiving messages should be as fast as possible to ensure
				// the successfulness of time synchronization
				if (cmdString[0].equals(GroupOwnerSocketHandler.SYNC_CMD)
						&& cmdString.length > 1)
				{
					// check if we have received a timer parameter, if
					// so, set the time, then send back an
					// Acknowledgment
					timer.setCurrTime(Long.parseLong(cmdString[1]));

					// just send the same message back to the server
					oStream.write(recMsg.getBytes());
					// Send the obtained bytes to the UI Activity
					Log.d(TAG, "Command sent: " + recMsg);
				}

				handler.obtainMessage(EVENT_RECEIVE_MSG, buffer).sendToTarget();
			}
			// this is an ok exception, because someone could have wanted this
			// connection to be closed in the middle of socket read
			catch (SocketException e)
			{
				Log.d(TAG, "Socket connection has ended.", e);
				disconnect();
			}
			catch (IOException e)
			{
				Log.e(TAG, "Unexpectedly disconnected during socket read.", e);
				disconnect();
			}
			catch (NumberFormatException e)
			{
				Log.e(TAG, "Cannot parse time received from server", e);
				disconnect();
			}
		}
	}

	public void connect()
	{
		if (socket == null || socket.isClosed())
		{
			socket = new Socket();
		}

		try
		{
			socket.bind(null);

			socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
					GroupOwnerSocketHandler.SERVER_PORT), CONN_TIMEOUT);

			Log.d(TAG, "Connected to server");

			socket.setSoTimeout(CONN_TIMEOUT);
		}
		catch (IOException e)
		{
			Log.e(TAG, "Cannot connect to server.", e);
			disconnect();
		}
	}

	public void disconnect()
	{
		if (socket == null)
		{
			return;
		}
		try
		{
			socket.close();
			socket = null;
		}
		catch (IOException e)
		{
			Log.e(TAG, "Could not close socket upon disconnect.", e);
			socket = null;
		}
	}
}
