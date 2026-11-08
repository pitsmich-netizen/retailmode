#!/usr/bin/env python3
"""Generuje QR Android Enterprise provisioning dla RETAIL DPC TEST.

Przykład:
 python tools/generate_provisioning_qr.py \
    --wrapper-apk /sciezka/app-debug.apk \
    --wrapper-url https://github.com/USER/REPO/releases/download/dpc-test/RetailDpc.apk

Hasło demo jest niezależne od hasła Wi-Fi. Ten projekt nie automatyzuje kreatora demo.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
from urllib.parse import urlparse

try:
    import qrcode
except ImportError as e:
    raise SystemExit('Zainstaluj bibliotekę: python -m pip install qrcode[pil]') from e

PACKAGE = 'com.example.retaildpc'
ADMIN = f'{PACKAGE}/.AdminReceiver'


def build_payload(apk_bytes: bytes, url: str, ssid: str, password: str, site: str):
    if not url.startswith('https://') or not urlparse(url).netloc:
        raise ValueError('Wrapper APK musi mieć bezpośredni publiczny adres HTTPS')
    if not ssid:
        raise ValueError('SSID nie może być pusty')
    if not password or len(password) < 8:
        raise ValueError('Hasło WPA powinno mieć co najmniej 8 znaków')
    checksum = base64.urlsafe_b64encode(hashlib.sha256(apk_bytes).digest()).decode('ascii').rstrip('=')
    return {
        'android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME': ADMIN,
        'android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION': url,
        'android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM': checksum,
        'android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED': True,
        'android.app.extra.PROVISIONING_WIFI_SSID': ssid,
        'android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE': 'WPA',
        'android.app.extra.PROVISIONING_WIFI_PASSWORD': password,
        'android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE': {'site_id': site},
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--wrapper-apk', type=Path, required=True,
                   help='APK identyczny bajt w bajt z plikiem pod adresem URL')
    p.add_argument('--wrapper-url', required=True,
                   help='Publiczny URL HTTPS do powyższego APK (np. GitHub Release asset)')
    p.add_argument('--ssid', required=True)
    p.add_argument('--site', required=True)
    p.add_argument('--password', default=None,
                   help='Opcjonalnie: hasło WPA. Uwaga: argument trafia do historii powłoki!')
    p.add_argument('--out', type=Path, default=Path('provisioning'))
    a = p.parse_args()
    if not a.wrapper_apk.is_file():
        p.error('Nie odnaleziono APK Wrappera: ' + str(a.wrapper_apk))
    # Nigdy nie zapisuj prawdziwego hasla w kodzie ani na GitHubie.
    password = a.password or os.environ.get('RETAIL_TEST_WIFI_PASSWORD')
    if not password:
        p.error('Podaj haslo lokalnie przez RETAIL_TEST_WIFI_PASSWORD lub --password')
    payload = build_payload(a.wrapper_apk.read_bytes(), a.wrapper_url, a.ssid,
                            password, a.site)
    data = json.dumps(payload, ensure_ascii=False, separators=(',', ':'))
    a.out.parent.mkdir(parents=True, exist_ok=True)
    json_file, png_file = a.out.with_suffix('.json'), a.out.with_suffix('.png')
    json_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M,
                       box_size=12, border=4)
    qr.add_data(data)
    qr.make(fit=True)
    qr.make_image(fill_color='black', back_color='white').save(png_file)
    print('QR:', png_file)
    print('JSON:', json_file)
    print('UWAGA: oba pliki zawierają JAWNE HASŁO Wi-Fi; nie publikuj ich!')
    print('SHA-256 wrappera (hex):', hashlib.sha256(a.wrapper_apk.read_bytes()).hexdigest())
    print('Provisioning component:', ADMIN)


if __name__ == '__main__':
    main()
