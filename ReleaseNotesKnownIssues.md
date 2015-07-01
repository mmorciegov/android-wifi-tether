# Stable versions #


---


---

## wireless\_tether-2\_0\_2 ##

### - Release notes ###

`*` Added EVO support (ad-hoc mode only)
### - Known issues ###
?

---


---

## wireless\_tether-2\_0\_1 ##

### - Release notes ###

`*` Added support for CM 5.0.7 DS (HTC Dream/Magic 32B)
`*` Small bugfix (which didn't harm most platforms) in bluetooth the bluetooth start/stop shell scripts.
### - Known issues ###
?

---


---



# Experimental versions #


---


---

## wireless\_tether-2\_0\_5-pre7 ##

### - Release notes ###

`*` Adds support for HTC Evo (Android 2.2 - FroYo) - master/infrastructure mode.
### - Known issues ###
`*` Changing channel does not work.

---


---

## wireless\_tether-2\_0\_5-pre5 ##

### - Release notes ###

`*` Support for more devices (generic wifi-setup type). Works with Samsung Galaxy S.
### - Known issues ###
?

---


---

## wireless\_tether-2\_0\_5-pre3 ##

### - Release notes ###

`*` Added master/infastructure-mode support for Broadcom bcm4329 (using a driver from HTC)
### - Known issues ###
?

---


---

## wireless\_tether-2\_0\_2-pre6 ##

### - Release notes ###

`*` Added support for the HTC Legend.
### - Known issues ###
`*` HTC Legend does not support wep-encryption in adhoc-mode.

---


---

## wireless\_tether-2\_0-pre9 ##

### - Release notes ###

`*` Small bugfixes and cleanups. Maybe the last pre-release.
### - Known issues ###
?

---


---

## wireless\_tether-2\_0-pre8 ##

### - Release notes ###

`*` **Added** support for Motorola Cliq. Needs to be tested! (Thanks to the [DemoShadow Dev Team](http://www.demoshadow.com) for helping with this.)

`*` **Added** the "reduce transmit-power"-feature for NexusOne.

### - Known issues ###
?

---


---

## wireless\_tether-2\_0-pre7 ##

### - Release notes ###

`*` **Added** additional check for kernel-compatibility - Workaround added if /proc/config.gz does not exist.

### - Known issues ###
?

---


---

## wireless\_tether-2\_0-pre6 ##

### - Release notes ###
`*` ESSID can't be changed - Filed in **[issue 264](https://code.google.com/p/android-wifi-tether/issues/detail?id=264)** - **FIXED** see [r383](https://code.google.com/p/android-wifi-tether/source/detail?r=383)

`*` **Added** additional check for kernel-compatibility. See [r384](https://code.google.com/p/android-wifi-tether/source/detail?r=384).

### - Known issues ###
?

---


---

## wireless\_tether-2\_0-pre5 ##

### - Release notes ###
`*` **Added** (very experimental) support for Samsung Moment - Problem reported: cellular radio becomes unresponsive after an unpredictable interval.

`*` Failed message in log "Setting ad-hoc mode, channel and ssid.... failed" - Filed in **[issue 264](https://code.google.com/p/android-wifi-tether/issues/detail?id=264)** - hopefully **FIXED** (needs to be tested).

### - Known issues ###
`*` ESSID can't be changed - Filed in **[issue 264](https://code.google.com/p/android-wifi-tether/issues/detail?id=264)**

---


---

## wireless\_tether-2\_0-pre4 ##

### - Release notes ###
`*` **Added** support for older Android-versions (1.5/1.6) - pre4 supports now: HTC Dream/Magic/Hero, Samsung Galaxy, Google NexusOne, Motorola Droid/Milestone.

`*` Log (Show log) does not show failed commands - **FIXED**.

`*` Access-control does not update iptables-rules correctly - **FIXED**.

`*` Shared library not found if the .apk is pushed to /data/app (and not installed) - **FIXED**.

### - Known issues ###
`*`
`*` Failed message in log "Setting ad-hoc mode, channel and ssid.... failed" - Filed in **[issue 264](https://code.google.com/p/android-wifi-tether/issues/detail?id=264)**

---


---

## wireless\_tether-2\_0-pre3 ##

### - Release notes ###
`*` Fixed CPU Loading 99%. Filed in **[issue 237](https://code.google.com/p/android-wifi-tether/issues/detail?id=237)**.


### - Known issues ###
`*` Log (Show log) does not show failed commands

`*` Access-control does not update iptables-rules correctly.

---


---

## wireless\_tether-2\_0-pre2 ##

### - Release notes ###
`*` Support for Motorola Droid/Milestone

`*` Fixed **can't obtain ip-address**. Filed in **[issue 232](https://code.google.com/p/android-wifi-tether/issues/detail?id=232)**.

### - Known issues ###
`*`   	 CPU Loading 99%. Filed in **[issue 237](https://code.google.com/p/android-wifi-tether/issues/detail?id=237)**.

---


---

## wireless\_tether-2\_0-pre1 ##

### - Release notes ###
`*` Wep-encryption fixed.


### - Known issues ###
`*` Client **can't obtain ip-address**. Filed in **[issue 232](https://code.google.com/p/android-wifi-tether/issues/detail?id=232)**.

-> **Workaround:** change lan-network in settings

---


---
