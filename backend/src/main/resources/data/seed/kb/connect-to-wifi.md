---
title_en: Connecting to the Corporate Wi-Fi Network
title_fr: Connexion au réseau Wi-Fi d'entreprise
tags: [networking, wifi]
slug: connect-corporate-wifi
excerpt: Instructions for connecting your devices to the company wireless network.
excerpt_fr: Instructions pour connecter vos appareils au réseau sans fil de l'entreprise.
---

# Connecting to the Corporate Wi-Fi Network

This guide covers how to connect your laptop, smartphone, or tablet to the company Wi-Fi network.

## Available Networks

| Network Name | Security | Best For |
|---|---|---|
| `Company-Staff` | WPA2-Enterprise | Company-issued laptops |
| `Company-Guest` | WPA2-PSK | Visitor devices |

## Connecting to Company-Staff (Laptops)

### Windows
1. Click the **Wi-Fi icon** in the system tray
2. Select **Company-Staff** and click **Connect**
3. Enter your **AD username** (e.g., `jdoe`) and **password**
4. Click **OK**

### macOS
1. Click the **Wi-Fi icon** in the menu bar
2. Select **Company-Staff**
3. Enter your **AD username** and **password**
4. Click **Join**

### Ubuntu / Linux
1. Open **Settings > Wi-Fi**
2. Select **Company-Staff**
3. Set security to **WPA & WPA2 Enterprise**
4. Authentication: **Protected EAP (PEAP)**
5. CA certificate: **None** (or use company CA if issued)
6. Inner authentication: **MSCHAPv2**
7. Enter your **AD username** and **password**

## Connecting to Company-Guest (Visitors)
1. Select **Company-Guest** from available networks
2. Password: **CompanyGuest2026**
3. Accept the terms of service when the browser opens

## Troubleshooting
- Ensure **Wi-Fi is enabled** on your device
- Forget the network and re-enter credentials
- Restart your device and try again
- Contact IT if the issue persists

## Security Notice
Never share the guest Wi-Fi password with unauthorized individuals. The guest network is monitored and logged.
