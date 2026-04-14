#!/system/bin/sh
#######################################################################################
# APatch Boot Image Patcher
#######################################################################################
#
# Usage: boot_patch.sh <key1> <bootimage> [flash_to_device] [single|dual] [key2] [ARGS_PASS_TO_KPTOOLS]
#
# This script should be placed in a directory with the following files:
#
# File name          Type          Description
#
# boot_patch.sh      script        A script to patch boot image for APatch.
#                  (this file)      The script will use files in its same
#                                  directory to complete the patching process.
# bootimg            binary        The target boot image
# kpimg              binary        KernelPatch core Image
# kptools            executable    The KernelPatch tools binary to inject kpimg to kernel Image
#
#######################################################################################

ARCH=$(getprop ro.product.cpu.abi)

# Load utility functions
. ./util_functions.sh

echo "****************************"
echo " APatch Boot Image Patcher"
echo "****************************"

SUPERKEY="$1"
BOOTIMAGE=$2
FLASH_TO_DEVICE=$3
KEY_MODE=$4
KEY2=$5
shift 2

if [ "$KEY_MODE" = "single" ] || [ "$KEY_MODE" = "dual" ]; then
  shift 3
else
  KEY_MODE="single"
  KEY2=""
fi

[ -z "$SUPERKEY" ] && { >&2 echo "- SuperKey empty!"; exit 1; }
[ -e "$BOOTIMAGE" ] || { >&2 echo "- $BOOTIMAGE does not exist!"; exit 1; }

# Check for dependencies

command -v ./kptools >/dev/null 2>&1 || { >&2 echo "- Command kptools not found!"; exit 1; }
[ -x ./magiskboot ] || { >&2 echo "- Command magiskboot not found!"; exit 1; }
chmod +x ./kptools

if [ ! -f kernel ]; then
echo "- Unpacking boot image"

set -x
./magiskboot unpack "$BOOTIMAGE"
patch_rc=$?
set +x
  if [ $patch_rc -ne 0 ] || [ ! -f kernel ]; then
    >&2 echo "- Unpack error: $patch_rc"
    exit $patch_rc
  fi
fi

KFLAG_OUTPUT="$(./kptools -i kernel -f 2>&1 || true)"
if echo "$KFLAG_OUTPUT" | grep -q "zlib support disabled"; then
  echo "- kptools built without zlib support, skip CONFIG_KALLSYMS precheck."
elif ! echo "$KFLAG_OUTPUT" | grep -q "CONFIG_KALLSYMS=y"; then
  echo "- Patcher has Aborted!"
  echo "- APatch requires CONFIG_KALLSYMS to be Enabled."
  echo "- But your kernel seems NOT enabled it."
  exit 1
fi

if [  $(./kptools -i kernel -l | grep patched=false) ]; then
	echo "- Backing boot.img "
  cp "$BOOTIMAGE" "ori.img" >/dev/null 2>&1
fi

mv kernel kernel.ori

echo "- Patching kernel"

set -x
if [ "$KEY_MODE" = "dual" ]; then
  [ -z "$KEY2" ] && { >&2 echo "- key2 empty in dual mode!"; exit 1; }
  ./kptools -p -i kernel.ori -R "$SUPERKEY" -s "$KEY2" -k kpimg -o kernel "$@"
else
  ./kptools -p -i kernel.ori -s "$SUPERKEY" -k kpimg -o kernel "$@"
fi
patch_rc=$?
set +x

if [ $patch_rc -ne 0 ]; then
  >&2 echo "- Patch kernel error: $patch_rc"
  exit $?
fi

echo "- Repacking boot image"
set -x
./magiskboot repack "$BOOTIMAGE" new-boot.img
repack_rc=$?
if [ $repack_rc -ne 0 ]; then
  ./magiskboot repack "$BOOTIMAGE"
  repack_rc=$?
fi
set +x

KFLAG_ORI_OUTPUT="$(./kptools -i kernel.ori -f 2>&1 || true)"
if echo "$KFLAG_ORI_OUTPUT" | grep -q "zlib support disabled"; then
  echo "- kptools built without zlib support, skip CONFIG_KALLSYMS_ALL check."
elif ! echo "$KFLAG_ORI_OUTPUT" | grep -q "CONFIG_KALLSYMS_ALL=y"; then
  echo "- Detected CONFIG_KALLSYMS_ALL is not set!"
  echo "- APatch has patched but maybe your device won't boot."
  echo "- Make sure you have original boot image backup."
fi

if [ $repack_rc -ne 0 ] || [ ! -s new-boot.img ]; then
  >&2 echo "- Repack error: $repack_rc"
  exit $repack_rc
fi

if [ "$FLASH_TO_DEVICE" = "true" ]; then
  # flash
  if [ ! -s "new-boot.img" ]; then
    >&2 echo "- Flash skipped: new-boot.img missing"
    exit 1
  fi

  if [ -b "$BOOTIMAGE" ] || [ -c "$BOOTIMAGE" ]; then
    echo "- Flashing new boot image"
    flash_image new-boot.img "$BOOTIMAGE"
    if [ $? -ne 0 ]; then
      >&2 echo "- Flash error: $?"
      exit $?
    fi
  else
    >&2 echo "- Flash target is not block/char device: $BOOTIMAGE"
    exit 1
  fi

  echo "- Successfully Flashed!"
else
  echo "- Successfully Patched!"
fi

