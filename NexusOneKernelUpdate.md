# Introduction #

This page describes the procedure for updating your Nexus One phone with a kernel that supports tethering. It will work with both Wireless and Wired (usb) tether. It also includes a loopback block driver for mounting filesystem images.

# Details #

Download the new kernel update from the Download page.

**If you are running on a _user_ build (default install), see further below**

Instructions for userdebug or eng build:

  1. **You must be running a userdebug or eng build**. This update won't run on the 'user' build, you will get 'verification failed'.
  1. Download the update from the 'Downloads' page above.
  1. Rename it to 'update.zip' and copy it to the top directory of your phone's SD card.
  1. If you have 'adb':
    1. adb reboot recovery
  1. If you do NOT have adb
    1. Power off your phone.
    1. Turn it on **whilst holding the trackball down**
    1. At the boot screen, use the volume keys to select 'bootloader' then press the power button to select.
    1. At the next screen, select 'recovery' and press the power button to select it.
  1. The boot 'cross' will appear, and shortly after a '!' graphic will appear. Hold down the power button, then press 'volume up'.
  1. You will see a menu with blue text. Use the trackball to highlight and select 'apply SD card update'
  1. The process should take a few seconds, after which you can reboot your phone.
  1. If you get 'verification failed' you probably have a user build. See below.
  1. You now have a tether-enabled kernel.

Instructions for user build (with root hack only):
**All steps before step 12 are safe and wont touch your phone's Rom /flash**

  1. Get a copy of 'adb' and 'fastboot' (check the SDK or google for them).
  1. Grab the Nexus One update zip **and** 'nexusone-zimage' from the downloads page
  1. Plug your phone's USB in.
  1. adb reboot bootloader
  1. When the bootloader screen appears: fastboot boot nexusone-zimage
  1. **Make sure the phone boots, if not, STOP**
    1. Note that Wifi will not work at this stage.
    1. If the phone fails to boot, something is wrong. Pull the battery to reboot.
  1. Unzip the update zipfile, then 'adb push bcm4329.ko /sqlite\_stmt\_journals/'
  1. adb shell
  1. su
  1. insmod /sqlite\_stmt\_journals/bcm4329.ko
    1. If an error appears doing this, **STOP**
  1. lsmod
    1. You should see 'bcm4329' listed. If not, **STOP**.
  1. mount -w -o remount /dev/block/mtdblock3 /system
  1. cat /sqlite\_stmt\_journals/bcm4329.ko > /system/lib/modules/bcm4329.ko
  1. Logout out of the phone.
  1. adb reboot bootloader
  1. When the boot screen appears: fastboot flash zimage nexusone-zimage
  1. fastboot reboot
  1. Your phone should come up, you are done!!