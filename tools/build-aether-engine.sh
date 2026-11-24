#!/usr/bin/env bash
# Build the merged QW-patched Aether engine (based on Cluvex upstream) for ZX Android ABIs.
# This is additive: no existing ZX engine or source is replaced.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AETHER_ROOT="$ROOT/native/aether-qw/native/aether"
SRC="$AETHER_ROOT/aether"
PATCHER="$ROOT/native/aether-qw/scripts/patch-engine-dependencies.py"
JNI="$ROOT/app/src/main/jniLibs"
API="${ANDROID_API:-24}"
if [ -z "${ANDROID_NDK_HOME:-}" ]; then
  NDK_ROOT="${ANDROID_HOME:-$HOME/Android/Sdk}/ndk"
  ANDROID_NDK_HOME="$(find "$NDK_ROOT" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -n1)"
fi
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
if [ ! -f "$SRC/Cargo.toml" ] || [ ! -f "$PATCHER" ]; then
  echo "Merged Aether source or dependency patcher missing; initialize submodules recursively." >&2
  exit 1
fi
UPSTREAM_SRC="$ROOT/native/aether/aether"
if [ ! -f "$UPSTREAM_SRC/Cargo.toml" ]; then
  echo "Cluvex upstream Aether submodule is missing." >&2
  exit 1
fi
UPSTREAM_VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' "$UPSTREAM_SRC/Cargo.toml" | head -n1)"
MERGED_VERSION="$(sed -n 's/^version = "\(.*\)"/\1/p' "$SRC/Cargo.toml" | head -n1)"
if [ -z "$UPSTREAM_VERSION" ] || [ "$UPSTREAM_VERSION" != "$MERGED_VERSION" ]; then
  echo "Aether core versions differ (Cluvex=$UPSTREAM_VERSION, QW=$MERGED_VERSION); review the merge before building." >&2
  exit 1
fi
echo "==> Cluvex upstream and QW patched Aether core versions match: $MERGED_VERSION"
if [ ! -f "$ROOT/native/aether-qw/PATCHLEVEL" ]; then
  echo "Merged Aether PATCHLEVEL is missing; refusing an unidentifiable engine build." >&2
  exit 1
fi
APP_PATCHLEVEL="$(tr -d '[:space:]' < "$ROOT/native/aether-qw/PATCHLEVEL")"
[ -n "$APP_PATCHLEVEL" ] || { echo "Aether PATCHLEVEL is empty" >&2; exit 1; }
export APP_PATCHLEVEL
export CARGO_TARGET_DIR="$AETHER_ROOT/target"
echo "==> Building merged Aether core at patch level $APP_PATCHLEVEL"
python3 "$PATCHER" "$SRC"
if [ -z "$NDK" ] || [ ! -d "$NDK/toolchains/llvm/prebuilt/linux-x86_64" ]; then
  echo "Linux Android NDK not found. Set ANDROID_NDK_HOME to the NDK root." >&2; exit 1
fi
export ANDROID_NDK_HOME="$NDK" ANDROID_NDK_ROOT="$NDK"
command -v cargo-ndk >/dev/null || { echo "cargo-ndk is required" >&2; exit 1; }
build_one() {
  local abi="$1" triple="$2"
  echo "==> Building pinned Aether for $abi ($triple)"
  (cd "$SRC" && cargo ndk -t "$abi" --platform "$API" build --bin aether --release --locked --features tor)
  local bin="$CARGO_TARGET_DIR/$triple/release/aether"
  [ -x "$bin" ] || { echo "Aether executable missing: $bin" >&2; find "$CARGO_TARGET_DIR" -maxdepth 5 -type f -name aether -print >&2 || true; exit 1; }
  mkdir -p "$JNI/$abi"
  install -m 755 "$bin" "$JNI/$abi/libaether.so"
  local sysroot_triple="$triple"
  if [ "$abi" = "armeabi-v7a" ]; then sysroot_triple="arm-linux-androideabi"; fi
  local syslib_dir="$NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/$sysroot_triple"
  local syslib="$syslib_dir/libc++_shared.so"
  if [ ! -f "$syslib" ] && [ -d "$syslib_dir" ]; then
    syslib="$(find "$syslib_dir" -name libc++_shared.so -type f | sort -V | tail -n1)"
  fi
  if [ -f "$syslib" ]; then
    install -m 644 "$syslib" "$JNI/$abi/libc++_shared.so"
  else
    echo "NDK libc++_shared.so not found for $abi ($sysroot_triple)" >&2
    exit 1
  fi

  # Strip and verify every dynamic dependency before the APK is assembled.
  local llvm_bin="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
  "$llvm_bin/llvm-strip" "$JNI/$abi/libaether.so"
  local needed
  needed="$("$llvm_bin/llvm-readelf" -d "$JNI/$abi/libaether.so" 2>/dev/null | sed -n 's/.*Shared library: \[\([^]]*\)\].*/\1/p')"
  if [ -z "$needed" ]; then
    echo "Could not read dynamic dependencies from $JNI/$abi/libaether.so" >&2
    exit 1
  fi
  while IFS= read -r dep; do
    [ -n "$dep" ] || continue
    case "$dep" in
      libc.so|libdl.so|liblog.so|libm.so|libandroid.so|libz.so) ;;
      libc++_shared.so)
        [ -f "$JNI/$abi/libc++_shared.so" ] || {
          echo "Missing libc++_shared.so for $abi" >&2; exit 1;
        }
        ;;
      *)
        if [ ! -f "$JNI/$abi/$dep" ]; then
          echo "Unpackaged Aether runtime dependency for $abi: $dep" >&2
          exit 1
        fi
        ;;
    esac
  done <<< "$needed"
  local stamp="AETHER-BUILD-STAMP:$APP_PATCHLEVEL"
  if ! grep -aF -- "$stamp" "$JNI/$abi/libaether.so" >/dev/null; then
    echo "Aether binary for $abi is missing build stamp $stamp" >&2
    exit 1
  fi
  echo "    verified merged Aether build stamp: $stamp"
  echo "    packaged and dependency-verified $JNI/$abi/libaether.so"
}
build_one arm64-v8a aarch64-linux-android
build_one armeabi-v7a armv7-linux-androideabi
build_one x86_64 x86_64-linux-android

# Aether's built-in Psiphon-only fallback launches the Psiphon console client
# as a child process. Package the Shir o Khorshid-pinned client beside each
# ABI's Aether executable; Kotlin passes its absolute path through
# AETHER_PSIPHON_BIN. Tor is built into libaether.so with --features tor.
build_psiphon() {
  local abi="$1" goarch="$2" goarm="${3:-}"
  local out="$CARGO_TARGET_DIR/psiphon-$abi"
  local licenses="$ROOT/app/src/main/assets/licenses"
  rm -rf "$out"
  mkdir -p "$out" "$licenses" "$JNI/$abi"
  [ -f "$AETHER_ROOT/psiphon-build.sh" ] || {
    echo "Pinned Aether Psiphon build script is missing: $AETHER_ROOT/psiphon-build.sh" >&2
    exit 1
  }
  # Go 1.23+ rejects anet's intentional net.zoneCache linkname unless
  # the linker check is explicitly disabled for this pinned Android client.
  # Patch a temporary copy so the upstream submodule stays untouched.
  local psiphon_build="$out/psiphon-build.sh"
  if ! grep -Fq 'go build -trimpath -ldflags "-s -w"' "$AETHER_ROOT/psiphon-build.sh"; then
    echo "Pinned Psiphon build script changed; review its linker flags before patching." >&2
    exit 1
  fi
  sed \
    -e 's/go build -trimpath -ldflags "-s -w"/go build -trimpath -ldflags "-s -w -checklinkname=0"/' \
    -e 's/CGO_ENABLED=0 GOFLAGS=/CGO_ENABLED="${CGO_ENABLED:-0}" GOFLAGS=/' \
    "$AETHER_ROOT/psiphon-build.sh" > "$psiphon_build"

  local clang=""
  case "$abi" in
    armeabi-v7a)
      # Go's android/arm port requires cgo and external linking.
      clang="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi${API}-clang"
      ;;
    x86_64)
      # Go's android/amd64 port also requires cgo and external linking.
      clang="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android${API}-clang"
      ;;
  esac
  if [ -n "$clang" ]; then
    [ -x "$clang" ] || {
      echo "Android NDK C compiler not found for $abi: $clang" >&2
      exit 1
    }
    CGO_ENABLED=1 CC="$clang" bash "$psiphon_build" android "$goarch" "$out" "$goarm" "$licenses"
  else
    # arm64 uses Go's supported pure-Go Android build path here.
    bash "$psiphon_build" android "$goarch" "$out" "$goarm" "$licenses"
  fi
  [ -s "$out/psiphon-tunnel-core" ] || {
    echo "Psiphon Android executable was not produced for $abi" >&2
    exit 1
  }
  install -m 755 "$out/psiphon-tunnel-core" "$JNI/$abi/libpsiphon-tunnel-core.so"
  echo "    packaged Aether's built-in Psiphon fallback for $abi"
}

build_psiphon arm64-v8a arm64
build_psiphon armeabi-v7a arm 7
build_psiphon x86_64 amd64
