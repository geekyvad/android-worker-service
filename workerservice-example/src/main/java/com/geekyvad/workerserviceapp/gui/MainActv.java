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
package com.geekyvad.workerserviceapp.gui;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.geekyvad.workerserviceapp.R;
import com.geekyvad.workerserviceapp.databinding.MainActvBinding;
import com.geekyvad.workerserviceapp.svc.MainService;
import com.geekyvad.workerserviceapp.svc.counter.SimpleCounter;
import com.geekyvad.workerserviceapp.svc.counter.SimpleCounterEvents;
import com.geekyvad.workerserviceapp.svc.restartable.RestartableCounter;
import com.geekyvad.workerserviceapp.svc.restartable.RestartableCounterEvents;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActv extends AppCompatActivity
{

  @Override
  protected void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    MainActvBinding binding = DataBindingUtil.setContentView( this, R.layout.main_actv );
    binding.setSimpleCounter( mSimpleCounterBind );
    binding.setRestartableCounter( mRestartableCounterBind );

    // Startup worker here because we'll send commands to it from other places of this activity
    RestartableCounter.createWorker( getApplicationContext() );

    if( savedInstanceState != null ) {
      mRestartableCounterBind.start.set( savedInstanceState.getString( STATE_RESTARTABLE_START ) );
      mRestartableCounterBind.end.set( savedInstanceState.getString( STATE_RESTARTABLE_END ) );
    }
  }

  @Override
  protected void onSaveInstanceState( Bundle outState )
  {
    super.onSaveInstanceState( outState );
    outState.putString( STATE_RESTARTABLE_START, mRestartableCounterBind.start.get() );
    outState.putString( STATE_RESTARTABLE_END, mRestartableCounterBind.end.get() );
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    EventBus.getDefault().register( this );
  }

  @Override
  protected void onStop()
  {
    EventBus.getDefault().unregister( this );
    super.onStop();
  }

  @Override
  protected void onDestroy()
  {
    if( !isChangingConfigurations() && MainService.isCreated() ) {
      MainService.shutdownService( getApplicationContext(), false, true );
    }
    super.onDestroy();
  }

  public class SimpleCounterBind
  {
    public ObservableInt counter = new ObservableInt();
    public ObservableBoolean completed = new ObservableBoolean( true );

    @SuppressWarnings( "unused" )
    public void onStartStopClicked( View v )
    {
      SimpleCounterEvents.CounterStatus status = SimpleCounterEvents.CounterStatus.obtain();
      if( status.completed ) {
        SimpleCounter.startWorker( getApplicationContext() );
      } else {
        SimpleCounter.stopWorker( getApplicationContext(), false );
      }
    }
  }

  public class RestartableCounterBind
  {
    public ObservableInt counter = new ObservableInt();
    public ObservableBoolean completed = new ObservableBoolean( true );
    public ObservableField<String> start = new ObservableField<>( "0" );
    public ObservableField<String> end = new ObservableField<>( "20" );


    @SuppressWarnings( "unused" )
    public void onStartStopClicked( View v )
    {
      RestartableCounterEvents.CounterStatus status = RestartableCounterEvents.CounterStatus.obtain();
      if( status.completed ) {
        RestartableCounter.setRange( getApplicationContext(),
            Integer.valueOf( start.get() ), Integer.valueOf( end.get() ) );
        RestartableCounter.start( getApplicationContext() );
      } else {
        // Does not stop worker, but simple stops counting
        RestartableCounter.stop( getApplicationContext() );
      }
    }

    @SuppressWarnings( "unused" )
    public void onRestartClicked( View v )
    {
      RestartableCounter.stop( getApplicationContext() );
      RestartableCounter.setRange( getApplicationContext(),
          Integer.valueOf( start.get() ), Integer.valueOf( end.get() ) );
      RestartableCounter.start( getApplicationContext() );
    }
  }

  @Subscribe( sticky = true, threadMode = ThreadMode.MAIN )
  @SuppressWarnings( "unused" )
  public void onSimpleCounterStatusChanged( SimpleCounterEvents.CounterStatus status )
  {
    mSimpleCounterBind.counter.set( status.count );
    mSimpleCounterBind.completed.set( status.completed );
  }

  @Subscribe( sticky = true, threadMode = ThreadMode.MAIN )
  @SuppressWarnings( "unused" )
  public void onRestartableCounterStatusChanged( RestartableCounterEvents.CounterStatus status )
  {
    mRestartableCounterBind.counter.set( status.count );
    mRestartableCounterBind.completed.set( status.completed );
  }

  private SimpleCounterBind mSimpleCounterBind = new SimpleCounterBind();
  private RestartableCounterBind mRestartableCounterBind = new RestartableCounterBind();

  private static final String STATE_RESTARTABLE_START = "start";
  private static final String STATE_RESTARTABLE_END = "end";
}
