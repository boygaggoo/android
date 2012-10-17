package eu.ttbox.geoping.ui.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.NotifToasts;
import eu.ttbox.geoping.domain.PairingProvider;

/**
 * {link http://www.vogella.com/articles/AndroidWidgets/article.html}
 * 
 * @author jmorille
 * 
 */
@TargetApi(14)
public class PairingWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "PairingWidgetProvider";

    private static final String ACTION_CLICK = "ACTION_CLICK";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static PairingWidgetDataProviderObserver sDataObserver;
    
    public PairingWidgetProvider() {
        // Start the worker thread
        sWorkerThread = new HandlerThread("PairingWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onEnabled(Context context) {
        // Register for external updates to the data to trigger an update of the widget.  When using
        // content providers, the data is often updated via a background service, or in response to
        // user interaction in the main app.  To ensure that the widget always reflects the current
        // state of the data, we must listen for changes and update ourselves accordingly.
     
        if (sDataObserver == null) {
            final ContentResolver r = context.getContentResolver();
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, PairingWidgetProvider.class);
            sDataObserver = new PairingWidgetDataProviderObserver(mgr, cn, sWorkerQueue);
            r.registerContentObserver(PairingProvider.Constants.CONTENT_URI, true, sDataObserver);
         }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, String.format("OnReceive Intent %s : %s", action, intent));
        if (action.equals(ACTION_CLICK)) {
            final int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final String phoneNumber = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            // Send it
            Intent intentGeoPing = Intents.sendSmsGeoPingResponse(context, phoneNumber);
            context.startService(intentGeoPing);
            // Display Notif
            NotifToasts.showToastSendGeoPingResponse(context, phoneNumber);
         }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ComponentName thisWidget = new ComponentName(context, PairingWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {
            // Specify the service to provide data for the collection widget.
            // Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, PairingWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_pairing);
            rv.setRemoteAdapter(R.id.widget_person_list, intent);

            // Bind a click listener template for the contents of the weather
            // list. Note that we
            // need to update the intent's data if we set an extra, since the
            // extras will be ignored otherwise.
            final Intent onClickIntent = new Intent(context, PairingWidgetProvider.class);
            onClickIntent.setAction(PairingWidgetProvider.ACTION_CLICK);
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_person_list, onClickPendingIntent);

        
            appWidgetManager.updateAppWidget(widgetId, rv);
        }
        // Super
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    
    class PairingWidgetDataProviderObserver extends ContentObserver {
        private AppWidgetManager mAppWidgetManager;
        private ComponentName mComponentName;

        PairingWidgetDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
            super(h);
            mAppWidgetManager = mgr;
            mComponentName = cn;
        }

        @Override
        public void onChange(boolean selfChange) {
            // The data has changed, so notify the widget that the collection view needs to be updated.
            // In response, the factory's onDataSetChanged() will be called which will requery the
            // cursor for the new data.
            mAppWidgetManager.notifyAppWidgetViewDataChanged(
                    mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.widget_person_list);
        }
    }
}
