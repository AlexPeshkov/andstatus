/*
 * Copyright (C) 2017 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.activity;

import android.support.annotation.NonNull;

import org.andstatus.app.msg.MessageEditorContainer;
import org.andstatus.app.user.UserListContextMenu;
import org.andstatus.app.user.UserViewItem;
import org.andstatus.app.view.MyContextMenu;

public class ActorContextMenu extends UserListContextMenu {

    public ActorContextMenu(MessageEditorContainer menuContainer) {
        super(menuContainer, MyContextMenu.MENU_GROUP_ACTOR);
    }

    @NonNull
    @Override
    protected UserViewItem getViewItem(ActivityViewItem activityViewItem) {
        return activityViewItem.actor;
    }
}
