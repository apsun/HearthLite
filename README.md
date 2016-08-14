# HearthLite

Automatically delete Hearthstone OBB files after updates.

**Note**: You still need enough space on your phone to hold the OBB
files when updating Hearthstone. This app only deletes the OBB files
after the update is complete.

## Requirements

- [Xposed framework](http://forum.xda-developers.com/xposed)
- [Hearthstone](https://play.google.com/store/apps/details?id=com.blizzard.wtcg.hearthstone)

## Why?

Hearthstone's massize app size on Android (1GB larger than the desktop
version) is due to the usage of [OBB files](https://developer.android.com/google/play/expansion-files.html).
These are similar to installers, in the sense that Hearthstone is updated
by downloading new OBB files and extracting their contents to your device's
storage.

The problem is that Hearthstone does not delete these OBB files after the
update is completed, causing them to needlessly take up space on your
device. Additionally, even if you manually delete these files, they will
automatically be downloaded again when opening the app.

This Xposed module automatically deletes the OBB files after updates, and
prevents them from being re-downloaded, saving around 1GB of storage on
your device.

## Is this safe?

In the sense that it won't blow up your phone, yes. In the sense that
you won't get banned for using it, most likely. All it does is hook the
app's update process and delete the leftover files after it's done. It
does not modify the actual game files in any way.
