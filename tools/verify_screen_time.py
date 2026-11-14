"""Checks TimeWise's screen time against Android's own numbers on a connected phone.

Reads `adb shell dumpsys usagestats`, replays the same counting rules as
UsageAggregator.sessions on the raw events, and prints them next to Android's per-app
totals for Android's current daily window, plus how long the screen was actually on.

Usage: python tools/verify_screen_time.py [adb_path] [serial]
"""
import re
import subprocess
import sys
from datetime import datetime

CLOSING = {"SCREEN_NON_INTERACTIVE", "KEYGUARD_SHOWN", "DEVICE_SHUTDOWN"}
EVENT_RE = re.compile(r'time="([^"]+)" type=(\S+) package=(\S+)(?: class=(\S+))?')
STATS_RE = re.compile(r'package=(\S+) totalTimeUsed="([^"]+)"')


def adb(adb_path, serial, *args):
    cmd = [adb_path] + (["-s", serial] if serial else []) + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace").stdout


def parse_clock(text):
    text = text.replace("\u202f", " ").replace("\u2009", " ").strip()
    return datetime.strptime(text, "%m/%d/%Y, %I:%M %p").timestamp()


def parse_duration(text):
    parts = [int(p) for p in text.split(":")]
    return parts[0] * 60 + parts[1] if len(parts) == 2 else parts[0] * 3600 + parts[1] * 60 + parts[2]


def parse(dump):
    """Events and Android's daily totals for the primary user."""
    lines = dump.splitlines()
    start = next(i for i, line in enumerate(lines) if line.startswith("user=0"))
    lines = lines[start:]
    daily = next(i for i, line in enumerate(lines) if "In-memory daily stats" in line)

    events = []
    for line in lines[:daily]:
        m = EVENT_RE.search(line)
        if m:
            t = datetime.strptime(m.group(1), "%Y-%m-%d %H:%M:%S").timestamp()
            events.append((t, m.group(2), m.group(3), m.group(4)))

    rng = re.search(r'timeRange="(.+? [AP]M)\s*\S\s*(.+? [AP]M)"',
                    lines[daily + 1].replace("\u202f", " ").replace("\u2009", " "))
    window = (parse_clock(rng.group(1)), parse_clock(rng.group(2)))

    android = {}
    for line in lines[daily + 2:]:
        if "In-memory weekly stats" in line:
            break
        m = STATS_RE.search(line)
        if m:
            android[m.group(1)] = parse_duration(m.group(2))
    return events, window, android


def timewise_sessions(events, ws, we, ignored):
    """Mirror of UsageAggregator.sessions: one app at a time, kept while any activity is resumed."""
    totals, cur, cur_start, resumed = {}, None, 0, set()

    def close(at):
        nonlocal cur
        if cur is not None:
            s, e = max(cur_start, ws), min(at, we)
            if e > s:
                totals[cur] = totals.get(cur, 0) + (e - s)
        cur = None
        resumed.clear()

    for t, typ, pkg, cls in events:
        if t > we:
            break
        if typ == "ACTIVITY_RESUMED":
            if pkg in ignored:
                continue
            if pkg != cur:
                close(t)
                cur, cur_start = pkg, t
            resumed.add(cls)
        elif typ in ("ACTIVITY_PAUSED", "ACTIVITY_STOPPED"):
            if pkg == cur:
                resumed.discard(cls)
                if not resumed:
                    close(t)
        elif typ in CLOSING:
            close(t)
    close(we)
    return totals


def screen_on_seconds(events, ws, we):
    total, on_at = 0, None
    for t, typ, *_ in events:
        if not ws <= t <= we:
            continue
        if typ == "SCREEN_INTERACTIVE" and on_at is None:
            on_at = t
        elif typ == "SCREEN_NON_INTERACTIVE" and on_at is not None:
            total, on_at = total + t - on_at, None
    return total + (we - on_at if on_at is not None else 0)


def launchers(adb_path, serial):
    """Mirror of ScreenTimeRepository.ignoredPackages."""
    def packages(*args):
        out = adb(adb_path, serial, "shell", "cmd", "package", *args)
        return {line.strip().split("/")[0] for line in out.splitlines() if "/" in line}

    home = ("-a", "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
    with_icon = packages("query-activities", "--brief", "-a", "android.intent.action.MAIN",
                         "-c", "android.intent.category.LAUNCHER")
    iconless_homes = packages("query-activities", "--brief", *home) - with_icon
    return iconless_homes | packages("resolve-activity", "--brief", *home) | {"com.android.systemui"}


def minutes(seconds):
    return f"{seconds / 60:7.1f}m"


def main():
    adb_path = sys.argv[1] if len(sys.argv) > 1 else "adb"
    serial = sys.argv[2] if len(sys.argv) > 2 else None
    events, (ws, we), android = parse(adb(adb_path, serial, "shell", "dumpsys", "usagestats"))
    ignored = launchers(adb_path, serial)
    ours = timewise_sessions(events, ws, we, ignored)

    print(f"Window (Android's daily bucket): {datetime.fromtimestamp(ws):%d %b %H:%M} -> {datetime.fromtimestamp(we):%d %b %H:%M}")
    print(f"Launchers ignored: {', '.join(sorted(ignored))}\n")
    print(f"{'App':45} {'TimeWise':>9} {'Android':>9}")
    apps = sorted({p for p in set(ours) | set(android) if p not in ignored},
                  key=lambda p: -max(ours.get(p, 0), android.get(p, 0)))
    for pkg in apps[:20]:
        if max(ours.get(pkg, 0), android.get(pkg, 0)) < 30:
            break
        print(f"{pkg:45} {minutes(ours.get(pkg, 0)):>9} {minutes(android.get(pkg, 0)):>9}")

    android_sum = sum(v for p, v in android.items() if p not in ignored)
    print(f"\n{'TimeWise total':45} {minutes(sum(ours.values())):>9}")
    print(f"{'Android per-app sum':45} {minutes(android_sum):>9}")
    print(f"{'Screen actually on':45} {minutes(screen_on_seconds(events, ws, we)):>9}")
    print("\nTimeWise counts one app at a time, so its total stays at or under screen-on time.")
    print("Android counts every visible app, so split-screen and floating windows are counted twice.")


if __name__ == "__main__":
    main()
