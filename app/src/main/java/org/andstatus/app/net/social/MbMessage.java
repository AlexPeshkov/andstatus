/*
 * Copyright (C) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.net.social;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.database.table.MsgTable;
import org.andstatus.app.origin.OriginType;
import org.andstatus.app.util.MyHtml;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.StringUtils;
import org.andstatus.app.util.TriState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.andstatus.app.util.UriUtils.TEMP_OID_PREFIX;
import static org.andstatus.app.util.UriUtils.isEmptyOid;
import static org.andstatus.app.util.UriUtils.isRealOid;
import static org.andstatus.app.util.UriUtils.nonRealOid;

/**
 * Message of a Social Network
 * @author yvolk@yurivolkov.com
 */
public class MbMessage extends AObject {
    public static final MbMessage EMPTY = new MbMessage(0, getTempOid());

    private boolean isEmpty = false;
    private DownloadStatus status = DownloadStatus.UNKNOWN;
    
    public final String oid;
    private long updatedDate = 0;
    private Audience recipients = new Audience();
    private String body = "";

    private MbActivity inReplyTo = MbActivity.EMPTY;
    public final List<MbActivity> replies = new ArrayList<>();
    public String conversationOid="";
    public String via = "";
    public String url="";

    public final List<MbAttachment> attachments = new ArrayList<>();

    /** Some additional attributes may appear from "My account's" (authenticated User's) point of view */
    private TriState isPrivate = TriState.UNKNOWN;

    // In our system
    public final long originId;
    public long msgId = 0L;
    private long conversationId = 0L;

    @NonNull
    public static MbMessage fromOriginAndOid(long originId, String oid, DownloadStatus status) {
        MbMessage message = new MbMessage(originId, isEmptyOid(oid) ? getTempOid() : oid);
        message.status = status;
        if (TextUtils.isEmpty(oid) && status == DownloadStatus.LOADED) {
            message.status = DownloadStatus.UNKNOWN;
        }
        return message;
    }

    private static String getTempOid() {
        return TEMP_OID_PREFIX + "msg:" + MyLog.uniqueCurrentTimeMS() ;
    }

    private MbMessage(long originId, String oid) {
        this.originId = originId;
        this.oid = oid;
    }

    @NonNull
    public MbActivity update(MbUser accountUser) {
        return act(accountUser, MbUser.EMPTY, MbActivityType.UPDATE);
    }

    @NonNull
    public MbActivity act(MbUser accountUser, @NonNull MbUser actor, @NonNull MbActivityType activityType) {
        MbActivity mbActivity = MbActivity.from(accountUser, activityType);
        mbActivity.setActor(actor);
        mbActivity.setMessage(this);
        return mbActivity;
    }

    public String getBody() {
        return body;
    }
    public String getBodyToSearch() {
        return MyHtml.getBodyToSearch(body);
    }

    private boolean isHtmlContentAllowed() {
        return MyContextHolder.get().persistentOrigins().isHtmlContentAllowed(originId);
    }

    public static boolean mayBeEdited(OriginType originType, DownloadStatus downloadStatus) {
        if (originType == null || downloadStatus == null) return false;
        return downloadStatus == DownloadStatus.DRAFT || downloadStatus.mayBeSent() ||
                (downloadStatus == DownloadStatus.LOADED && originType.allowEditing());
    }

    public void setBody(String body) {
        if (TextUtils.isEmpty(body)) {
            this.body = "";
        } else if (isHtmlContentAllowed()) {
            this.body = MyHtml.stripUnnecessaryNewlines(MyHtml.unescapeHtml(body));
        } else {
            this.body = MyHtml.fromHtml(body);
        }
    }

    public MbMessage setConversationOid(String conversationOid) {
        if (TextUtils.isEmpty(conversationOid)) {
            this.conversationOid = "";
        } else {
            this.conversationOid = conversationOid;
        }
        return this;
    }

    public long lookupConversationId() {
        if (conversationId == 0  && !TextUtils.isEmpty(conversationOid)) {
            conversationId = MyQuery.conversationOidToId(originId, conversationOid);
        }
        if (conversationId == 0 && msgId != 0) {
            conversationId = MyQuery.msgIdToLongColumnValue(MsgTable.CONVERSATION_ID, msgId);
        }
        if (conversationId == 0 && getInReplyTo().nonEmpty()) {
            if (getInReplyTo().getMessage().msgId != 0) {
                conversationId = MyQuery.msgIdToLongColumnValue(MsgTable.CONVERSATION_ID,
                        getInReplyTo().getMessage().msgId);
            }
        }
        return setConversationIdFromMsgId();
    }

    public long setConversationIdFromMsgId() {
        if (conversationId == 0 && msgId != 0) {
            conversationId = msgId;
        }
        return conversationId;
    }

    public long getConversationId() {
        return conversationId;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public boolean isEmpty() {
        return this.isEmpty
                || originId == 0
                || (nonRealOid(oid)
                    && ((status != DownloadStatus.SENDING && status != DownloadStatus.DRAFT)
                        || (TextUtils.isEmpty(body) && attachments.isEmpty())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MbMessage other = (MbMessage) o;
        return hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        if (this == EMPTY) {
            return MyLog.formatKeyValue(this, "EMPTY");
        }
        StringBuilder builder = new StringBuilder();
        if (isEmpty()) {
            builder.append("empty,");
        }
        if(msgId != 0) {
            builder.append("id:" + msgId + ",");
        }
        if(conversationId != msgId) {
            builder.append("conversation_id:" + conversationId + ",");
        }
        builder.append("status:" + status + ",");
        if(StringUtils.nonEmpty(body)) {
            builder.append("body:'" + body + "',");
        }
        if(isEmpty) {
            builder.append("isEmpty,");
        }
        if(isPrivate()) {
            builder.append("private,");
        } else if(nonPrivate()) {
            builder.append("nonprivate,");
        }
        if(isRealOid(oid)) {
            builder.append("oid:'" + oid + "',");
        }
        if(isRealOid(conversationOid)) {
            builder.append("conversation_oid:'" + conversationOid + "',");
        }
        if(!TextUtils.isEmpty(url)) {
            builder.append("url:'" + url + "',");
        }
        if(!TextUtils.isEmpty(via)) {
            builder.append("via:'" + via + "',");
        }
        builder.append("updated:" + MyLog.debugFormatOfDate(updatedDate) + ",");
        builder.append("originId:" + originId + ",");
        if(recipients.nonEmpty()) {
            builder.append("\nrecipients:" + recipients + ",");
        }
        if (!attachments.isEmpty()) {
            builder.append("\nattachments:" + attachments + ",");
        }
        if(getInReplyTo().nonEmpty()) {
            builder.append("\ninReplyTo:" + getInReplyTo() + ",");
        }
        if(replies.size() > 0) {
            builder.append("\nReplies:" + replies + ",");
        }
        return MyLog.formatKeyValue(this, builder.toString());
    }

    @NonNull
    public MbActivity getInReplyTo() {
        return Optional.ofNullable(inReplyTo).orElse(MbActivity.EMPTY);
    }

    public void setInReplyTo(MbActivity activity) {
        if (activity != null && activity.nonEmpty()) {
            inReplyTo = activity;
        }
    }

    public TriState getPrivate() {
        return isPrivate;
    }

    public boolean isPrivate() {
        return isPrivate == TriState.TRUE;
    }

    public boolean nonPrivate() {
        return !isPrivate();
    }

    public MbMessage setPrivate(TriState isPrivate) {
        this.isPrivate = isPrivate;
        return this;
    }

    public long getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }

    @NonNull
    public Audience audience() {
        return recipients;
    }

    public void addRecipients(@NonNull Audience audience) {
        recipients.addAll(audience);
    }

    public void addRecipient(MbUser recipient) {
        if (recipient != null && recipient.nonEmpty()) {
            recipients.add(recipient);
        }
    }

    public void addRecipientsFromBodyText(MbUser author) {
        for (MbUser user : author.extractUsersFromBodyText(getBody(), true)) {
            addRecipient(user);
        }
    }

    public MbMessage shallowCopy() {
        MbMessage message = fromOriginAndOid(originId, oid, status);
        message.msgId = msgId;
        message.setUpdatedDate(updatedDate);
        return message;
    }

    public MbMessage copy(String oidNew) {
        MbMessage message = fromOriginAndOid(originId, oidNew, status);
        message.msgId = msgId;
        message.setUpdatedDate(updatedDate);

        message.recipients.addAll(recipients);
        message.setBody(body);
        message.inReplyTo = getInReplyTo();
        message.replies.addAll(replies);
        message.setConversationOid(conversationOid);
        message.via = via;
        message.url = url;

        message.attachments.addAll(attachments);
        message.isPrivate = getPrivate();

        message.conversationId = conversationId;
        return message;
    }

    public void addFavoriteBy(@NonNull MbUser accountUser, @NonNull TriState favoritedByMe) {
        if (favoritedByMe != TriState.TRUE) {
            return;
        }
        MbActivity favorite = MbActivity.from(accountUser, MbActivityType.LIKE);
        favorite.setActor(accountUser);
        favorite.setUpdatedDate(getUpdatedDate());
        favorite.setMessage(shallowCopy());
        replies.add(favorite);
    }

    @NonNull
    public TriState getFavoritedBy(MbUser accountUser) {
        if (msgId == 0) {
            for (MbActivity reply : replies) {
                if (reply.type == MbActivityType.LIKE && reply.getActor().equals(accountUser)
                        && reply.getMessage().oid.equals(oid) ) {
                    return TriState.TRUE;
                }
            }
            return TriState.UNKNOWN;
        } else {
            final Pair<Long, MbActivityType> favAndType = MyQuery.msgIdToLastFavoriting(MyContextHolder.get().getDatabase(),
                    msgId, accountUser.userId);
            switch (favAndType.second) {
                case LIKE:
                    return TriState.TRUE;
                case UNDO_LIKE:
                    return TriState.FALSE;
                default:
                    return TriState.UNKNOWN;
            }
        }
    }
}
