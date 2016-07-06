package com.geekyvad.workerservice.msg;

/**
 * @author Vadim Zadorozhny
 */
public interface BindableRunnable extends Runnable
{
  void bind( Object b );
  Object getBoundObject();
}
