"""Quick test to check if Discord RPC pipe is reachable."""

import struct, json, os, time

CLIENT_ID = "1491748892327542956"

def send(pipe, op, data):
    payload = json.dumps(data).encode()
    pipe.write(struct.pack("<II", op, len(payload)) + payload)
    pipe.flush()

def recv(pipe):
    raw = pipe.read(8)
    op, length = struct.unpack("<II", raw)
    body = pipe.read(length)
    return op, json.loads(body)

pipe = None
for i in range(10):
    path = f"\\\\.\\pipe\\discord-ipc-{i}"
    try:
        pipe = open(path, "r+b", buffering=0)
        print(f"[ok] Connected to {path}")
        break
    except Exception as e:
        print(f"[miss] {path}: {e}")

if pipe is None:
    print("\nERROR: Could not connect to Discord. Make sure Discord is open.")
else:
    send(pipe, 0, {"v": 1, "client_id": CLIENT_ID})
    op, resp = recv(pipe)
    print(f"\nHandshake response (op={op}):")
    print(json.dumps(resp, indent=2))

    if resp.get("evt") == "READY":
        print("\n[ok] RPC is working! Setting test activity...")
        send(pipe, 1, {
            "cmd": "SET_ACTIVITY",
            "args": {
                "pid": os.getpid(),
                "activity": {
                    "details": "Playing SlothScape",
                    "state": "via RSPS Hub",
                    "timestamps": {"start": int(time.time())},
                    "assets": {"large_image": "logo", "large_text": "RSPS Hub"}
                }
            },
            "nonce": "test"
        })
        op2, resp2 = recv(pipe)
        print("Activity response:", json.dumps(resp2, indent=2))
        print("\nCheck your Discord profile now — you should see the activity.")
        input("Press Enter to clear and exit...")
        send(pipe, 1, {"cmd": "SET_ACTIVITY", "args": {"pid": os.getpid(), "activity": None}, "nonce": "clear"})
    else:
        print("\nERROR: Handshake failed. Check the CLIENT_ID is correct.")

    pipe.close()
