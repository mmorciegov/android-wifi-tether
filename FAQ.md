**What does this application do?**

This program enables tethering (via **wifi** and **bluetooth**) for "rooted" handsets running android (such as the Android DevPhone 1). Clients (your laptop for example) can connect via wifi (ad-hoc mode) or bluetooth and get access to the internet using the 3G, 2G mobile connection or (in case you are using bluetooth) the wifi connection which is established by the handset.


**What is this root thing and how do I get it?**

Root means more power over your device. This is useful, but can also be a security risk, so it's disabled in the retail Android distribution. You can find instructions on how to get it for the HTC Dream / G1 [here](http://forum.xda-developers.com/showthread.php?t=442480).


**How do I install the program?**

For Android 1.5 (cupcake) users you can either download it [directly](http://code.google.com/p/android-wifi-tether/downloads/list) on your phone or look it up in the Market.

For Android 1.1 users make sure you download version 0.97.1 from [here](http://code.google.com/p/android-wifi-tether/downloads/list).

**I have an old version installed and get an error-message when trying to update the app. What's wrong?**

Some custom roms come with a pre-installed version of "Wireless Tether". If that version is located on the "system"-partition the update-process will fail. The "system"-partition is read-only and needs to be remounted (with write-premission) before removing the old app manually.

**Why do I get the message "No Netfilter" on startup?**

This means that the Linux kernel on your device does not have the features (CONFIG\_NETFILTER, CONFIG\_IP\_NF\_IPTABLES and CONFIG\_NETFILTER\_XT\_MATCH\_MAC) required for tethering.

**Why does the "access-control"-feature not work or is disabled.**

If the feature "CONFIG\_NETFILTER\_XT\_MATCH\_MAC" is missing the "access control"-feature will not work correctly (you will see a "failed"-status in "Show log" for "Enabling access control"). To detect if all kernel-option were enabled in your current kernel the following kernel-options should be enabled: CONFIG\_PROC\_FS, CONFIG\_IKCONFIG, and CONFIG\_IKCONFIG\_PRO. This dumps the current kernel-config to /proc/config.gz.


If you have an ADP G1, please read [ADP15KernelUpdate](ADP15KernelUpdate.md) (or [ADP16KernelUpdate](ADP16KernelUpdate.md) if you're using Donut), otherwise you will need to find a firmware/kernel with these features. For the Nexus One phone, see [NexusOneKernelUpdate](NexusOneKernelUpdate.md). The developers of Wireless Tether are unable to help with other types of devices (but donations of hardware/etc would help ;)

**Constantly Disconnecting?**

Check: [http://www.androidpolice.com/2010/10/05/android-wifi-tether-app-for-rooted-users-constantly-disconnecting-try-these-solutions-to-fix-it/](http://www.androidpolice.com/2010/10/05/android-wifi-tether-app-for-rooted-users-constantly-disconnecting-try-these-solutions-to-fix-it/)


**Why does WEP-encryption not work on my phone?**

Wireless interfaces of almost all HTC-devices which are sold by HTC (these devices are coming with the so called "Sense" user interface) don't seem to support WEP-encryption in ad-hoc mode. We developers don't have such devices available for testing. So, if you have found any solution regarding this problem - please let us know.

**Why won't tethering start on my phone?**

First, check that you have root permissions. When you open Wifi Tether for Root Users, you should see a message warning you if you do not have root permissions. However, you may still want to check the steps [here](http://forum.xda-developers.com/showthread.php?t=442480) and make sure you have done them or something like them to obtain root permissions.

If you have root permissions, but still cannot start tethering, it may be an issue with either the tethering binaries or your tethering settings. From the main tether screen, try going to Menu->Setup->Menu->Reinstall binaries/configs.

**What keeps killing tethering or turning wifi/GPS on during tether?**

It's probably another application. The most common we've seen causing this issue is ShopSavvy. To prevent this from happening, open the Shop Savvy application and go to Settings->Privacy. If you see a "Find my location" setting and it is currently on, turn it off.


**My computer is having trouble connecting! What can I do?**

Check whether your operating system and wireless card support ad-hoc connections. Ubuntu is known to have poor support for these types of connections, for instance.

If you think your OS is not the issue, it may be an issue with either the tethering binaries or your tethering settings. From the main tether screen, try going to Menu->Setup->Menu->Reinstall binaries/configs.

If that still doesn't resolve your issue, it may be a problem with your allowed clients or security settings. Start tethering and connect your laptop. then from the main tether screen, go Menu->Setup. If "Activate Access-Control" is already checked, either uncheck it or check the entry below containing your laptop's MAC address. If "Activate Access-Control" was unchecked originally, try checking it and checking the entry below containing your laptop's MAC address.

**Windows 7 won't connect. What could be wrong?**

We have some reports [issue 244](http://code.google.com/p/android-wifi-tether/issues/detail?id=244) that some intel-wifi-cards are making troubles joining ad-hoc-networks. I driver-update solved this issue.

**I'm running JF's-Firmware (1.4x) but tethering doesn't start or my phone hangs!**

JF's firmware comes with an application called Superuser Whitelist. This application has an error which sometimes prevents programs from being able to acquire root-permission, even when the user tells it to give them!
See the [bug report](http://code.google.com/p/superuser/issues/detail?id=1). You can first try simply opening Superuser Whitelist and removing the entry for "android.tether".
If that doesn't work, try uninstalling Android Wifi Tether and installing it again. If you are lucky enough (this issue happens and/or is fixed randomly) you will no longer experience this issue!