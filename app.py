import os
import shutil
import subprocess
import sys
import time

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
PACKAGE = "com.you.reelblocker"
LAUNCH_COMPONENT = f"{PACKAGE}/{PACKAGE}.MainActivity"
GRADLEW = os.path.join(PROJECT_DIR, "gradlew.bat" if os.name == "nt" else "gradlew")
DEBUG_APK = os.path.join(PROJECT_DIR, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
EXE = ".exe" if os.name == "nt" else ""
EMULATOR_BOOT_TIMEOUT = 300


def find_sdk_dir():
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var):
            return os.environ[var]

    local_props = os.path.join(PROJECT_DIR, "local.properties")
    if os.path.exists(local_props):
        with open(local_props, encoding="utf-8") as f:
            for line in f:
                if line.startswith("sdk.dir="):
                    return line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "\\")
    return None


def find_sdk_tool(subdir, name):
    sdk_dir = find_sdk_dir()
    if sdk_dir:
        path = os.path.join(sdk_dir, subdir, name + EXE)
        if os.path.exists(path):
            return path
    return shutil.which(name)


ADB = find_sdk_tool("platform-tools", "adb")
EMULATOR = find_sdk_tool("emulator", "emulator")


def run_command(command):
    print(f"\n> {' '.join(command)}")
    return subprocess.run(command, cwd=PROJECT_DIR).returncode == 0


def adb_output(*args):
    result = subprocess.run([ADB, *args], capture_output=True, text=True)
    return result.stdout.strip()


def list_devices():
    """Returns (serial, state) pairs, e.g. ("emulator-5554", "device")."""
    devices = []
    for line in adb_output("devices").splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2:
            devices.append((parts[0], parts[1]))
    return devices


def is_emulator(serial):
    return serial.startswith("emulator-")


def device_name(serial):
    if is_emulator(serial):
        avd = adb_output("-s", serial, "emu", "avd", "name").splitlines()
        return f"{avd[0]} (emulator)" if avd else "Emulator"
    model = adb_output("-s", serial, "shell", "getprop", "ro.product.model")
    return model or serial


def choose(items, label):
    if len(items) == 1:
        return items[0]

    print(f"\nSelect {label}:")
    for i, (_, name) in enumerate(items, start=1):
        print(f"{i}. {name}")
    choice = input("Choose a number: ").strip()
    if choice.isdigit() and 1 <= int(choice) <= len(items):
        return items[int(choice) - 1]
    print("Invalid selection.")
    return None


def require_adb():
    if ADB is None:
        print("adb was not found. Install Android SDK platform-tools from Android Studio's SDK Manager.")
        return False
    return True


def build_debug_apk():
    if not run_command([GRADLEW, "assembleDebug"]):
        print("Build failed.")
        return False
    print(f"Debug APK: {DEBUG_APK}")
    return True


def install_and_launch(serial):
    if not build_debug_apk():
        return
    if not run_command([ADB, "-s", serial, "install", "-r", DEBUG_APK]):
        print("Install failed.")
        return
    if run_command([ADB, "-s", serial, "shell", "am", "start", "-n", LAUNCH_COMPONENT]):
        print("App launched.")
    else:
        print("Failed to launch the app.")


def wait_for_boot(serial):
    print(f"Waiting for {serial} to finish booting...")
    deadline = time.time() + EMULATOR_BOOT_TIMEOUT
    while time.time() < deadline:
        if adb_output("-s", serial, "shell", "getprop", "sys.boot_completed") == "1":
            print("Emulator is ready.")
            return True
        time.sleep(3)
    print("Timed out waiting for the emulator to boot.")
    return False


def start_emulator():
    if EMULATOR is None:
        print("Android emulator was not found. Install it from Android Studio's SDK Manager.")
        return None

    avds = subprocess.run([EMULATOR, "-list-avds"], capture_output=True, text=True).stdout.split()
    if not avds:
        print("No emulators (AVDs) found. Create one in Android Studio > Device Manager.")
        return None

    selected = choose([(avd, avd) for avd in avds], "an emulator to start")
    if selected is None:
        return None
    avd = selected[0]

    existing = {serial for serial, _ in list_devices()}
    print(f"Starting emulator '{avd}'...")
    flags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
    subprocess.Popen(
        [EMULATOR, "-avd", avd],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        creationflags=flags,
        start_new_session=os.name != "nt",
    )

    deadline = time.time() + EMULATOR_BOOT_TIMEOUT
    while time.time() < deadline:
        new = [s for s, _ in list_devices() if is_emulator(s) and s not in existing]
        if new:
            return new[0] if wait_for_boot(new[0]) else None
        time.sleep(2)
    print("Timed out waiting for the emulator to appear in adb.")
    return None


def run_on_emulator():
    if not require_adb():
        return

    emulators = [s for s, state in list_devices() if is_emulator(s) and state == "device"]
    if emulators:
        selected = choose([(s, device_name(s)) for s in emulators], "an emulator")
        serial = selected[0] if selected else None
    else:
        print("No running emulator found.")
        serial = start_emulator()

    if serial:
        install_and_launch(serial)


def run_on_physical_device():
    if not require_adb():
        return

    devices = [(s, state) for s, state in list_devices() if not is_emulator(s)]
    unauthorized = [s for s, state in devices if state == "unauthorized"]
    ready = [s for s, state in devices if state == "device"]

    if unauthorized:
        print("Some devices are unauthorized. Unlock the phone and accept the 'Allow USB debugging' prompt.")
    if not ready:
        print("No connected mobile device found.")
        print("Enable Developer options > USB debugging on the phone and connect it with a USB cable.")
        return

    selected = choose([(s, device_name(s)) for s in ready], "a device")
    if selected:
        install_and_launch(selected[0])


def show_connected_devices():
    if not require_adb():
        return
    devices = list_devices()
    if not devices:
        print("No devices or emulators connected.")
        return
    print("\nConnected devices:")
    for serial, state in devices:
        name = device_name(serial) if state == "device" else state
        print(f"- {serial}: {name}")


def pick_ready_device():
    ready = [s for s, state in list_devices() if state == "device"]
    if not ready:
        print("No devices or emulators connected.")
        return None
    selected = choose([(s, device_name(s)) for s in ready], "a device")
    return selected[0] if selected else None


def show_app_logs():
    if not require_adb():
        return
    serial = pick_ready_device()
    if not serial:
        return

    pid = adb_output("-s", serial, "shell", "pidof", PACKAGE)
    if not pid:
        print("The app is not running on this device. Launch it first.")
        return

    print("Showing app logs. Press Ctrl+C to return to the menu.")
    try:
        subprocess.run([ADB, "-s", serial, "logcat", f"--pid={pid.split()[0]}"])
    except KeyboardInterrupt:
        print("\nStopped logs.")


def uninstall_app():
    if not require_adb():
        return
    serial = pick_ready_device()
    if serial and run_command([ADB, "-s", serial, "uninstall", PACKAGE]):
        print("App uninstalled.")


MENU = [
    ("1", "Run the app in Emulator", run_on_emulator),
    ("2", "Run the app on connected mobile device (USB / adb)", run_on_physical_device),
    ("3", "Show connected devices", show_connected_devices),
    ("4", "Build debug APK only", build_debug_apk),
    ("5", "Show app logs", show_app_logs),
    ("6", "Uninstall the app from a device", uninstall_app),
    ("0", "Exit", None),
]


def main():
    actions = {key: action for key, _, action in MENU}
    while True:
        print("\n=== Reel Blocker ===")
        for key, label, _ in MENU:
            print(f"{key}. {label}")

        try:
            choice = input("Choose an option: ").strip()
        except (KeyboardInterrupt, EOFError):
            choice = "0"

        if choice == "0":
            print("\nGoodbye!")
            sys.exit(0)
        elif choice in actions:
            try:
                actions[choice]()
            except KeyboardInterrupt:
                print("\nCancelled.")
        else:
            print("Invalid option. Please choose from the menu.")


if __name__ == "__main__":
    main()
