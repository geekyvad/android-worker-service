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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

/**
 * @author Vadim Zadorozhny (created on 20.07.2016)
 */
public class ProgressDlgFrg extends DialogFragment
{
  @NonNull
  @Override
  public Dialog onCreateDialog( Bundle savedInstanceState )
  {
    ProgressDialog dlg = new ProgressDialog( getContext() );
    dlg.setMessage( "Please wait..." );
    dlg.setOnKeyListener( new DialogInterface.OnKeyListener()
    {
      @Override
      public boolean onKey( DialogInterface dialog, int keyCode, KeyEvent event )
      {
        return keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP;
      }
    } );
    dlg.setCanceledOnTouchOutside( false );
    return dlg;
  }

}
