---
title_en: Accessing the Company Intranet Remotely
title_fr: Accéder à l'intranet de l'entreprise à distance
tags: [vpn, remote-access, networking]
slug: remote-access-intranet
excerpt: How to connect to the company intranet from home or while traveling using the VPN client.
excerpt_fr: Comment se connecter à l'intranet de l'entreprise depuis chez vous ou en voyage à l'aide du client VPN.
---

# Accessing the Company Intranet Remotely

Working remotely requires a secure VPN connection to access internal company resources.

## Before You Start

You need the following:
- A company-issued laptop (or personal device with IT approval)
- VPN client software installed
- Active company account with MFA configured
- Stable internet connection

## Step 1: Install the VPN Client

### Windows / macOS
1. Download the VPN client from [portal.company.com/tools/vpn](https://portal.company.com/tools/vpn)
2. Run the installer — **administrator privileges** are required
3. Restart your computer when prompted

### Company-issued Devices
The VPN client is pre-installed on all company laptops. Skip to Step 2.

## Step 2: Connect to the VPN

1. Open the **Company VPN** application
2. Enter the server address: **vpn.company.com**
3. Click **Connect**
4. Enter your **AD username** and **password**
5. Approve the **MFA prompt** on your phone
6. You are now connected — the VPN icon will show **Connected**

## Step 3: Access the Intranet

Once connected, you can access internal resources:
- **Intranet Home:** [https://intranet.company.com](https://intranet.company.com)
- **HR Portal:** [https://hr.company.com](https://hr.company.com)
- **IT Service Desk:** [https://servicedesk.company.com](https://servicedesk.company.com)
- **File Shares:** `\\fileserver\shared\`

## Disconnecting

When finished working remotely, disconnect the VPN:
- **Right-click** the VPN icon in the system tray and select **Disconnect**
- Alternatively, open the VPN app and click **Disconnect**

## Troubleshooting

### Cannot Connect to VPN
- Check your **internet connection**
- Verify your **password** is correct
- Ensure **MFA** is set up on your account
- Try a different network (switch from Wi-Fi to mobile hotspot)

### VPN Connected but Cannot Access Intranet
- Your VPN may have **split tunneling** disabled — all traffic routes through the VPN
- Try accessing the intranet via **IP address** instead of hostname
- Flush DNS: `ipconfig /flushdns` (Windows) or `sudo dscacheutil -flushcache` (macOS)

### Slow Connection
- Use a **wired Ethernet connection** if possible
- Close bandwidth-heavy applications (video streaming, large downloads)
- Switch to a different VPN protocol in the client settings (WireGuard is fastest)

## Security Reminders
- Do not leave your device **unattended** while connected to the VPN
- Always **disconnect** the VPN when not actively working
- Report lost or stolen devices immediately to IT
- Do not use public Wi-Fi without the VPN enabled

<!-- FR -->

# Acceder a l'intranet de l'entreprise a distance

Le travail a distance exige une connexion VPN securisee pour acceder aux ressources internes.

## Avant de commencer

Vous avez besoin de:
- Un ordinateur professionnel (ou appareil personnel approuve par l'IT)
- Le client VPN installe
- Un compte entreprise actif avec MFA configuree
- Une connexion internet stable

## Etape 1: Installer le client VPN

### Windows / macOS
1. Telechargez le client VPN: [portal.company.com/tools/vpn](https://portal.company.com/tools/vpn)
2. Lancez l'installation (droits administrateur requis)
3. Redemarrez votre ordinateur si demande

### Appareils professionnels
Le client VPN est deja installe sur les ordinateurs de l'entreprise. Passez a l'etape 2.

## Etape 2: Se connecter au VPN

1. Ouvrez l'application **Company VPN**
2. Entrez l'adresse du serveur: **vpn.company.com**
3. Cliquez **Connect**
4. Entrez votre **nom d'utilisateur AD** et votre **mot de passe**
5. Validez la demande **MFA** sur votre telephone
6. L'etat passe a **Connected**

## Etape 3: Acceder a l'intranet

Une fois connecte, vous pouvez acceder a:
- **Accueil Intranet:** [https://intranet.company.com](https://intranet.company.com)
- **Portail RH:** [https://hr.company.com](https://hr.company.com)
- **Service Desk IT:** [https://servicedesk.company.com](https://servicedesk.company.com)
- **Partages fichiers:** `\\fileserver\shared\`

## Deconnexion

Quand vous avez termine:
- Clic droit sur l'icone VPN > **Disconnect**
- Ou ouvrez l'application puis cliquez **Disconnect**

## Depannage

### Impossible de se connecter
- Verifiez la connexion internet
- Verifiez votre mot de passe
- Verifiez la configuration MFA
- Testez un autre reseau

### VPN connecte mais intranet inaccessible
- Votre configuration peut desactiver le split tunneling
- Testez via adresse IP
- Videz le cache DNS (`ipconfig /flushdns` sur Windows)

### Connexion lente
- Preferez une connexion Ethernet
- Fermez les applications gourmandes
- Essayez un autre protocole VPN

## Rappels de securite
- Ne laissez pas votre appareil sans surveillance
- Deconnectez le VPN quand vous ne travaillez pas
- Signalez tout appareil perdu/vole immediatement
- N'utilisez pas un Wi-Fi public sans VPN actif
