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

    // Must be called prior to any service operations
    MainService.setWorkerServiceClass( MainService.class );

    EventBus.builder()
        .logNoSubscriberMessages( false )
        .sendNoSubscriberEvent( false )
        .throwSubscriberException( true )
        .installDefaultEventBus();
  }
}
