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

<!-- FR -->

# Connexion au reseau Wi-Fi de l'entreprise

Ce guide explique comment connecter votre ordinateur portable, votre telephone intelligent ou votre tablette au reseau Wi-Fi de l'entreprise.

## Reseaux disponibles

| Nom du reseau | Securite | Ideal pour |
|---|---|---|
| `Company-Staff` | WPA2-Enterprise | Ordinateurs fournis par l'entreprise |
| `Company-Guest` | WPA2-PSK | Appareils visiteurs |

## Connexion a Company-Staff (ordinateurs portables)

### Windows
1. Cliquez sur l'icone **Wi-Fi** dans la barre des taches
2. Selectionnez **Company-Staff** puis cliquez sur **Connecter**
3. Entrez votre **nom d'utilisateur AD** (ex. `jdoe`) et votre **mot de passe**
4. Cliquez sur **OK**

### macOS
1. Cliquez sur l'icone **Wi-Fi** dans la barre de menu
2. Selectionnez **Company-Staff**
3. Entrez votre **nom d'utilisateur AD** et votre **mot de passe**
4. Cliquez sur **Rejoindre**

### Ubuntu / Linux
1. Ouvrez **Parametres > Wi-Fi**
2. Selectionnez **Company-Staff**
3. Reglez la securite sur **WPA & WPA2 Enterprise**
4. Authentification: **Protected EAP (PEAP)**
5. Certificat CA: **Aucun** (ou certificat de l'entreprise s'il est fourni)
6. Authentification interne: **MSCHAPv2**
7. Entrez votre **nom d'utilisateur AD** et votre **mot de passe**

## Connexion a Company-Guest (visiteurs)
1. Selectionnez **Company-Guest** dans la liste des reseaux
2. Mot de passe: **CompanyGuest2026**
3. Acceptez les conditions de service lorsque le navigateur s'ouvre

## Depannage
- Verifiez que le **Wi-Fi est active** sur votre appareil
- Oubliez le reseau puis ressaisissez les identifiants
- Redemarrez votre appareil puis reessayez
- Contactez l'IT si le probleme persiste

## Avis de securite
Ne partagez jamais le mot de passe du reseau invite avec des personnes non autorisees. Le reseau invite est surveille et journalise.
