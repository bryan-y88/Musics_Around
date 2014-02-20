/*
 * Copyright (C) 2011 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.musicsaround.speaker;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicsaround.R;
import com.example.musicsaround.Timer;
import com.example.musicsaround.dj.GroupOwnerSocketHandler;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class ClientDeviceListFragment extends ListFragment implements
		PeerListListener, ConnectionInfoListener, Handler.Callback
{
	public static final String TAG = "Client Device List";

	View mContentView = null;

	private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
	ProgressDialog progressDialog = null;
	private WifiP2pDevice device;

	private Handler handler = new Handler(this);

	private ClientSocketHandler clientThread;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		this.setListAdapter(new WiFiPeerListAdapter(getActivity(),
				R.layout.row_devices, peers));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		mContentView = inflater.inflate(R.layout.device_list, null);

		return mContentView;
	}

	/*
	 * When this fragment is no longer visible, then disconnect the client
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.app.ListFragment#onDestroyView()
	 */
	@Override
	public void onDestroyView()
	{
		stopClient();
		super.onDestroyView();
	}

	/**
	 * @return this device
	 */
	public WifiP2pDevice getDevice()
	{
		return device;
	}

	private static String getDeviceStatus(int deviceStatus)
	{
		Log.d(TAG, "Peer status :" + deviceStatus);
		switch (deviceStatus)
		{
			case WifiP2pDevice.AVAILABLE:
				return "Available";
			case WifiP2pDevice.INVITED:
				return "Invited";
			case WifiP2pDevice.CONNECTED:
				return "Connected";
			case WifiP2pDevice.FAILED:
				return "Failed";
			case WifiP2pDevice.UNAVAILABLE:
				return "Unavailable";
			default:
				return "Unknown";
		}
	}

	/**
	 * Perform an acion with a peer, depending on what their state is
	 */
	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(
				position);

		switch (device.status)
		{
			case WifiP2pDevice.AVAILABLE:
				// show a progress bar for connection
				// TODO: make these non-blocking and timing out
				if (progressDialog != null && progressDialog.isShowing())
				{
					progressDialog.dismiss();
				}

				progressDialog = ProgressDialog.show(getActivity(),
						"Press back to cancel", "Connecting to: "
								+ device.deviceName, true, true);

				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.wps.setup = WpsInfo.PBC;
				((SpeakerFragmentListener) getActivity()).connect(config);
				break;

			case WifiP2pDevice.INVITED:
				if (progressDialog != null && progressDialog.isShowing())
				{
					progressDialog.dismiss();
				}

				progressDialog = ProgressDialog.show(getActivity(),
						"Press back to abort", "Revoking invitation to: "
								+ device.deviceName, true, true);

				((SpeakerFragmentListener) getActivity()).cancelDisconnect();
				// start another discovery
				((SpeakerFragmentListener) getActivity()).discoverDevices();
				break;

			case WifiP2pDevice.CONNECTED:
				if (progressDialog != null && progressDialog.isShowing())
				{
					progressDialog.dismiss();
				}

				progressDialog = ProgressDialog.show(getActivity(),
						"Press back to abort", "Disconnecting: "
								+ device.deviceName, true, true);

				((SpeakerFragmentListener) getActivity()).disconnect();
				// start another discovery
				((SpeakerFragmentListener) getActivity()).discoverDevices();
				break;

			// refresh the list of devices
			case WifiP2pDevice.FAILED:
			case WifiP2pDevice.UNAVAILABLE:
			default:
				((SpeakerFragmentListener) getActivity()).discoverDevices();
				break;
		}

		((SpeakerFragmentListener) getActivity()).showDetails(device);
	}

	/**
	 * Array adapter for ListFragment that maintains WifiP2pDevice list.
	 */
	private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>
	{

		private List<WifiP2pDevice> items;

		/**
		 * @param context
		 * @param textViewResourceId
		 * @param objects
		 */
		public WiFiPeerListAdapter(Context context, int textViewResourceId,
				List<WifiP2pDevice> objects)
		{
			super(context, textViewResourceId, objects);
			items = objects;

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View v = convertView;
			if (v == null)
			{
				LayoutInflater vi = (LayoutInflater) getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row_devices, null);
			}
			WifiP2pDevice device = items.get(position);
			if (device != null)
			{
				TextView top = (TextView) v.findViewById(R.id.device_name);
				TextView bottom = (TextView) v
						.findViewById(R.id.device_details);
				if (top != null)
				{
					top.setText(device.deviceName);
				}
				if (bottom != null)
				{
					// show the invited dialog
					if (device.status == WifiP2pDevice.INVITED)
					{
						if (progressDialog != null
								&& progressDialog.isShowing())
						{
							progressDialog.dismiss();
						}

						progressDialog = ProgressDialog
								.show(getActivity(),
										"Inviting peer",
										"Sent invitation to: "
												+ device.deviceName
												+ "\n\nTap on peer to revoke invitation.",
										true, true);
					}

					bottom.setText(getDeviceStatus(device.status));
				}
			}

			return v;

		}
	}

	/**
	 * Update UI for this device.
	 * 
	 * @param device
	 *            WifiP2pDevice object
	 */
	public void updateThisDevice(WifiP2pDevice device)
	{
		this.device = device;
		TextView view = (TextView) mContentView.findViewById(R.id.my_name);
		view.setText(device.deviceName);
		view = (TextView) mContentView.findViewById(R.id.my_status);
		view.setText(getDeviceStatus(device.status));
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peerList)
	{
		if (progressDialog != null && progressDialog.isShowing())
		{
			progressDialog.dismiss();
		}
		peers.clear();
		peers.addAll(peerList.getDeviceList());
		((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
		if (peers.size() == 0)
		{
			Log.d(TAG, "No devices found");
			return;
		}
	}

	public void clearPeers()
	{
		peers.clear();
		((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
	}

	@Override
	public boolean handleMessage(Message msg)
	{

		switch (msg.what)
		{
			case ClientSocketHandler.EVENT_RECEIVE_MSG:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf);

				// interpret the command
				String[] cmdString = readMessage
						.split(GroupOwnerSocketHandler.CMD_DELIMITER);

				if (cmdString[0].equals(GroupOwnerSocketHandler.PLAY_CMD)
						&& cmdString.length > 3)
				{
					try
					{
						((SpeakerFragmentListener) getActivity()).playMusic(
								cmdString[1], Long.parseLong(cmdString[2]),
								Integer.parseInt(cmdString[3]));
					}
					catch (NumberFormatException e)
					{
						Log.e(TAG,
								"Could not convert to a proper time for these two strings: "
										+ cmdString[2] + " and " + cmdString[3],
								e);
					}
				}
				else if (cmdString[0].equals(GroupOwnerSocketHandler.STOP_CMD)
						&& cmdString.length > 0)
				{
					((SpeakerFragmentListener) getActivity()).stopMusic();
				}

				Log.d(TAG, readMessage);

				// Toast.makeText(mContentView.getContext(),
				// "Received message: " + readMessage, Toast.LENGTH_SHORT)
				// .show();
				break;

			case ClientSocketHandler.CLIENT_CALLBACK:
				clientThread = (ClientSocketHandler) msg.obj;
				Log.d(TAG, "Retrieved client thread.");
				break;

			default:
				Log.d(TAG, "I thought we heard something? Message type: "
						+ msg.what);
				break;
		}
		return true;
	}

	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info)
	{
		if (progressDialog != null && progressDialog.isShowing())
		{
			progressDialog.dismiss();
		}

		// display this info if necessary
		((SpeakerFragmentListener) getActivity()).showInfo(info);

		// The group owner IP is now known.
		if (info.groupFormed && info.isGroupOwner)
		{
			// In Speaker mode, we must not be the group owner, or else we have
			// a problem
			Log.d(TAG, "Speaker Mode became the group owner! >:(.");

			Toast.makeText(mContentView.getContext(),
					"Speaker Mode became the group owner! >:(.",
					Toast.LENGTH_SHORT).show();
		}
		else if (info.groupFormed)
		{
			// WARNING:
			// depends on the timing, if we don't get a server back in time,
			// we may end up running multiple threads of the client
			// instance!
			if (this.clientThread == null)
			{
				Thread client = new ClientSocketHandler(this.handler,
						info.groupOwnerAddress,
						((SpeakerFragmentListener) getActivity())
								.retrieveTimer());
				client.start();
			}

			Toast.makeText(mContentView.getContext(),
					"Speaker client started.", Toast.LENGTH_SHORT).show();
		}
	}

	public void stopClient()
	{
		if (clientThread != null)
		{
			clientThread.disconnect();
			clientThread = null;
		}
	}

	/**
	 * An interface-callback for the activity to listen to fragment interaction
	 * events.
	 */
	public interface SpeakerFragmentListener
	{
		void showDetails(WifiP2pDevice device);

		void showInfo(WifiP2pInfo info);

		void cancelDisconnect();

		void connect(WifiP2pConfig config);

		void disconnect();

		void discoverDevices();

		void playMusic(String url, long startTime, int startPos);

		void stopMusic();

		Timer retrieveTimer();
	}
}
