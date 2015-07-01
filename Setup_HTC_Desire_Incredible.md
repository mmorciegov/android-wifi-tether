# Setup Howto for HTC Desire, HTC Incredible, HTC Aria, HTC Wildfire #

**(These extra setup-steps are not required if you have Android 2.2 (FroYo) installed.) A rooted handset is mandatory.**

The **"HTC Desire"**, **"HTC Droid Incredible"** and **"HTC Aria"** need some extra setup-steps to make "Wireless Tether for Root Users" work.

(First you need to root your device. Please use google-search to find a guide/tutorial).



---

**1)** **Download** a **HTC Evo 4g system-dump** (use google search - I bet you will find one)

**2)** **Extract a file** named **fw\_bcm4329\_ap.bin** (located in /system/etc/firmware)

**3)** **Mount your sdcard**

**4)** Create a directory named "android.tether" (without quotes)

**5)** Open this directory and copy/move fw\_bcm4329\_ap.bin into it.

**6)** Rename **fw\_bcm4329\_ap.bin to fw\_bcm4329.bin**

**7)** **Install** the latest versions of **"wireless tether"**

---


### Update ###
Depending on which device you have and firmware-version you need the latest **experimental** release of the app:

**[Experimental](http://code.google.com/p/android-wifi-tether/downloads/list?can=2&q=Experimental)**

---


### Additional Information ###
Please consult this issue report where this workaround was discussed:

**[Issue 330](http://code.google.com/p/android-wifi-tether/issues/detail?id=330#c66)**,
**[Issue 410](https://code.google.com/p/android-wifi-tether/issues/detail?id=410)**

---
