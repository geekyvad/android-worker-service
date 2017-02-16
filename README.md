# android-worker-service

Android service that allows multiple asynchronous workers to be controlled independently and simplifies implementation of background business logic of your android app.

Usage
-----

**Gradle dependency**

  -  Add the following to your project level `build.gradle`:

  
```gradle
allprojects {
	repositories {
		maven { url 'https://dl.bintray.com/geekyvad/maven' }
	}
}
```
  -  Add this to your app `build.gradle`:
 
```gradle
dependencies {
	compile 'com.geekyvad.android:workerservice:x.y.z'
}
```

Where x.y.z - version. Check tags in master branch to find out the latest one.

Dependencies
------------

  * [Android support annotations](https://developer.android.com/reference/android/support/annotation/package-summary.html)
  * [greenrobot EventBus](https://github.com/greenrobot/EventBus)


License
-------


    Copyright (C) 2016 Vadim Zadorozhny
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
