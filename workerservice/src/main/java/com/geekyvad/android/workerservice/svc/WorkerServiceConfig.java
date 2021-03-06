/*
 * Copyright (C) 2016 Vadim Zadorozhny
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
package com.geekyvad.android.workerservice.svc;

import android.util.Log;

public class WorkerServiceConfig
{

  public static WorkerServiceConfig getInstance()
  {
    return ourInstance;
  }

  public WorkerServiceConfig setThrowRuntimeExceptions( boolean throwRuntimeExceptions )
  {
    mThrowRuntimeExceptions = throwRuntimeExceptions;
    return this;
  }

  public boolean isThrowRuntimeExceptions()
  {
    return mThrowRuntimeExceptions;
  }

  public WorkerServiceConfig setLogLevel( int logLevel )
  {
    mLogLevel = logLevel;
    return this;
  }

  public int getLogLevel()
  {
    return mLogLevel;
  }

  private WorkerServiceConfig()
  {
  }

  private static WorkerServiceConfig ourInstance = new WorkerServiceConfig();

  private boolean mThrowRuntimeExceptions;
  private int mLogLevel = Log.WARN;
}
