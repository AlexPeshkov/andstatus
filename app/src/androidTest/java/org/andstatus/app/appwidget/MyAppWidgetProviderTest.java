/*
 * Copyright (C) 2010-2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.appwidget;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;

import org.andstatus.app.context.DemoData;
import org.andstatus.app.context.MyContext;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.DbUtils;
import org.andstatus.app.notification.NotificationEventType;
import org.andstatus.app.notification.NotificationEvents;
import org.andstatus.app.notification.Notifier;
import org.andstatus.app.service.MyServiceManager;
import org.andstatus.app.util.MyLog;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Runs various tests...
 * @author yvolk@yurivolkov.com
 */
public class MyAppWidgetProviderTest {
    private MyContext myContext;
    
    @Before
    public void setUp() throws Exception {
        TestSuite.initializeWithData(this);
        myContext = MyContextHolder.get();
        MyServiceManager.setServiceUnavailable();
        initializeDateTests();
    }

    @Test
    public void testTimeFormatting() throws Exception {
        MyLog.v(this, "testTimeFormatting started");
    	
        for (DateTest dateTest : dateTests) {
            if (dateTest == null) {
                break;
            }
            long startMillis = dateTest.date1.toMillis(false /* use isDst */);
            long endMillis = dateTest.date2.toMillis(false /* use isDst */);
            int flags = dateTest.flags;
            String output = DateUtils.formatDateRange(myContext.context(), startMillis, endMillis, flags);
            String output2 = MyRemoteViewData.formatWidgetTime(myContext.context(), startMillis, endMillis);
            MyLog.v(this, "\"" + output + "\"; \"" + output2 + "\"");
        }         
    }   

    private DateTest[] dateTests = new DateTest[101];
    
    static private class DateTest {
        Time date1;
        Time date2;
        int flags;
        
        DateTest(long startMillis, long endMillis) {
        	date1 = new Time();
        	date1.set(startMillis);
        	date2 = new Time();
        	date2.set(endMillis);
        	flags = DateUtils.FORMAT_24HOUR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;
        }
    }
    
    private void initializeDateTests() {
    	// Initialize dateTests
    	int ind = 0;
    	Calendar cal1 = Calendar.getInstance();
    	Calendar cal2 = Calendar.getInstance();

    	cal1.setTimeInMillis(System.currentTimeMillis());
    	cal2.setTimeInMillis(System.currentTimeMillis());
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());
    	
    	ind += 1;
    	cal1.roll(Calendar.SECOND, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.roll(Calendar.SECOND, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());
    	
    	ind += 1;
    	cal1.roll(Calendar.MINUTE, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.add(Calendar.SECOND, 5);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.roll(Calendar.MINUTE, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.roll(Calendar.HOUR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.roll(Calendar.HOUR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());
    	
    	ind += 1;
    	cal1.roll(Calendar.DAY_OF_YEAR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());
    	
    	ind += 1;
    	cal1.roll(Calendar.DAY_OF_YEAR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal2.roll(Calendar.MINUTE, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal2.roll(Calendar.HOUR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal2.roll(Calendar.HOUR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());

    	ind += 1;
    	cal1.roll(Calendar.YEAR, false);
    	dateTests[ind] = new DateTest(cal1.getTimeInMillis(), cal2.getTimeInMillis());
    }

    @Test
    public void testReceiver() throws Exception {
        final String method = "testReceiver";
    	MyLog.i(this, method + "; started");

        long dateSinceMin = System.currentTimeMillis();
        final Notifier notifier = MyContextHolder.get().getNotifier();
        NotificationEvents events = notifier.events;
    	// To set dateSince correctly!
        updateWidgets(events, NotificationEventType.ANNOUNCE, 1);
        DbUtils.waitMs(method, 5000);
        long dateSinceMax = System.currentTimeMillis();
        DbUtils.waitMs(method, 5000);
        AppWidgets.clearAndUpdateWidgets(myContext);
        notifier.clearAll();

        checkWidgetData(0, 0, 0);
        checkDateChecked(dateSinceMin, dateSinceMax);

    	int numMentions = 3;
        updateWidgets(events, NotificationEventType.MENTION, numMentions);
    	
    	int numPrivate = 1;
        updateWidgets(events, NotificationEventType.PRIVATE, numPrivate);
    	
    	int numReblogs = 7;
        updateWidgets(events, NotificationEventType.ANNOUNCE, numReblogs);
    	
        checkWidgetData(numMentions, numPrivate, numReblogs);
        
        long dateCheckedMin = System.currentTimeMillis();  
        numMentions++;
        updateWidgets(events, NotificationEventType.MENTION, 1);
        checkWidgetData(numMentions, numPrivate, numReblogs);
        long dateCheckedMax = System.currentTimeMillis();
        
        checkDateSince(dateSinceMin, dateSinceMax);
        checkDateChecked(dateCheckedMin, dateCheckedMax);
    }

    private void checkWidgetData(long numMentions, long numPrivate, long numReblogs)
            throws InterruptedException {
        final String method = "checkWidgetData";
        DbUtils.waitMs(method, 500);

    	AppWidgets appWidgets = AppWidgets.newInstance(myContext);
    	if (appWidgets.isEmpty()) {
            MyLog.i(this, method + "; No appWidgets found");
    	}
    	for (MyAppWidgetData widgetData : appWidgets.collection()) {
    	    NotificationEvents events = widgetData.notifier.events;
            assertEquals("Mentions " + widgetData.toString(), numMentions,
                    events.getCount(NotificationEventType.MENTION));
    	    assertEquals("Private " + widgetData.toString(), numPrivate,
                    events.getCount(NotificationEventType.PRIVATE));
            assertEquals("Reblogs " + widgetData.toString(), numReblogs,
                    events.getCount(NotificationEventType.ANNOUNCE));
    	}
    }

    private void checkDateSince(long dateMin, long dateMax)
            throws InterruptedException {
        final String method = "checkDateSince";
        DbUtils.waitMs(method, 500);

        AppWidgets appWidgets = AppWidgets.newInstance(myContext);
        if (appWidgets.isEmpty()) {
            MyLog.i(this, method + "; No appWidgets found");
        }
        for (MyAppWidgetData widgetData : appWidgets.collection()) {
            assertDatePeriod(method, dateMin, dateMax, widgetData.dateSince);
        }
    }

    private void checkDateChecked(long dateMin, long dateMax)
            throws InterruptedException {
        final String method = "checkDateChecked";
        DbUtils.waitMs(method, 500);

        AppWidgets appWidgets = AppWidgets.newInstance(myContext);
        if (appWidgets.isEmpty()) {
            MyLog.i(this, method + "; No appWidgets found");
        }
        for (MyAppWidgetData widgetData : appWidgets.collection()) {
            assertDatePeriod(method, dateMin, dateMax, widgetData.dateLastChecked);
        }
    }
    
    private void assertDatePeriod(String message, long dateMin, long dateMax, long dateActual) {
        if (dateActual >= dateMin && dateActual <= dateMax) {
            return;
        }
        if (dateActual == 0 ) {
            fail( message + " actual date is zero");
        } else if (dateActual < dateMin) {
            fail( message + " actual date " + dateTimeFormatted(dateActual) 
                    + " is less than expected " + dateTimeFormatted(dateMin) 
                    + " min by " + (dateMin - dateActual) + " ms");
        } else {
            fail( message + " actual date " + dateTimeFormatted(dateActual) 
                    + " is larger than expected " + dateTimeFormatted(dateMax) 
                    + " max by " + (dateActual - dateMax) + " ms");
        }
    }
    
    private static String dateTimeFormatted(long date) {
        return DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date(date)).toString();
    }

	/** 
	 * Update AndStatus Widget(s),
	 * if there are any installed... (e.g. on the Home screen...)
	 * @see MyAppWidgetProvider
	 */
	private void updateWidgets(NotificationEvents events, NotificationEventType event, int increment) throws InterruptedException{
        final String method = "updateWidgets";
        DbUtils.waitMs(method, 500);
        for (int ind = 0; ind < increment; ind++ ) {
            events.onNewEvent(event, DemoData.demoData.getConversationMyAccount(), System.currentTimeMillis());
        }
        AppWidgets appWidgets = AppWidgets.newInstance(myContext);
        appWidgets.updateData();
        appWidgets.updateViews();
	}
	
	
}