---
title_en: Wi-Fi Network Access
title_fr: Accès au réseau Wi-Fi
tags: Wi-Fi, Networking
slug: it-wifi-access
excerpt: Instructions for connecting to corporate and guest Wi-Fi networks, including WPA2-Enterprise configuration and common troubleshooting steps.
excerpt_fr: Instructions pour se connecter aux réseaux Wi-Fi d'entreprise et invité, incluant la configuration WPA2-Enterprise et les étapes de dépannage courantes.
---

# Wi-Fi Network Access

## Available Networks

The company provides two wireless networks:

| Network Name (SSID) | Purpose | Authentication |
|--------------------|---------|---------------|
| `Company-Corp` | Employee access to internal resources | WPA2-Enterprise (802.1X) |
| `Company-Guest` | Visitor internet access | Portal login (sponsor code) |

## Connecting to Company-Corp (WPA2-Enterprise)

### Windows 11

1. Click the Wi-Fi icon in the system tray
2. Select **Company-Corp** from the network list
3. Click **Connect**
4. Enter your corporate credentials (email@company.com and password)
5. Click **OK** to accept the certificate warning (our internal CA)
6. Wait for the "Connected" confirmation

### macOS

1. Click the Wi-Fi icon in the menu bar
2. Select **Company-Corp**
3. Enter your corporate username and password
4. Click **Join**
5. Accept the certificate when prompted

### iOS / Android

1. Open **Settings** → **Wi-Fi**
2. Tap **Company-Corp**
3. Enter your corporate credentials
4. Tap **Join** or **Connect**
5. A profile will be installed — accept the configuration

## Guest Wi-Fi Access

Visitors and contractors can connect to `Company-Guest`:

1. Select the **Company-Guest** network
2. Open a browser — you will be redirected to the guest portal
3. Enter the sponsor code provided by your host
4. Accept the terms of service
5. Guest access is valid for 24 hours (extendable)

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Cannot see Company-Corp | Ensure Wi-Fi is enabled; move closer to an access point |
| Authentication failed | Verify your password; reset at https://password.company.com |
| Certificate warning | This is normal for company-issued certificates — click Accept |
| Slow connection | Check signal strength; connect to the 5 GHz band if available |
| Device not supported | Contact IT helpdesk for manual network profile configuration |

<!-- FR -->

# Accès au réseau Wi-Fi

## Réseaux disponibles

L'entreprise propose deux réseaux sans fil :

| Nom du réseau (SSID) | Objectif | Authentification |
|----------------------|----------|-----------------|
| `Company-Corp` | Accès employé aux ressources internes | WPA2-Entreprise (802.1X) |
| `Company-Guest` | Accès Internet pour les visiteurs | Portail de connexion (code sponsor) |

## Connexion à Company-Corp (WPA2-Entreprise)

### Windows 11

1. Cliquez sur l'icône Wi-Fi dans la barre d'état système
2. Sélectionnez **Company-Corp** dans la liste des réseaux
3. Cliquez sur **Connecter**
4. Entrez vos identifiants professionnels (courriel@company.com et mot de passe)
5. Cliquez sur **OK** pour accepter l'avertissement de certificat (notre AC interne)
6. Attendez la confirmation « Connecté »

### macOS

1. Cliquez sur l'icône Wi-Fi dans la barre de menus
2. Sélectionnez **Company-Corp**
3. Entrez votre nom d'utilisateur et mot de passe professionnels
4. Cliquez sur **Rejoindre**
5. Acceptez le certificat lorsque demandé

### iOS / Android

1. Ouvrez **Paramètres** → **Wi-Fi**
2. Appuyez sur **Company-Corp**
3. Entrez vos identifiants professionnels
4. Appuyez sur **Rejoindre** ou **Connecter**
5. Un profil sera installé — acceptez la configuration

## Accès Wi-Fi invité

Les visiteurs et les entrepreneurs peuvent se connecter à `Company-Guest` :

1. Sélectionnez le réseau **Company-Guest**
2. Ouvrez un navigateur — vous serez redirigé vers le portail invité
3. Entrez le code de parrain fourni par votre hôte
4. Acceptez les conditions d'utilisation
5. L'accès invité est valide 24 heures (prolongeable)

## Dépannage

| Problème | Solution |
|----------|----------|
| Impossible de voir Company-Corp | Assurez-vous que le Wi-Fi est activé ; rapprochez-vous d'un point d'accès |
| Échec d'authentification | Vérifiez votre mot de passe ; réinitialisez sur https://password.company.com |
| Avertissement de certificat | C'est normal pour les certificats d'entreprise — cliquez sur Accepter |
| Connexion lente | Vérifiez l'intensité du signal ; connectez-vous à la bande 5 GHz si disponible |
| Appareil non pris en charge | Contactez le service d'assistance informatique pour une configuration manuelle du profil réseau |
