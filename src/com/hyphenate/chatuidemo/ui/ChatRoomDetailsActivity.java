/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.chatuidemo.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMConversation.EMConversationType;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseAlertDialog;
import com.hyphenate.easeui.widget.EaseAlertDialog.AlertDialogUser;
import com.hyphenate.easeui.widget.EaseExpandGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatRoomDetailsActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "ChatRoomDetailsActivity";
	private static final int REQUEST_CODE_EXIT = 1;
	private static final int REQUEST_CODE_EXIT_DELETE = 2;
	private static final int REQUEST_CODE_CLEAR_ALL_HISTORY = 3;

	String operationUserId;

	private String roomId;
	private ProgressBar loadingPB;
	private EMChatRoom room;
	private OwnerAdminAdapter ownerAdminAdapter;
	private MemberAdapter membersAdapter;
	private ProgressDialog progressDialog;

	public static ChatRoomDetailsActivity instance;


	String st = "";

	private List<String> adminList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> memberList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> muteList = Collections.synchronizedList(new ArrayList<String>());
	private List<String> blackList = Collections.synchronizedList(new ArrayList<String>());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.em_activity_chatroom_details);
		instance = this;
		st = getResources().getString(R.string.people);
		loadingPB = (ProgressBar) findViewById(R.id.progressBar);
		RelativeLayout blacklistLayout = (RelativeLayout) findViewById(R.id.rl_blacklist);
		RelativeLayout changeChatRoomNameLayout = (RelativeLayout) findViewById(R.id.rl_change_chatroom_name);
		RelativeLayout changeChatRoomDescriptionLayout = (RelativeLayout) findViewById(R.id.rl_change_chatroom_detail);

		TextView chatRoomIdTextView = (TextView) findViewById(R.id.tv_chat_room_id_value);
		TextView chatRoomNickTextView = (TextView) findViewById(R.id.tv_chat_room_nick_value);

		// get room id
		roomId = getIntent().getStringExtra("roomId");
		room = EMClient.getInstance().chatroomManager().getChatRoom(roomId);
		if (room == null) {
			return;
		}
		chatRoomIdTextView.setText(roomId);
		chatRoomNickTextView.setText(room.getName());

		if (isCurrentOwner(room) || isCurrentAdmin(room)) {
			blacklistLayout.setVisibility(View.VISIBLE);
			changeChatRoomNameLayout.setVisibility(View.VISIBLE);
			changeChatRoomDescriptionLayout.setVisibility(View.VISIBLE);
		} else {
			blacklistLayout.setVisibility(View.GONE);
			changeChatRoomNameLayout.setVisibility(View.GONE);
			changeChatRoomDescriptionLayout.setVisibility(View.GONE);
		}

		// owner & admin list
		List<String> ownerAdmins = new ArrayList<>();
		ownerAdmins.add(room.getOwner());
		ownerAdmins.addAll(room.getAdministratorList());
		ownerAdminAdapter = new OwnerAdminAdapter(this, R.layout.em_grid_owner, ownerAdmins);
		EaseExpandGridView ownerAdminGridView = (EaseExpandGridView) findViewById(R.id.owner_and_administrators);
		ownerAdminGridView.setAdapter(ownerAdminAdapter);

		// normal member list & black list && mute list
		// most show 500 members, most show 500 mute members, most show 500 black list
		memberList = new java.util.ArrayList<>();
		memberList.addAll(room.getMemberList());
		membersAdapter = new MemberAdapter(this, R.layout.em_grid_owner, memberList);
		EaseExpandGridView userGridView = (EaseExpandGridView) findViewById(R.id.gridview);
		userGridView.setAdapter(membersAdapter);

		updateRoom();

		blacklistLayout.setOnClickListener(this);
		changeChatRoomNameLayout.setOnClickListener(this);

		final EMChatRoom finalRoom = room;
		new Thread(new Runnable() {
			@Override
			public void run() {
				String owner = finalRoom.getOwner();
				List<String> administratorList = finalRoom.getAdministratorList();
				membersAdapter.notifyDataSetChanged();
			}
		}).start();
	}

	boolean isCurrentOwner(EMChatRoom room) {
		String owner = room.getOwner();
		if (owner == null || owner.isEmpty()) {
			return false;
		}
		return owner.equals(EMClient.getInstance().getCurrentUser());
	}

	boolean isCurrentAdmin(EMChatRoom room) {
		synchronized (adminList) {
			String currentUser = EMClient.getInstance().getCurrentUser();
			for (String admin : adminList) {
				if (currentUser.equals(admin)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean isAdmin(String id) {
		synchronized (adminList) {
			for (String admin : adminList) {
				if (id.equals(admin)) {
					return true;
				}
			}
		}
		return false;
	}

	boolean isInBlackList(String id) {
		synchronized (blackList) {
			if (id != null && !id.isEmpty()) {
				for (String item : blackList) {
					if (id.equals(item)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	boolean isInMuteList(String id) {
		synchronized (muteList) {
			if (id != null && !id.isEmpty()) {
				for (String item : muteList) {
					if (id.equals(item)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("UnusedAssignment")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		String st1 = getResources().getString(R.string.being_added);
		String st2 = getResources().getString(R.string.is_quit_the_chat_room);
		String st3 = getResources().getString(R.string.chatting_is_dissolution);
		String st4 = getResources().getString(R.string.are_empty_group_of_news);
		String st5 = getResources().getString(R.string.is_modify_the_group_name);
		final String st6 = getResources().getString(R.string.Modify_the_group_name_successful);
		final String st7 = getResources().getString(R.string.change_the_group_name_failed_please);
		String st8 = getResources().getString(R.string.Are_moving_to_blacklist);
		final String st9 = getResources().getString(R.string.failed_to_move_into);

		final String stsuccess = getResources().getString(R.string.Move_into_blacklist_success);
		if (resultCode == RESULT_OK) {
			if (progressDialog == null) {
				progressDialog = new ProgressDialog(ChatRoomDetailsActivity.this);
				progressDialog.setMessage(st1);
				progressDialog.setCanceledOnTouchOutside(false);
			}
			switch (requestCode) {
				case REQUEST_CODE_EXIT: // quit the group
					progressDialog.setMessage(st2);
					progressDialog.show();
					exitGroup();
					break;

				default:
					break;
			}
		}
	}


	public void exitGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class), REQUEST_CODE_EXIT);

	}


	public void exitDeleteGroup(View view) {
		startActivityForResult(new Intent(this, ExitGroupDialog.class).putExtra("deleteToast", getString(R.string.dissolution_group_hint)),
				REQUEST_CODE_EXIT_DELETE);

	}

	/**
	 * clear conversation history in group
	 */
	public void clearGroupHistory() {
		EMConversation conversation = EMClient.getInstance().chatManager().getConversation(room.getId(), EMConversationType.ChatRoom);
		if (conversation != null) {
			conversation.clearAllMessages();
		}
		Toast.makeText(this, R.string.messages_are_empty, Toast.LENGTH_SHORT).show();
	}

	/**
	 * exit group
	 *
	 * @param
	 */
	private void exitGroup() {
		new Thread(new Runnable() {
			public void run() {
				try {
					EMClient.getInstance().chatroomManager().leaveChatRoom(roomId);
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							setResult(RESULT_OK);
							finish();
							if (ChatActivity.activityInstance != null)
								ChatActivity.activityInstance.finish();
						}
					});
				} catch (final Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							progressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "Failed to quit group: " + e.getMessage(), Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	protected void updateRoom() {
		new Thread(new Runnable() {
			public void run() {
				try {
					room = EMClient.getInstance().chatroomManager().fetchChatRoomFromServer(roomId, true);

					room = EMClient.getInstance().chatroomManager().fetchChatRoomFromServer(roomId);
					adminList.clear();
					adminList.addAll(room.getAdministratorList());
					memberList.clear();
					memberList.addAll(EMClient.getInstance().chatroomManager().fetchChatroomMembers(roomId, 0, 500));
					muteList.clear();
					muteList.addAll(EMClient.getInstance().chatroomManager().fetchChatRoomMuteList(roomId, 0, 500));
					blackList.clear();
					blackList.addAll(EMClient.getInstance().chatroomManager().fetchChatRoomBlockList(roomId, 0, 500));

					runOnUiThread(new Runnable() {
						public void run() {
							TextView chatRoomNickTextView = (TextView) findViewById(R.id.tv_chat_room_nick_value);
							chatRoomNickTextView.setText(room.getName());
							loadingPB.setVisibility(View.INVISIBLE);

							refreshOwnerAdminAdapter();
							refreshMembersAdapter();

							Button destroyButton = (Button)ChatRoomDetailsActivity.this.findViewById(R.id.btn_destroy_chatroom);
							destroyButton.setVisibility(EMClient.getInstance().getCurrentUser().equals(room.getOwner()) ?
									View.VISIBLE : View.GONE);
						}
					});

				} catch (Exception e) {
					runOnUiThread(new Runnable() {
						public void run() {
							loadingPB.setVisibility(View.INVISIBLE);
						}
					});
				}
			}
		}).start();
	}

	private void refreshOwnerAdminAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ownerAdminAdapter.clear();
				synchronized (memberList) {
					ownerAdminAdapter.addAll(memberList);
				}
				ownerAdminAdapter.notifyDataSetChanged();
			}
		});
	}

	private void refreshMembersAdapter() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				membersAdapter.clear();
				synchronized (memberList) {
					membersAdapter.addAll(memberList);
				}
				membersAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.clear_all_history: // clear conversation history
				String st9 = getResources().getString(R.string.sure_to_empty_this);
				new EaseAlertDialog(ChatRoomDetailsActivity.this, null, st9, null, new AlertDialogUser() {

					@Override
					public void onResult(boolean confirmed, Bundle bundle) {
						if (confirmed) {
							clearGroupHistory();
						}
					}
				}, true).show();
				break;
			case R.id.menu_item_add_admin:
				new Thread(new Runnable() {
					@Override
					public void run() {

						updateRoom();
					}
				}).start();
				break;
			case R.id.menu_item_rm_admin:
				break;
			case R.id.menu_item_add_to_blacklist:
				break;
			case R.id.menu_item_remove_from_blacklist:
				break;
			case R.id.menu_item_mute:
				break;
			case R.id.menu_item_unmute:
				break;
			case R.id.menu_item_transfer_owner:
				break;
			default:
				break;
		}

	}

	private class OwnerAdminAdapter extends ArrayAdapter<String> {
		private int res;
		private List<String> ownerAndAdministratorList;

		/**
		 * Owner and Administrator list
		 *
		 * @param context
		 * @param textViewResourceId
		 * @param ownerAndAdministratorList the first element should be owner
		 */
		public OwnerAdminAdapter(Context context, int textViewResourceId, List<String> ownerAndAdministratorList) {
			super(context, textViewResourceId, ownerAndAdministratorList);
			res = textViewResourceId;
			this.ownerAndAdministratorList = ownerAndAdministratorList;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			@SuppressWarnings("UnusedAssignment") ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(res, null);
				holder.imageView = (ImageView) convertView.findViewById(R.id.iv_avatar);
				holder.textView = (TextView) convertView.findViewById(R.id.tv_name);
				holder.badgeDeleteView = (ImageView) convertView.findViewById(R.id.badge_delete);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);
			// group member item
			final String username = getItem(position);
			holder.textView.setText(username);
			EaseUserUtils.setUserNick(username, holder.textView);
			EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);
			LinearLayout id_background = (LinearLayout) convertView.findViewById(R.id.l_bg_id);
			id_background.setBackgroundColor(convertView.getResources().getColor(
					position == 0 ? R.color.holo_red_light: R.color.holo_orange_light));
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCurrentOwner(room)) {
						return;
					}
					// do nothing here, you can show group member's profile here
					operationUserId = username;
					Dialog dialog = new Dialog(ChatRoomDetailsActivity.this);
					dialog.setTitle("chat room");
					dialog.setContentView(R.layout.em_chatroom_member_menu);
					dialog.show();

					LinearLayout itemAddAdmin = (LinearLayout)dialog.findViewById(R.id.menu_item_add_admin);
					LinearLayout itemRemoveAdmin = (LinearLayout)dialog.findViewById(R.id.menu_item_rm_admin);
					LinearLayout itemTransferOwner = (LinearLayout)dialog.findViewById(R.id.menu_item_transfer_owner);
					LinearLayout itemAddToBlackList = (LinearLayout) dialog.findViewById(R.id.menu_item_add_to_blacklist);
					LinearLayout itemRemoveFromBlackList = (LinearLayout) dialog.findViewById(R.id.menu_item_remove_from_blacklist);
					LinearLayout itemMute = (LinearLayout) dialog.findViewById(R.id.menu_item_mute);
					LinearLayout itemUnMute = (LinearLayout) dialog.findViewById(R.id.menu_item_unmute);

					if (isAdmin(username)) {
						itemRemoveAdmin.setVisibility(View.VISIBLE);
						itemTransferOwner.setVisibility(View.VISIBLE);
					}
				}
			});
			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount();
		}
	}


	/**
	 * group member grid adapter
	 *
	 * @author admin_new
	 */
	private class MemberAdapter extends ArrayAdapter<String> {

		private int res;

		public MemberAdapter(Context context, int textViewResourceId, List<String> objects) {
			super(context, textViewResourceId, objects);
			res = textViewResourceId;
		}

		@Override
		public View getView(final int position, View convertView, final ViewGroup parent) {
			@SuppressWarnings("UnusedAssignment") ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = LayoutInflater.from(getContext()).inflate(res, null);
				holder.imageView = (ImageView) convertView.findViewById(R.id.iv_avatar);
				holder.textView = (TextView) convertView.findViewById(R.id.tv_name);
				holder.badgeDeleteView = (ImageView) convertView.findViewById(R.id.badge_delete);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			final LinearLayout button = (LinearLayout) convertView.findViewById(R.id.button_avatar);
			// group member item
			final String username = getItem(position);
			holder.textView.setText(username);
			EaseUserUtils.setUserNick(username, holder.textView);
			EaseUserUtils.setUserAvatar(getContext(), username, holder.imageView);

			LinearLayout id_background = (LinearLayout) convertView.findViewById(R.id.l_bg_id);
			if (isInMuteList(username)) {
				id_background.setBackgroundColor(convertView.getResources().getColor(R.color.holo_green_light));
			} else if (isInBlackList(username)) {
				id_background.setBackgroundColor(convertView.getResources().getColor(R.color.holo_black));
			} else {
				id_background.setBackgroundColor(convertView.getResources().getColor(R.color.holo_blue_bright));
			}

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isCurrentOwner(room) && !isCurrentAdmin(room)) {
						return;
					}
					// do nothing here, you can show group member's profile here
					operationUserId = username;
					Dialog dialog = new Dialog(ChatRoomDetailsActivity.this);
					dialog.setTitle("chat room");
					dialog.setContentView(R.layout.em_chatroom_member_menu);
					dialog.show();

					LinearLayout itemAddAdmin = (LinearLayout)dialog.findViewById(R.id.menu_item_add_admin);
					LinearLayout itemRemoveAdmin = (LinearLayout)dialog.findViewById(R.id.menu_item_rm_admin);
					LinearLayout itemTransferOwner = (LinearLayout)dialog.findViewById(R.id.menu_item_transfer_owner);
					LinearLayout itemAddToBlackList = (LinearLayout) dialog.findViewById(R.id.menu_item_add_to_blacklist);
					LinearLayout itemRemoveFromBlackList = (LinearLayout) dialog.findViewById(R.id.menu_item_remove_from_blacklist);
					LinearLayout itemMute = (LinearLayout) dialog.findViewById(R.id.menu_item_mute);
					LinearLayout itemUnMute = (LinearLayout) dialog.findViewById(R.id.menu_item_unmute);

					if (isCurrentOwner(room)) {
						itemAddAdmin.setVisibility(View.VISIBLE);
						if (!isInBlackList(username)) {
							itemAddToBlackList.setVisibility(View.VISIBLE);
						} else {
							itemRemoveFromBlackList.setVisibility(View.VISIBLE);
						}
						if (!isInMuteList(username)) {
							itemMute.setVisibility(View.VISIBLE);
						} else {
							itemUnMute.setVisibility(View.VISIBLE);
						}
					} else if (isCurrentAdmin(room)) {
						if (!isInBlackList(username)) {
							itemAddToBlackList.setVisibility(View.VISIBLE);
						} else {
							itemRemoveFromBlackList.setVisibility(View.VISIBLE);
						}
						if (!isInMuteList(username)) {
							itemMute.setVisibility(View.VISIBLE);
						} else {
							itemUnMute.setVisibility(View.VISIBLE);
						}
					}
				}
			});

			return convertView;
		}

		@Override
		public int getCount() {
			return super.getCount();
		}
	}


	public void back(View view) {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}

	private static class ViewHolder {
		ImageView imageView;
		TextView textView;
		ImageView badgeDeleteView;
	}

	public void onDestroyChatRoomClick(View v) {
		if (EMClient.getInstance().getCurrentUser().equals(room.getOwner())) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						EMClient.getInstance().chatroomManager().destroyChatroom(room.getId());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								if (ChatRoomDetailsActivity.this.isFinishing() || ChatRoomDetailsActivity.this.isDestroyed()) {
									return;
								}
								finish();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		} else {
			Toast.makeText(this, "Current user is not owner, can't destroy the chat room", Toast.LENGTH_LONG);
		}

	}
}