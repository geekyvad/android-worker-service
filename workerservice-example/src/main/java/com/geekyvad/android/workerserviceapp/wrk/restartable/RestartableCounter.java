/*
 * Copyright (C) 2016  Vadim Zadorozhny
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.geekyvad.android.workerserviceapp.wrk.restartable;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import com.geekyvad.android.workerservice.wrk.HandlerWorker;
import com.geekyvad.android.workerserviceapp.R;
import com.geekyvad.android.workerserviceapp.gui.MainActv;
import com.geekyvad.android.workerserviceapp.wrk.MainService;
import org.greenrobot.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class RestartableCounter extends HandlerWorker
{
  /* Control */

  public static final String TYPE = "MyWorker2";
  public static final String NAME = "RestartableCountWorker";

  public static final int NOTIFICATION_ID = 123;

  public static void createWorker( @NonNull Context context )
  {
    Intent intent = MainService.createStartWorkerIntent( context, TYPE, NAME, null );
    MainService.processIntent( context, intent );
  }

  public static void stopWorker( @NonNull Context context, boolean wait )
  {
    MainService
        .processIntent( context, MainService.createStopWorkerIntent( context, NAME, wait ) );
  }

  public enum Command
  {
    START,
    STOP,
    RANGE,
    ENABLE_FOREGROUND,
  }

  public static void setRange( @NonNull Context context, int start, int end )
  {
    Message msg = new Message();
    msg.what = Command.RANGE.ordinal();
    msg.arg1 = start;
    msg.arg2 = end;
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
    Intent intent = MainService.createWorkerMessageIntent( context, NAME, msg );
    MainService.processIntent( context, intent );
  }

  public static void enableForeground( @NonNull Context context, boolean enable )
  {
    Message msg = new Message();
    msg.what = Command.ENABLE_FOREGROUND.ordinal();
    msg.arg1 = enable ? 1 : 0;
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
    Intent intent = MainService.createWorkerMessageIntent( context, NAME, msg );
    MainService.processIntent( context, intent );
  }

  public static void start( @NonNull Context context )
  {
    Message msg = new Message();
    msg.what = Command.START.ordinal();
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
  }

  public static void stop( @NonNull Context context )
  {
    Message msg = new Message();
    msg.what = Command.STOP.ordinal();
    MainService.processIntent( context,
        MainService.createWorkerMessageIntent( context, NAME, msg ) );
  }

  public static final int COUNT_LIMIT = 20;

  /* Life cycle */

  @Override
  public void onCreate( @Nullable Bundle params )
  {
    mEnd = COUNT_LIMIT;
  }

  @Override
  public void onMessage( Message message )
  {
    Command cmd = Command.values()[ message.what ];
    switch( cmd ) {
    case START:
      restartCounter();
      break;
    case STOP:
      cancelSubscription();
      break;
    case RANGE:
      mStart = message.arg1;
      mEnd = message.arg2;
      break;
    case ENABLE_FOREGROUND:
      if( mIsForegroundEnabled && message.arg1 == 0 ) {
        // Disable foreground
        mIsForegroundEnabled = false;
        if( isCounting() ) {
          stopForeground();
        }
      } else if( !mIsForegroundEnabled && message.arg1 == 1 ) {
        // Enable foreground
        mIsForegroundEnabled = true;
        if( isCounting() ) {
          startForeground();
        }
      }
      break;
    }
  }

  @Override
  public void onDestroy()
  {
    cancelSubscription();
    stopForeground();
  }

  /* Counter implementation */

  private void restartCounter()
  {
    cancelSubscription();
    rx.Observable<Long> counterObservable = rx.Observable.interval( 100, 1000, TimeUnit.MILLISECONDS );
    mSubscription = counterObservable
        .map( new Func1<Long, Integer>()
        {
          @Override
          public Integer call( Long aLong )
          {
            return aLong.intValue();
          }
        } )
    .take( mEnd - mStart + 1)
    .observeOn( AndroidSchedulers.from( getLooper() ) )
    .subscribe( new rx.Observer<Integer>() {
      @Override
      public void onNext( Integer integer )
      {
        EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCount( integer + mStart ) );
        if( mIsForegroundEnabled ) {
          updateForegroundNotification();
        }
      }

      @Override
      public void onCompleted()
      {
        EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( true ) );
        // stop foreground but the flag mIsForegroundEnabled keeps its value
        getManager().getWorkerService().stopForeground( true );
      }

      @Override
      public void onError( Throwable e )
      {
        getManager().getWorkerService().stopForeground( true );
      }
    });

    EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( false ) );
    if( mIsForegroundEnabled ) {
      startForeground();
    }
  }

  private void cancelSubscription()
  {
    if( mSubscription != null && !mSubscription.isUnsubscribed() ) {
      mSubscription.unsubscribe();
    }
    EventBus.getDefault().postSticky( RestartableCounterEvents.CounterStatus.obtain().setCompleted( true ) );
    if( mIsForegroundEnabled ) {
      stopForeground();
    }
  }

  private boolean isCounting()
  {
    return mSubscription != null && !mSubscription.isUnsubscribed();
  }

  /* Service foreground implementation */

  private void startForeground()
  {
    // Back stack for main activity
    Intent resultIntent = new Intent( getContext(), MainActv.class );
    resultIntent.addFlags( Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT );

//    TaskStackBuilder stackBuilder = TaskStackBuilder.create( getContext() );
//    stackBuilder.addParentStack( MainActv.class);
//    stackBuilder.addNextIntent( resultIntent );
//    PendingIntent resultPendingIntent =
//        stackBuilder.getPendingIntent( 0, PendingIntent.FLAG_UPDATE_CURRENT );
    PendingIntent resultPendingIntent = PendingIntent.getActivity(
        getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT );

    // Notification
    mNotificationBuilder = new NotificationCompat.Builder( getContext() );
    mNotificationBuilder
        .setSmallIcon( R.drawable.ic_hourglass_empty_white_24dp )
        .setContentTitle( "Counter" )
        .setContentIntent( resultPendingIntent );
    Notification notif = createNotification();

    // Start service
    getManager().getWorkerService().startForeground( NOTIFICATION_ID, notif );
  }

  private void stopForeground()
  {
    getManager().getWorkerService().stopForeground( true );
    mNotificationBuilder = null;
  }

  private void updateForegroundNotification()
  {
    Notification notif = createNotification();
    NotificationManagerCompat notifManager = NotificationManagerCompat.from( getContext() );
    notifManager.notify( NOTIFICATION_ID, notif );
  }

  private Notification createNotification()
  {
    int count = RestartableCounterEvents.CounterStatus.obtain().count;
    mNotificationBuilder.setNumber( count );
    return mNotificationBuilder.build();
  }


  /* Data members */

  private int mStart;
  private int mEnd;

  rx.Subscription mSubscription;

  private NotificationCompat.Builder mNotificationBuilder;
  private boolean mIsForegroundEnabled;

}
