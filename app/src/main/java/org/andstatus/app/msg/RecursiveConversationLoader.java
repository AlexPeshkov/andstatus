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

import android.database.Cursor;
import android.net.Uri;

import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContext;
import org.andstatus.app.data.DbUtils;
import org.andstatus.app.data.MatchedUri;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.ProjectionMap;
import org.andstatus.app.database.table.ActivityTable;
import org.andstatus.app.database.table.MsgTable;
import org.andstatus.app.timeline.meta.Timeline;
import org.andstatus.app.timeline.meta.TimelineType;
import org.andstatus.app.util.MyLog;

/**
 * @author yvolk@yurivolkov.com
 */
public class RecursiveConversationLoader<T extends ConversationItem<T>> extends ConversationLoader<T> {
    public RecursiveConversationLoader(T emptyItem, MyContext myContext, MyAccount ma,
                                       long selectedMessageId, boolean sync) {
        super(emptyItem, myContext, ma, selectedMessageId, sync);
    }

    @Override
    protected void load2(T oMsg) {
        cacheConversation(oMsg);
        findPreviousMessagesRecursively(getOMsg(oMsg.getMsgId(), 0));
    }

    private void cacheConversation(T oMsg) {
        long conversationId = MyQuery.msgIdToLongColumnValue(MsgTable.CONVERSATION_ID, oMsg.getMsgId());
        String selection = (conversationId == 0
                ? ProjectionMap.ACTIVITY_TABLE_ALIAS + "." + ActivityTable.MSG_ID + "=" + oMsg.getMsgId()
                : ProjectionMap.MSG_TABLE_ALIAS + "." + MsgTable.CONVERSATION_ID + "=" + conversationId);
        Uri uri = Timeline.getTimeline(TimelineType.EVERYTHING, ma, 0, null).getUri();

        try (Cursor cursor = myContext.context().getContentResolver().query(uri, oMsg.getProjection(),
                selection, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    T oMsg2 = newOMsg(DbUtils.getLong(cursor, ActivityTable.MSG_ID));
                    oMsg2.load(cursor);
                    cachedMessages.put(oMsg2.getMsgId(), oMsg2);
                }
            }
        }
    }

    private void findPreviousMessagesRecursively(T oMsg) {
        if (!addMessageIdToFind(oMsg.getMsgId())) {
            return;
        }
        findRepliesRecursively(oMsg);
        MyLog.v(this, "findPreviousMessages id=" + oMsg.getMsgId() + " replies:" + oMsg.mNReplies);
        loadMessageFromDatabase(oMsg);
        if (oMsg.isLoaded()) {
            if (addMessageToList(oMsg)) {
                if (oMsg.inReplyToMsgId != 0) {
                    findPreviousMessagesRecursively(getOMsg(oMsg.inReplyToMsgId,
                            oMsg.replyLevel - 1));
                }
            }
        } else if (mAllowLoadingFromInternet) {
            loadFromInternet(oMsg.getMsgId());
        }
    }

    public void findRepliesRecursively(T oMsg) {
        MyLog.v(this, "findReplies for id=" + oMsg.getMsgId());
        for (T oMsgReply : cachedMessages.values()) {
            if (oMsgReply.inReplyToMsgId == oMsg.getMsgId()) {
                oMsg.mNReplies++;
                oMsgReply.replyLevel = oMsg.replyLevel + 1;
                findPreviousMessagesRecursively(oMsgReply);
            }
        }
    }
}
