package com.geekyvad.workerserviceapp;

import android.app.Application;
import com.geekyvad.workerserviceapp.svc.MainService;
import org.greenrobot.eventbus.EventBus;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class ExampleApp extends Application
{
  @Override
  public void onCreate()
  {
    super.onCreate();

    // Must be called before any service operations
    MainService.setWorkerServiceClass( MainService.class );

    EventBus.builder()
        .logNoSubscriberMessages( false )
        .sendNoSubscriberEvent( false )
        .throwSubscriberException( true )
        .installDefaultEventBus();
  }
}
