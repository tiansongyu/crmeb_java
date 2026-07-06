#!/usr/bin/env python3
import base64
import json
import os
import queue
import shutil
import subprocess
import tempfile
import threading
import time
import urllib.request

import requests
import websocket


BASE_URL = os.environ.get("BASE_URL", "http://127.0.0.1:8082").rstrip("/")
API_URL = os.environ.get("API_URL", "http://127.0.0.1:20410").rstrip("/")
FRONT_ACCOUNT = os.environ.get("FRONT_ACCOUNT")
FRONT_PASSWORD = os.environ.get("FRONT_PASSWORD")
COMBINATION_ID = os.environ.get("COMBINATION_ID")
TARGET_SKU = os.environ.get("TARGET_SKU")
SCREENSHOT = os.environ.get("SCREENSHOT", "artifacts/combination-sku-popup-smoke.png")
DETAIL_SCREENSHOT = os.environ.get(
    "DETAIL_SCREENSHOT",
    "artifacts/combination-detail-limit-smoke.png",
)
CONFIRM_SCREENSHOT = os.environ.get(
    "CONFIRM_SCREENSHOT",
    "artifacts/combination-sku-popup-confirm-smoke.png",
)
EXPECTED_LIMIT_TEXT = os.environ.get("EXPECTED_LIMIT_TEXT")


class Cdp:
    def __init__(self, websocket_url, origin):
        self.websocket = websocket.create_connection(websocket_url, timeout=10, origin=origin)
        self.next_id = 1
        self.pending = {}
        self.lock = threading.Lock()
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self):
        while True:
            try:
                message = json.loads(self.websocket.recv())
            except Exception as exc:
                with self.lock:
                    for pending in self.pending.values():
                        pending.put({"error": str(exc)})
                return

            if "id" not in message:
                continue
            with self.lock:
                pending = self.pending.get(message["id"])
            if pending:
                pending.put(message)

    def call(self, method, params=None, timeout=10):
        with self.lock:
            message_id = self.next_id
            self.next_id += 1
            pending = queue.Queue(maxsize=1)
            self.pending[message_id] = pending
        self.websocket.send(json.dumps({"id": message_id, "method": method, "params": params or {}}))
        try:
            message = pending.get(timeout=timeout)
        finally:
            with self.lock:
                self.pending.pop(message_id, None)
        if "error" in message:
            raise RuntimeError("{} failed: {}".format(method, message["error"]))
        return message.get("result") or {}

    def eval(self, expression, timeout=10):
        result = self.call(
            "Runtime.evaluate",
            {"expression": expression, "returnByValue": True},
            timeout=timeout,
        )
        if "exceptionDetails" in result:
            raise RuntimeError(json.dumps(result["exceptionDetails"], ensure_ascii=False))
        return (result.get("result") or {}).get("value")

    def wait(self, expression, timeout=20):
        deadline = time.time() + timeout
        last = None
        while time.time() < deadline:
            try:
                last = self.eval(expression, timeout=3)
                if last:
                    return last
            except Exception as exc:
                last = str(exc)
            time.sleep(0.25)
        raise RuntimeError("Timed out waiting for {}. Last value: {}".format(expression, last))

    def tap(self, x, y):
        point = {"x": x, "y": y, "radiusX": 1, "radiusY": 1, "force": 1}
        self.call("Input.dispatchTouchEvent", {"type": "touchStart", "touchPoints": [point]})
        self.call("Input.dispatchTouchEvent", {"type": "touchEnd", "touchPoints": []})


def login():
    response = requests.post(
        API_URL + "/api/front/login",
        json={"account": FRONT_ACCOUNT, "password": FRONT_PASSWORD, "spread_spid": 0},
        timeout=20,
    )
    response.raise_for_status()
    payload = response.json()
    token = ((payload.get("data") or {}).get("token"))
    if payload.get("code") != 200 or not token:
        raise RuntimeError("Login failed: {}".format(json.dumps(payload, ensure_ascii=False)))
    return token


def get_page_websocket(port):
    deadline = time.time() + 15
    last = None
    while time.time() < deadline:
        try:
            with urllib.request.urlopen("http://127.0.0.1:{}/json/list".format(port), timeout=1) as response:
                targets = json.load(response)
            for target in targets:
                if target.get("type") == "page" and target.get("webSocketDebuggerUrl"):
                    return target["webSocketDebuggerUrl"]
        except Exception as exc:
            last = exc
        time.sleep(0.2)
    raise RuntimeError("Chrome page target unavailable: {}".format(last))


def main():
    missing = [
        name
        for name, value in {
            "FRONT_ACCOUNT": FRONT_ACCOUNT,
            "FRONT_PASSWORD": FRONT_PASSWORD,
            "COMBINATION_ID": COMBINATION_ID,
            "TARGET_SKU": TARGET_SKU,
            "EXPECTED_LIMIT_TEXT": EXPECTED_LIMIT_TEXT,
        }.items()
        if not value
    ]
    if missing:
        raise SystemExit("{} must be set for template-store popup smoke tests".format(", ".join(missing)))
    token = login()
    port = int(os.environ.get("CHROME_PORT", "9340"))
    profile = tempfile.mkdtemp(prefix="template-popup-smoke-")
    chrome = None
    try:
        chrome = subprocess.Popen(
            [
                "google-chrome-stable",
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--remote-allow-origins=*",
                "--remote-debugging-port={}".format(port),
                "--user-data-dir={}".format(profile),
                "--window-size=390,844",
                "about:blank",
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        cdp = Cdp(get_page_websocket(port), "http://127.0.0.1:{}".format(port))
        cdp.call("Page.enable")
        cdp.call("Runtime.enable")
        cdp.call(
            "Emulation.setUserAgentOverride",
            {
                "userAgent": (
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) "
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
                )
            },
        )
        cdp.call(
            "Emulation.setDeviceMetricsOverride",
            {"width": 390, "height": 844, "deviceScaleFactor": 3, "mobile": True},
        )
        cdp.call("Emulation.setTouchEmulationEnabled", {"enabled": True, "maxTouchPoints": 1})

        cdp.call("Page.navigate", {"url": BASE_URL + "/?popup_smoke_login={}".format(int(time.time()))})
        cdp.wait('document.readyState === "complete"', timeout=15)
        expires = int(time.time()) + 86400
        cdp.eval(
            "localStorage.setItem('LOGIN_STATUS_TOKEN', {});"
            "localStorage.setItem('EXPIRES_TIME', '{}');"
            "if (window.uni) {{"
            "uni.setStorageSync('LOGIN_STATUS_TOKEN', {});"
            "uni.setStorageSync('EXPIRES_TIME', '{}');"
            "}}".format(json.dumps(token), expires, json.dumps(token), expires)
        )

        cdp.call(
            "Page.navigate",
            {
                "url": "{}/pages/activity/goods_combination_details/index?id={}&popup_smoke={}".format(
                    BASE_URL, COMBINATION_ID, int(time.time())
                )
            },
        )
        cdp.wait('document.body.innerText.includes("立即开团")', timeout=30)
        detail_limit = cdp.wait(
            """
            (() => {
              const text = document.body.innerText || '';
              const match = text.match(/限[购量]:\\s*\\d+\\s*件/);
              if (!match) return '';
              return match[0].replace(/\\s+/g, ' ');
            })()
            """,
            timeout=10,
        )
        if detail_limit != EXPECTED_LIMIT_TEXT:
            raise SystemExit(
                "Unexpected detail limit text: {!r}, expected {!r}".format(
                    detail_limit, EXPECTED_LIMIT_TEXT
                )
            )
        os.makedirs(os.path.dirname(DETAIL_SCREENSHOT), exist_ok=True)
        detail_screenshot = cdp.call("Page.captureScreenshot", {"format": "png", "captureBeyondViewport": False}, timeout=15)
        with open(DETAIL_SCREENSHOT, "wb") as output:
            output.write(base64.b64decode(detail_screenshot["data"]))
        click_result = cdp.eval(
            """
            (() => {
              const visible = el => {
                const style = getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                return style.display !== 'none' && style.visibility !== 'hidden' &&
                  rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < innerHeight;
              };
              const nodes = [...document.querySelectorAll('*')]
                .filter(el => visible(el) && (el.innerText || '').trim() === '立即开团');
              const el = nodes[nodes.length - 1];
              if (!el) return {clicked: false, count: nodes.length};
              const rect = el.getBoundingClientRect();
              el.click();
              return {clicked: true, count: nodes.length, top: rect.top, bottom: rect.bottom};
            })()
            """
        )
        if not click_result.get("clicked"):
            raise RuntimeError("Could not click open-group button: {}".format(click_result))

        cdp.wait("document.querySelector('.product-window.on') !== null", timeout=8)
        time.sleep(0.4)
        popup_state = cdp.eval(
            """
            (() => {
              const root = document.querySelector('.product-window.on');
              const visible = el => {
                const style = getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                return style.display !== 'none' && style.visibility !== 'hidden' &&
                  rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < innerHeight;
              };
              const itemNodes = root ? [...root.querySelectorAll('.itemn')] : [];
              const visibleItems = itemNodes.filter(visible);
              const confirmButtons = root ? [...root.querySelectorAll('.joinBnt')]
                .filter(visible)
                .map(el => (el.innerText || '').trim()) : [];
              const popupRect = root ? root.getBoundingClientRect() : null;
              return {
                popupRect: popupRect ? {
                  top: popupRect.top,
                  bottom: popupRect.bottom,
                  height: popupRect.height
                } : null,
                totalSkuItems: itemNodes.length,
                visibleSkuItems: visibleItems.length,
                confirmButtons,
                firstSkuRect: itemNodes[0] ? {
                  top: itemNodes[0].getBoundingClientRect().top,
                  bottom: itemNodes[0].getBoundingClientRect().bottom
                } : null,
                cartVisible: !!root && visible(root.querySelector('.cart')),
                selected: [...document.querySelectorAll('.product-window.on .itemn.on')]
                  .map(el => el.innerText.trim())
              };
            })()
            """
        )

        os.makedirs(os.path.dirname(SCREENSHOT), exist_ok=True)
        screenshot = cdp.call("Page.captureScreenshot", {"format": "png", "captureBeyondViewport": False}, timeout=15)
        with open(SCREENSHOT, "wb") as output:
            output.write(base64.b64decode(screenshot["data"]))

        print(
            json.dumps(
                {
                    "detailLimit": detail_limit,
                    "detailScreenshot": DETAIL_SCREENSHOT,
                    "click": click_result,
                    "popup": popup_state,
                    "screenshot": SCREENSHOT,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
        if popup_state.get("visibleSkuItems", 0) < 1:
            raise SystemExit("No SKU option is visible after opening the group-buy popup")
        if "立即开团" not in popup_state.get("confirmButtons", []):
            raise SystemExit("No in-popup open-group confirmation button is visible")

        select_target = cdp.eval(
            """
            (() => {
              const root = document.querySelector('.product-window.on');
              const nodes = root ? [...root.querySelectorAll('.itemn')]
                .filter(el => (el.innerText || '').trim() === %s) : [];
              const el = nodes[0];
              if (!el) return {found: false, count: nodes.length};
              el.scrollIntoView({block: 'center'});
              const rect = el.getBoundingClientRect();
              return {
                found: true,
                count: nodes.length,
                x: rect.left + rect.width / 2,
                y: rect.top + rect.height / 2,
                top: rect.top,
                bottom: rect.bottom,
                text: el.innerText.trim()
              };
            })()
            """
            % json.dumps(TARGET_SKU)
        )
        if not select_target.get("found"):
            raise SystemExit("Target SKU option was not found in popup: {}".format(TARGET_SKU))
        cdp.tap(select_target["x"], select_target["y"])
        cdp.wait(
            "[...document.querySelectorAll('.product-window.on .itemn.on')]"
            ".some(el => el.innerText.trim() === {})".format(json.dumps(TARGET_SKU)),
            timeout=8,
        )
        select_result = dict(select_target, tapped=True, input="touch")
        confirm_result = cdp.eval(
            """
            (() => {
              const root = document.querySelector('.product-window.on');
              const visible = el => {
                const style = getComputedStyle(el);
                const rect = el.getBoundingClientRect();
                return style.display !== 'none' && style.visibility !== 'hidden' &&
                  rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < innerHeight;
              };
              const nodes = root ? [...root.querySelectorAll('.joinBnt')]
                .filter(el => visible(el) && (el.innerText || '').trim() === '立即开团') : [];
              const el = nodes[0];
              if (!el) return {clicked: false, count: nodes.length};
              const rect = el.getBoundingClientRect();
              el.click();
              return {clicked: true, count: nodes.length, top: rect.top, bottom: rect.bottom};
            })()
            """
        )
        if not confirm_result.get("clicked"):
            raise SystemExit("Could not click in-popup open-group confirmation button")
        cdp.wait("location.href.includes('/pages/order/order_confirm/index?preOrderNo=')", timeout=30)
        cdp.wait("document.body.innerText.includes({})".format(json.dumps(TARGET_SKU)), timeout=20)
        cdp.wait('document.body.innerText.includes("提交订单")', timeout=20)

        confirm_screenshot = cdp.call("Page.captureScreenshot", {"format": "png", "captureBeyondViewport": False}, timeout=15)
        with open(CONFIRM_SCREENSHOT, "wb") as output:
            output.write(base64.b64decode(confirm_screenshot["data"]))
        print(
            json.dumps(
                {
                    "select": select_result,
                    "confirm": confirm_result,
                    "confirmUrl": cdp.eval("location.href"),
                    "confirmScreenshot": CONFIRM_SCREENSHOT,
                },
                ensure_ascii=False,
                indent=2,
            )
        )
    finally:
        if chrome:
            chrome.terminate()
            try:
                chrome.wait(timeout=5)
            except subprocess.TimeoutExpired:
                chrome.kill()
        shutil.rmtree(profile, ignore_errors=True)


if __name__ == "__main__":
    main()
