
## **Reference**
| <img src="https://raw.githubusercontent.com/ZSShen/Android-Code-Morph/master/res/AndroidProtectionIntro.png" width="450px" height="400"/> |
|---|
| The scheme is inspired by the technical doc posted by [Jack Jia]. The objective of code packing is to hide the original APK in the static view. Thus it is hard for adversaries to realize the APK logic without dynamic tracing. In general, there is no uncrackable protection scheme. What we can do is just increase the effort for adversaries to break the protection. |

## **Limitation**
Currently, the scheme relies on `DexClassLoader` to load the protected APK. However, the API supporting `in memory class loading` is removed since `Android 4.4`. With current API, we have to specify the directory to store the optimized DEX. And it offers the opportunity for adversaries to steal the unpacked payload!  

## **Rationale**

### **Basic Idea**
For the programs compiled to native code, which can be directly executed by processor, the packer just simply jumps to the entry point of the protected text after the unpacking is finished. But for the programs compiled to intermediate language, the packer should prepare the appropriate runtime environment to ensure that the protected app can be normally executed. Though modern Android applies AoT compilation and most of the app code is directly executed by processor, the app is still loaded and managed by Android Runtime. Therefore, the proposed Android packer should be able to accomplish such context switch. Specifically, the packer must:  
  + Prepare the custom class loader to load the unpacked code.  
  + Change specific fields of Runtime management structures which are initially set for the packer class.  
  + When the environment is ready, it should trigger the execution for the original code.  

For this, there are two major issues to be solved. The one is the timing for the packer to unpack and load the original code. And the other is the replacement for the Runtime structure field for context switch.   

### **Issues**
#### **Timing to Load Original Code**
`ActivityThread` is the class which manages the execution of the main thread in an app process. Specially, it serves the requests from `ActivityManager` and manages the life cycle of `Application`, `Activity`, `Service`, and `ContentProvider` components. When a new app process is created, `ActivityThread` launches a series of class loading procedures to customize the environment for it. By tracing the Android source code, we can notice that `ActivityThread.handleBindApplication()` is the starting point of that series of initialization. Basically, the method should:  
+ Create the base app context via `ContextImpl.createAppContext()`.  
+ Create the `Application` instance via `LoadedApk.makeApplication()`.  
+ Install the `ContentProvider` instances via `ActivityThread.installContentProviders()`.
+ Invoke `Application.onCreate()` for the app.

After the above initialization, `ActivityThread` will further receive the requests relevant to the launch of `Activity` classes and the creation of `Service` classes. But in our scenario, we have to focus on `ActivityThread.handleBindApplication()`.  Since it is the very first timing to create the `Application` and `ContentProvider` instances. 

Then when is the timing to unpack and load the original code? As mentioned above, `Application.onCreate()` is executed after the installation of `ContentProvider`s. If we put the unpacking logic in it, we will miss the providers. Fortunately, if we trace into the call chain of `LoadedApk.makeApplication()`, we can notice that it invokes `Application.attachBaseContext()` to set the base context for the newly initialized `Application` instance.

Now we know to craft the packer APK. Create an `Application` class and overwrite the `Application.attachBaseContext()` method to put our work. Specifically, unpack the original code and generate the custom class loader to load the payload. Then the first step is finished, and we should fall through the context switch part for normal Runtime environment.  


#### **Context Switch**
To be updated soon.

<p align="center">
  <img src="https://github.com/ZSShen/Android-Code-Morph/blob/master/res/AndroidProtectionResearch.png" width="650px" height="650"/>
</p>




[Jack Jia]: http://blog.csdn.net/androidsecurity/article/details/8809542
