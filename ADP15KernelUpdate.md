# ADP 1.5 - Kernel-Update #

<font color='#990000' size='4'>For ADP1.5 (Cupcake) ONLY! ADP1.6 (Donut) is not supported!</font>

Kernel for ADP1.6 (Donut) is available [here](http://code.google.com/p/android-wifi-tether/wiki/ADP16KernelUpdate).

**This kernel-update is for the OFFICIAL ADP1.5 FIRMWARE published by HTC only!**

You might have noticed that "Wifi Tether for Root Users" does not work anymore after updating your **Android DevPhone 1** to [ADP1.5 (cupcake)](http://www.htc.com/www/support/android/adp.html) **(official firmware from HTC)**.

The new kernel does not support netfilter/iptables! To make our application run you have to **replace the original kernel** with our "netfilter-enabled" kernel.

<font color='#990000' size='4'>This kernel-update won't work correctly on other devices/firmwares. Don't apply this update on a HTC Magic for instance!</font>

## Instruction ##

**=== AT YOUR OWN RISK ===**

1) Install the official firmware (if not already installed) - [ADP1.5 (cupcake)](http://www.htc.com/www/support/android/adp.html)

**=== MAKE SURE THE NEW FIRMWARE IS INSTALLED CORRECTLY ===**

2) Download the kernel-update from [here](http://android-wifi-tether.googlecode.com/files/adp-1_5-kernel-update.zip)

3) Copy adp-1\_5-kernel-update.zip to you sd-card and rename it to "update.zip"

4) Reboot the device into recovery mode by holding down the HOME key during reboot. When the device enters recovery mode, it displays a "!" icon.

5) With the recovery console displayed, open the sliding keyboard and hold down the ALT+l key combination to enable log output in the recovery console.

6) Next, hold down the ALT+s key combination to install the update. An "installing update" icon and progress bar (or a similar status message) are displayed. When the progress bar completes, the installation is finished.

7) Press the HOME-BACK key combination to reboot.
Enjoy!