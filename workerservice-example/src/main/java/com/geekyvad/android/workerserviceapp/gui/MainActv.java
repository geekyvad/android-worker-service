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
package com.geekyvad.android.workerserviceapp.gui;

import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.geekyvad.android.workerservice.svc.WorkerServiceEvents;
import com.geekyvad.android.workerservice.svc.WorkerServiceStarter;
import com.geekyvad.android.workerserviceapp.R;
import com.geekyvad.android.workerserviceapp.databinding.MainActvBinding;
import com.geekyvad.android.workerserviceapp.wrk.MainService;
import com.geekyvad.android.workerserviceapp.wrk.counter.SimpleCounter;
import com.geekyvad.android.workerserviceapp.wrk.counter.SimpleCounterEvents;
import com.geekyvad.android.workerserviceapp.wrk.restartable.RestartableCounter;
import com.geekyvad.android.workerserviceapp.wrk.restartable.RestartableCounterEvents;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActv extends AppCompatActivity implements
    WorkerServiceStarter.Callback
{

  @Override
  protected void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );
    MainActvBinding binding = DataBindingUtil.setContentView( this, R.layout.main_actv );
    binding.setSimpleCounter( mSimpleCounterBind );
    binding.setRestartableCounter( mRestartableCounterBind );

    if( savedInstanceState != null ) {
      mRestartableCounterBind.start.set( savedInstanceState.getString( STATE_RESTARTABLE_START ) );
      mRestartableCounterBind.end.set( savedInstanceState.getString( STATE_RESTARTABLE_END ) );

      mProgressDlgFrg = (ProgressDlgFrg) getSupportFragmentManager().findFragmentByTag( FRG_TAG_PROGRESS_DLG );
    }

    mServiceStarter = new WorkerServiceStarter();
    mServiceStarter.startService( this );
  }

  @Override
  protected void onDestroy()
  {
    if( mServiceStarter != null ) {
      mServiceStarter.cancelStart();
      mServiceStarter = null;
    }
    super.onDestroy();
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
  protected void onResume()
  {
    super.onResume();
    mActivityVisible = true;
    updateServiceForeground();
  }

  @Override
  protected void onPause()
  {
    mActivityVisible = false;
    updateServiceForeground();
    super.onPause();
  }

  @Override
  public void onOldInstanceShuttingDown()
  {
    if( mProgressDlgFrg == null ) {
      mProgressDlgFrg = new ProgressDlgFrg();
    }
    if( ! mProgressDlgFrg.isAdded() ){
      mProgressDlgFrg.show( getSupportFragmentManager(), FRG_TAG_PROGRESS_DLG );
    }
  }

  @Override
  public void onStartWorkers()
  {
    if( mProgressDlgFrg != null ) {
      mProgressDlgFrg.dismiss();
    }
    mProgressDlgFrg = null;
    mServiceStarter = null;
    // Startup worker here because we'll send commands to it from other places of this activity
    RestartableCounter.createWorker( getApplicationContext() );
    updateServiceForeground();
  }

  @Override
  public void onBackPressed()
  {
    if( isTaskRoot() ) {
      MainService.shutdownService( getApplicationContext(), true, true );
    }
    super.onBackPressed();
  }

  private void updateServiceForeground()
  {
    WorkerServiceEvents.ServiceActivityStatus status = MainService.obtainServiceStatusEvent();
    if( status.started && !status.shuttingDown ) {
      RestartableCounter.enableForeground( getApplicationContext(), !mActivityVisible );
    }
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

  private boolean mActivityVisible;
  private ProgressDlgFrg mProgressDlgFrg;
  private WorkerServiceStarter mServiceStarter;

  private static final String FRG_TAG_PROGRESS_DLG = "frgTag.ProgressDlg";

  private static final String STATE_RESTARTABLE_START = "start";
  private static final String STATE_RESTARTABLE_END = "end";
}
