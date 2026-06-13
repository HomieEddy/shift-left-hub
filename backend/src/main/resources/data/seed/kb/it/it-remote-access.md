---
title_en: Remote Access to Intranet
title_fr: Accès distant à l'intranet
tags: Remote Access, VPN
slug: it-remote-access
excerpt: Guide for accessing internal company resources remotely via VPN, including Remote Desktop, application access, and file share connectivity.
excerpt_fr: Guide pour accéder aux ressources internes de l'entreprise à distance via VPN, incluant le Bureau à distance, l'accès aux applications et la connectivité aux partages de fichiers.
---

# Remote Access to Intranet

## Overview

Remote access allows employees to securely connect to internal company resources from outside the office. All remote connections require the VPN to be active.

## Prerequisites

- VPN client installed and configured (see [VPN Setup Guide](/article/it-vpn-setup))
- Active corporate account with MFA enrolled
- Company-issued laptop or approved personal device
- Stable internet connection (minimum 5 Mbps download / 2 Mbps upload)

## Accessing Company Applications via VPN

Once connected to the VPN:

1. Launch your VPN client and connect to the corporate VPN
2. Open a web browser
3. Navigate to the Intranet Portal: https://intranet.company.com
4. You will have access to:
   - Knowledge Base (internal articles and documentation)
   - Ticket System (create and manage support tickets)
   - HR Portal (pay stubs, time off requests)
   - Departmental applications (varies by role)

## Remote Desktop Connection (RDC)

To access your office workstation remotely:

1. Ensure your office computer is powered on and connected to the network
2. Connect to the corporate VPN
3. Open **Remote Desktop Connection** on your laptop
4. Enter your office computer name (e.g., `WS-12345`)
5. Enter your corporate credentials
6. Click **Connect**

## Accessing File Shares Remotely

### Windows File Explorer

1. Connect to the VPN
2. Open File Explorer
3. In the address bar, type `\\files.company.com\shared`
4. Enter your credentials when prompted
5. Mapped drives will appear in **This PC**

### macOS

1. Connect to the VPN
2. Open **Finder**
3. Press **Cmd + K** (Connect to Server)
4. Enter `smb://files.company.com/shared`
5. Enter your corporate credentials
6. The share will mount on your desktop

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Cannot access intranet | Verify VPN is connected; try accessing http://intranet.company.com |
| RDP connection fails | Verify office computer is on and not in sleep mode; check with IT if off |
| File share not working | Ensure VPN is active; verify you have permissions to the share |
| Slow performance | Check internet speed; close unnecessary applications |
| Application not loading | Clear browser cache and cookies; try a different browser |

<!-- FR -->

# Accès distant à l'intranet

## Aperçu

L'accès distant permet aux employés de se connecter de manière sécurisée aux ressources internes de l'entreprise depuis l'extérieur du bureau. Toutes les connexions à distance nécessitent que le VPN soit actif.

## Prérequis

- Client VPN installé et configuré (voir [Guide d'installation VPN](/article/it-vpn-setup))
- Compte professionnel actif avec authentification multifacteur inscrite
- Ordinateur portable fourni par l'entreprise ou appareil personnel approuvé
- Connexion Internet stable (minimum 5 Mbps téléchargement / 2 Mbps téléversement)

## Accès aux applications d'entreprise via VPN

Une fois connecté au VPN :

1. Lancez votre client VPN et connectez-vous au VPN d'entreprise
2. Ouvrez un navigateur Web
3. Accédez au Portail Intranet : https://intranet.company.com
4. Vous aurez accès à :
   - Base de connaissances (articles et documentation internes)
   - Système de billets (créer et gérer des tickets de support)
   - Portail RH (fiches de paie, demandes de congés)
   - Applications départementales (varie selon le rôle)

## Connexion au Bureau à distance (RDC)

Pour accéder à votre poste de travail de bureau à distance :

1. Assurez-vous que votre ordinateur de bureau est allumé et connecté au réseau
2. Connectez-vous au VPN d'entreprise
3. Ouvrez **Connexion Bureau à distance** sur votre ordinateur portable
4. Entrez le nom de votre ordinateur de bureau (ex. : `WS-12345`)
5. Entrez vos identifiants professionnels
6. Cliquez sur **Connecter**

## Accès aux partages de fichiers à distance

### Explorateur de fichiers Windows

1. Connectez-vous au VPN
2. Ouvrez l'Explorateur de fichiers
3. Dans la barre d'adresse, tapez `\\files.company.com\shared`
4. Entrez vos identifiants lorsque demandé
5. Les lecteurs mappés apparaîtront dans **Ce PC**

### macOS

1. Connectez-vous au VPN
2. Ouvrez **Finder**
3. Appuyez sur **Cmd + K** (Se connecter au serveur)
4. Entrez `smb://files.company.com/shared`
5. Entrez vos identifiants professionnels
6. Le partage sera monté sur votre bureau

## Dépannage

| Problème | Solution |
|----------|----------|
| Impossible d'accéder à l'intranet | Vérifiez que le VPN est connecté ; essayez http://intranet.company.com |
| Échec de la connexion RDP | Vérifiez que l'ordinateur du bureau est allumé et en veille ; vérifiez auprès du service informatique |
| Partages de fichiers inaccessibles | Assurez-vous que le VPN est actif ; vérifiez que vous avez les permissions sur le partage |
| Performances lentes | Vérifiez votre vitesse Internet ; fermez les applications inutiles |
| Application qui ne charge pas | Vide de cache et les cookies du navigateur ; essayez un autre navigateur |
