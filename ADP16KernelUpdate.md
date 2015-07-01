# ADP 1.6 - Kernel-Update #

<font color='#990000' size='4'>HTC has signed the official image with wrong keys.</font>

See:

[http://groups.google.com/group/Android-DevPhone-Updating/browse\_thread/thread/54f7a05cc06c7072/7b7972ec6f1cad72](http://groups.google.com/group/Android-DevPhone-Updating/browse_thread/thread/54f7a05cc06c7072/7b7972ec6f1cad72)

[http://groups.google.com/group/Android-DevPhone-Updating/browse\_thread/thread/5b7c52f40d91d2ef](http://groups.google.com/group/Android-DevPhone-Updating/browse_thread/thread/5b7c52f40d91d2ef)

**It looks like that the new recovery (image/mode) operates with wrong keys.**
**The only way** to get this kernel-update installed (if you don't want to use fastboot) is to replace it. I've put a package together which helps to do that. It includes a recovery.img from cupcake (the one which was installed before).

**=== AT YOUR OWN RISK ===**

**1)** You need to have "adb" in your PATH. [adb](http://developer.android.com/guide/developing/tools/adb.html) is part of the [Android-SDK](http://developer.android.com/sdk/1.6_r1/index.html) (binary is located in "tools"-folder).

**2)** Download this package: [replace\_recovery.zip](http://android-wifi-tether.googlecode.com/files/replace_recovery.zip) and extract it.

**3)** Execute "replace\_recovery.bat" - Windows-users only (of course).
> => If you are using a Mac or Linux please follow instructions in "Linux\_MAC\_Howto.txt".


---


<font color='#990000' size='4'>For ADP1.6 (Donut) ONLY!</font>

**This kernel-update is for the OFFICIAL ADP1.6 FIRMWARE published by HTC only!**

You might have noticed that "Wifi Tether for Root Users" does not work anymore after updating your **Android DevPhone 1** to [ADP1.6 (donut)](http://developer.htc.com/adp.html) **(official firmware from HTC)**.

The new kernel does not support netfilter/iptables! To make our application run you have to **replace the original kernel** with our "netfilter-enabled" kernel.

<font color='#990000' size='4'>This kernel-update won't work correctly on other devices/firmwares. Don't apply this update on a HTC Magic for instance!</font>


---


## Instruction ##

**=== AT YOUR OWN RISK ===**

**1)** Install the official firmware (if not already installed) - [ADP1.6 (donut)](http://developer.htc.com/adp.html)

**=== MAKE SURE THE NEW FIRMWARE IS INSTALLED CORRECTLY ===**

**2)** Download the kernel-update from [here](http://android-wifi-tether.googlecode.com/files/adp-1_6-kernel-update_r2.zip)

**3)** Copy adp-1\_6-kernel-update.zip to you sd-card and rename it to "update.zip"

**4)** Reboot the device into recovery mode by holding down the HOME key during reboot. When the device enters recovery mode, it displays a "!" icon.

**5)** With the recovery console displayed, open the sliding keyboard and hold down the ALT+l key combination to enable log output in the recovery console.

**6)** Next, hold down the ALT+s key combination to install the update. An "installing update" icon and progress bar (or a similar status message) are displayed. When the progress bar completes, the installation is finished.

**7)** Press the HOME-BACK key combination to reboot.
Enjoy!


---


## Security ##
The kernel-update installs a su binary which is theoretically callable from any other installed application.
If you want to **close this "security hole"** I recommend to install an application called **"Superuser Whitelist"** available **[here](http://www.koushikdutta.com/2008/11/update-to-superuser.html)**.
I'm not sure under which license this app is distributed so I've decided to not include it in the kernel-update-package.