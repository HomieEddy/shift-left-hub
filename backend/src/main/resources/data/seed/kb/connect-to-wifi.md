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

# Connexion au réseau Wi-Fi de l'entreprise

Ce guide explique comment connecter votre ordinateur portable, votre téléphone intelligent ou votre tablette au réseau Wi-Fi de l'entreprise.

## Réseaux disponibles

| Nom du réseau | Sécurité | Idéal pour |
|---|---|---|
| `Company-Staff` | WPA2-Enterprise | Ordinateurs fournis par l'entreprise |
| `Company-Guest` | WPA2-PSK | Appareils visiteurs |

## Connexion à Company-Staff (ordinateurs portables)

### Windows
1. Cliquez sur l'icône **Wi-Fi** dans la barre des tâches
2. Sélectionnez **Company-Staff** puis cliquez sur **Connecter**
3. Entrez votre **nom d'utilisateur AD** (ex. `jdoe`) et votre **mot de passe**
4. Cliquez sur **OK**

### macOS
1. Cliquez sur l'icône **Wi-Fi** dans la barre de menu
2. Sélectionnez **Company-Staff**
3. Entrez votre **nom d'utilisateur AD** et votre **mot de passe**
4. Cliquez sur **Rejoindre**

### Ubuntu / Linux
1. Ouvrez **Paramètres > Wi-Fi**
2. Sélectionnez **Company-Staff**
3. Réglez la sécurité sur **WPA & WPA2 Enterprise**
4. Authentification: **Protected EAP (PEAP)**
5. Certificat CA: **Aucun** (ou certificat de l'entreprise s'il est fourni)
6. Authentification interne: **MSCHAPv2**
7. Entrez votre **nom d'utilisateur AD** et votre **mot de passe**

## Connexion à Company-Guest (visiteurs)
1. Sélectionnez **Company-Guest** dans la liste des réseaux
2. Mot de passe: **CompanyGuest2026**
3. Acceptez les conditions de service lorsque le navigateur s'ouvre

## Dépannage
- Vérifiez que le **Wi-Fi est activé** sur votre appareil
- Oubliez le réseau puis ressaisissez les identifiants
- Redémarrez votre appareil puis réessayez
- Contactez l'IT si le problème persiste

## Avis de sécurité
Ne partagez jamais le mot de passe du réseau invité avec des personnes non autorisées. Le réseau invité est surveillé et journalisé.
