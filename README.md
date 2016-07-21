
## **Reference**
| <img src="https://raw.githubusercontent.com/ZSShen/Android-Code-Morph/master/res/AndroidProtectionIntro.png" width="450px" height="400"/> |
|---|
| The scheme is inspired by the technical doc posted by [Jack Jia]. The objective of code packing is to hide the original APK in the static view. Thus it is hard for adversaries to realize the APK logic without dynamic tracing. In general, there is no uncrackable protection scheme. What we can do is just increase the effort for adversaries to break the protection. |

## **Limitation**
Currently, the scheme relies on `DexClassLoader` to load the protected APK. However, the API supporting `in memory class loading` is removed since `Android 4.4`. With current API, we have to specify the directory to store the optimized DEX. And it offers the opportunity for adversaries to steal the unpacked payload!  

## **Rationale**

For the programs compiled into native code, which can be directly executed by processor, the packer just simply jumps to the entry point of the protected text after the unpacking is finished. But for the programs compiled into intermediate language, the packer should prepare the appropriate runtime environment to ensure that the protected app can be normally executed. Though modern Android applies AoT compilation and most of the app code is directly executed by processor, the app is still loaded and managed by Android Runtime. Therefore, the proposed Android packer should be able to accomplish such context switch. Specifically, the packer must:  
  + Prepare the custom class loader to load the unpacked APK.  
  + Change specific fields of Runtime management structures which are initially set for the packer class.  
  + When the environment is ready, it should trigger the execution for the original APK.  


[Jack Jia]: http://blog.csdn.net/androidsecurity/article/details/8809542
