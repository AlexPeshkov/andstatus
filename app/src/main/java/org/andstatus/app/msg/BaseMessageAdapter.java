/*
 * Copyright (c) 2016 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.msg;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.andstatus.app.R;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyPreferences;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.graphics.AvatarView;
import org.andstatus.app.timeline.BaseTimelineAdapter;
import org.andstatus.app.timeline.TimelineData;
import org.andstatus.app.util.I18n;
import org.andstatus.app.util.MyUrlSpan;
import org.andstatus.app.util.SharedPreferencesUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yvolk@yurivolkov.com
 */
public abstract class BaseMessageAdapter<T extends BaseMessageViewItem<T>> extends BaseTimelineAdapter<T> {
    protected final boolean showButtonsBelowMessages =
            SharedPreferencesUtil.getBoolean(MyPreferences.KEY_SHOW_BUTTONS_BELOW_MESSAGE, true);
    protected final MessageContextMenu contextMenu;
    protected Set<Long> preloadedImages = new HashSet<>(100);

    public BaseMessageAdapter(@NonNull MessageContextMenu contextMenu, TimelineData<T> listData) {
        super(contextMenu.getMyContext(), listData);
        this.contextMenu = contextMenu;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup view = getEmptyView(convertView);
        view.setOnCreateContextMenuListener(contextMenu);
        view.setOnClickListener(this);
        setPosition(view, position);
        T item = getItem(position);
        populateView(view, item, position);
        return view;
    }

    public void populateView(ViewGroup view, T item, int position) {
        showRebloggers(view, item);
        MyUrlSpan.showText(view, R.id.message_author, item.authorName, false, false);
        showMessageBody(view, item);
        MyUrlSpan.showText(view, R.id.message_details, item.getDetails(contextMenu.getActivity()).toString(), false, false);

        showAvatarEtc(view, item);

        if (showAttachedImages) {
            showAttachedImage(view, item);
        }
        if (markReplies) {
            showMarkReplies(view, item);
        }
        if (showButtonsBelowMessages) {
            showButtonsBelowMessage(view, item);
        } else {
            showFavorited(view, item);
        }
        showMessageNumberEtc(view, item, position);
    }

    protected abstract void showAvatarEtc(ViewGroup view, T item);

    protected abstract void showMessageNumberEtc(ViewGroup view, T item, int position);

    protected ViewGroup getEmptyView(View convertView) {
        if (convertView == null) return newView();
        convertView.setBackgroundResource(0);
        View messageIndented = convertView.findViewById(R.id.message_indented);
        messageIndented.setBackgroundResource(0);
        return (ViewGroup) convertView;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getMsgId();
    }

    protected ViewGroup newView() {
        ViewGroup view = (ViewGroup) LayoutInflater.from(contextMenu.getActivity()).inflate(R.layout.message, null);
        setupButtons(view);
        return view;
    }

    protected void showRebloggers(View view, T item) {
        View viewGroup = view.findViewById(R.id.reblogged);
        if (viewGroup == null) {
            return;
        } else if (item.isReblogged()) {
            viewGroup.setVisibility(View.VISIBLE);
            StringBuilder rebloggers = new StringBuilder();
            for (String name : item.rebloggers.values()) {
                I18n.appendWithComma(rebloggers, name);
            }
            MyUrlSpan.showText(viewGroup, R.id.rebloggers, rebloggers.toString(), false, false);
        } else {
            viewGroup.setVisibility(View.GONE);
        }
    }

    protected void showMessageBody(View view, T item) {
        TextView body = view.findViewById(R.id.message_body);
        MyUrlSpan.showText(body, item.getBody(), true, true);
    }

    protected void showAvatar(View view, T item) {
        AvatarView avatarView = view.findViewById(R.id.avatar_image);
        item.avatarFile.showImage(contextMenu.getActivity(), avatarView);
    }

    protected void showAttachedImage(View view, T item) {
        preloadedImages.add(item.getMsgId());
        item.getAttachedImageFile().showImage(contextMenu.getActivity(), view.findViewById(R.id.attached_image));
    }

    protected void showMarkReplies(ViewGroup view, T item) {
        boolean show = item.inReplyToUserId != 0 && myContext.persistentAccounts().
                fromUserId(item.inReplyToUserId).isValid();
        View oldView = view.findViewById(R.id.reply_timeline_marker);
        if (oldView != null) {
            view.removeView(oldView);
        }
        if (show) {
            View referencedView = view.findViewById(R.id.message_indented);
            ImageView indentView = new ConversationIndentImageView(myContext.context(), referencedView, dpToPixes(6),
                    R.drawable.reply_timeline_marker_light, R.drawable.reply_timeline_marker);
            indentView.setId(R.id.reply_timeline_marker);
            view.addView(indentView, 1);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)  indentView.getLayoutParams();
            layoutParams.leftMargin = dpToPixes(3);
        }
    }

    public void setupButtons(View view) {
        if (showButtonsBelowMessages) {
            View buttons = view.findViewById(R.id.message_buttons);
            if (buttons != null) {
                buttons.setVisibility(View.VISIBLE);
                setOnButtonClick(buttons, R.id.reply_button, MessageListContextMenuItem.REPLY);
                setOnButtonClick(buttons, R.id.reblog_button, MessageListContextMenuItem.REBLOG);
                setOnButtonClick(buttons, R.id.reblog_button_tinted, MessageListContextMenuItem.DESTROY_REBLOG);
                setOnButtonClick(buttons, R.id.favorite_button, MessageListContextMenuItem.FAVORITE);
                setOnButtonClick(buttons, R.id.favorite_button_tinted, MessageListContextMenuItem.DESTROY_FAVORITE);
                setOnButtonClick(buttons, R.id.more_button, MessageListContextMenuItem.UNKNOWN);
            }
        }
    }

    private void setOnButtonClick(final View viewGroup, int buttonId, final MessageListContextMenuItem menuItem) {
        viewGroup.findViewById(buttonId).setOnClickListener(
                v -> {
                    if (menuItem.equals(MessageListContextMenuItem.UNKNOWN)) {
                        viewGroup.showContextMenu();
                    } else {
                        onButtonClick(v, menuItem);
                    }
                }
        );
    }

    private void onButtonClick(View v, MessageListContextMenuItem contextMenuItemIn) {
        T item = getItem(v);
        if (item != null && item.msgStatus == DownloadStatus.LOADED) {
            contextMenu.onCreateContextMenu(null, v, null, (contextMenu) -> {
                contextMenu.onContextItemSelected(contextMenuItemIn, item.getMsgId());
            });
        }
    }

    protected void showButtonsBelowMessage(View view, T item) {
        View viewGroup = view.findViewById(R.id.message_buttons);
        if (viewGroup == null) {
            return;
        } else if (showButtonsBelowMessages && item.msgStatus == DownloadStatus.LOADED) {
            viewGroup.setVisibility(View.VISIBLE);
            tintIcon(viewGroup, item.reblogged, R.id.reblog_button, R.id.reblog_button_tinted);
            tintIcon(viewGroup, item.favorited, R.id.favorite_button, R.id.favorite_button_tinted);
        } else {
            viewGroup.setVisibility(View.GONE);
        }
    }

    private void tintIcon(View viewGroup, boolean colored, int viewId, int viewIdColored) {
        ImageView imageView = viewGroup.findViewById(viewId);
        ImageView imageViewTinted = viewGroup.findViewById(viewIdColored);
        imageView.setVisibility(colored ? View.GONE : View.VISIBLE);
        imageViewTinted.setVisibility(colored ? View.VISIBLE : View.GONE);
    }

    protected void showFavorited(View view, T item) {
        View favorited = view.findViewById(R.id.message_favorited);
        favorited.setVisibility(item.favorited ? View.VISIBLE : View.GONE );
    }
}
