package com.geekyvad.workerservice;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author Vadim Zadorozhny
 */
interface IWorker
{
  void init( @NonNull Context context, @NonNull String workerName, @Nullable Bundle params,
      @NonNull WorkerManager manager );

  void onCreate( @Nullable Bundle params );

  /**
   * Release any resources. Called even if exception raised in onRun().
   */
  void onDestroy();

  String getWorkerName();

}
