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

package org.andstatus.app.net;

import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Specific implementation of the {@link ApiEnum.STATUSNET_TWITTER}
 * @author yvolk@yurivolkov.com
 */
public class ConnectionTwitterStatusNet extends ConnectionTwitter1p0 {

    @Override
    protected String getApiPath1(ApiRoutineEnum routine) {
        String url;
        switch(routine) {
            case GET_CONFIG:
                url = "statusnet/config" + EXTENSION;
                break;
            default:
                url = "";
                break;
        }
        if (TextUtils.isEmpty(url)) {
            url = super.getApiPath1(routine);
        } else {
            url = http.data.basicPath + "/" + url;
        }
        return url;
    }
    
    @Override
    public List<String> getIdsOfUsersFollowedBy(String userId) throws ConnectionException {
        Uri sUri = Uri.parse(getApiPath(ApiRoutineEnum.GET_FRIENDS_IDS));
        Uri.Builder builder = sUri.buildUpon();
        builder.appendQueryParameter("user_id", userId);
        List<String> list = new ArrayList<String>();
        JSONArray jArr = http.getRequestAsArray(builder.build().toString());
        try {
            for (int index = 0; index < jArr.length(); index++) {
                list.add(jArr.getString(index));
            }
        } catch (JSONException e) {
            throw ConnectionException.loggedJsonException(this, e, null, "Parsing friendsIds");
        }
        return list;
    }

    @Override
    public MbConfig getConfig() throws ConnectionException {
        JSONObject result = http.getRequest(getApiPath(ApiRoutineEnum.GET_CONFIG));
        MbConfig config = MbConfig.getEmpty();
        if (result != null) {
            JSONObject site = result.optJSONObject("site");
            if (site != null) {
                int textLimit = site.optInt("textlimit");
                config = MbConfig.fromTextLimit(textLimit);
                //Not used: status.shortUrlLength = site.optInt("shorturllength");
            }
        }
        return config;
    }
}
