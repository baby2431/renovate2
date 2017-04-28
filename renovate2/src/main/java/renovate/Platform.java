/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package renovate;


import renovate.call.CallAdapter;
import renovate.call.DefaultCallAdapter;
import renovate.call.ExecutorCallAdapter;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

class Platform {
  private static final Platform PLATFORM = findPlatform();

  static Platform get() {
    return PLATFORM;
  }

  private static Platform findPlatform() {
    try {
      Class.forName("android.os.Build");
//      if (Build.VERSION.SDK_INT != 0) {
//        return new Android();
//      }
    } catch (ClassNotFoundException ignored) {
    }
    try {
      Class.forName("");
      return new Java8();
    } catch (ClassNotFoundException ignored) {
    }
    return new Platform();
  }

  Executor defaultCallbackExecutor() {
    return null;
  }

  //如果在Platform当中有回掉，则使用异步的，不然使用同步回调
  <T>  CallAdapter<T> defaultCallAdapterFactory(Executor callbackExecutor) {
    if (callbackExecutor != null) {
      return (CallAdapter<T>) new ExecutorCallAdapter<T>(callbackExecutor);
    }
    return DefaultCallAdapter.INSTANCE;
  }

  boolean isDefaultMethod(Method method) {
    return false;
  }

  Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object, Object... args)
      throws Throwable {
    throw new UnsupportedOperationException();
  }

//  @IgnoreJRERequirement // Only classloaded and used on Java 8.
  static class Java8 extends Platform {
    @Override boolean isDefaultMethod(Method method) {
      return method.isDefault();
    }

    @Override Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
        Object... args) throws Throwable {
      // Because the service interface might not be public, we need to use a MethodHandle lookup
      // that ignores the visibility of the declaringClass.
      Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
      constructor.setAccessible(true);
      return constructor.newInstance(declaringClass, -1 /* trusted */)
          .unreflectSpecial(method, declaringClass)
          .bindTo(object)
          .invokeWithArguments(args);
    }
  }

  static class Android extends Platform {
    @Override public Executor defaultCallbackExecutor() {
      return new MainThreadExecutor();
    }

    @Override CallAdapter defaultCallAdapterFactory(Executor callbackExecutor) {
      return new ExecutorCallAdapter(callbackExecutor);
    }

    static class MainThreadExecutor implements Executor {


      @Override public void execute(Runnable r) {
//        handler.post(r);
      }
    }
  }
}
