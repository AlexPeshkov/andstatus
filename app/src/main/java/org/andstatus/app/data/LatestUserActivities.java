/**
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

package org.andstatus.app.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects {@link UserActivity} data (e.g. during timeline download) and allows to save it in bulk
 * @author yvolk@yurivolkov.com
 */
public class LatestUserActivities {
    private final Map<Long, UserActivity> userActivities = new HashMap<>();

    /**
     * Add information about new/updated message by the User
     */
    public void onNewUserActivity(UserActivity uaIn) {
        // On different implementations see 
        // http://stackoverflow.com/questions/81346/most-efficient-way-to-increment-a-map-value-in-java
        UserActivity um = userActivities.get(uaIn.getUserId());
        if (um == null) {
            um = uaIn;
        } else {
            um.onNewActivity(uaIn.getLastActivityId(), uaIn.getLastActivityDate() );
        }
        userActivities.put(um.getUserId(), um);
    }
    
    /**
     * Persist all information into the database
     * @return true if succeeded for all entries
     */
    public boolean save() {
        boolean ok = true;
        for (UserActivity um : userActivities.values()) {
            if (!um.save()) {
                ok = false;
            }
        }
        return ok;
    }
}
