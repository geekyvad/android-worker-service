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
package com.geekyvad.android.workerserviceapp.svc;

import android.util.Log;
import com.geekyvad.android.workerservice.svc.WorkerManager;
import com.geekyvad.android.workerservice.svc.WorkerService;
import com.geekyvad.android.workerservice.svc.WorkerServiceConfig;
import com.geekyvad.android.workerserviceapp.svc.counter.SimpleCounter;
import com.geekyvad.android.workerserviceapp.svc.restartable.RestartableCounter;

/**
 * @author Vadim Zadorozhny (created on 06.07.2016)
 */
public class MainService extends WorkerService
{

  @Override
  protected void onRegisterWorkerClasses( WorkerManager workerManager )
  {
    workerManager.registerWorkerClass( SimpleCounter.TYPE, SimpleCounter.class );
    workerManager.registerWorkerClass( RestartableCounter.TYPE, RestartableCounter.class );
  }

  @Override
  protected WorkerServiceConfig onCreateConfiguration()
  {
    return super.onCreateConfiguration()
        .setLogLevel( Log.VERBOSE )
        .setThrowRuntimeExceptions( true );
  }
}
