---
title_en: VPN Setup & Configuration
title_fr: Configuration et installation du VPN
tags: VPN, Security
slug: it-vpn-setup
excerpt: Step-by-step guide for installing and configuring the corporate VPN client, including MFA setup and common troubleshooting.
excerpt_fr: Guide étape par étape pour installer et configurer le client VPN d'entreprise, incluant la configuration de l'authentification multifacteur et la résolution des problèmes courants.
---

# VPN Setup & Configuration

## Overview

The corporate VPN provides secure remote access to internal company resources, including file shares, intranet sites, and business applications. All employees must use the VPN when working outside the office network.

## Supported VPN Clients

| Platform | Recommended Client | Download Source |
|----------|-------------------|-----------------|
| Windows 11 | OpenVPN Connect v3.x | Company Software Portal |
| macOS 14+ | OpenVPN Connect v3.x | Company Software Portal |
| iOS 17+ | OpenVPN Connect App | App Store |
| Android 14+ | OpenVPN Connect App | Google Play Store |

## Installation Steps

1. **Download the client** from the Company Software Portal (https://software.company.com)
2. **Install the application** using the default settings
3. **Launch OpenVPN Connect** and click "Import Profile"
4. **Browse to** the configuration file provided by IT (saved to your Downloads folder)
5. **Enter your credentials** — use your corporate email and network password
6. **Complete MFA** — approve the push notification on your registered mobile device

## Multi-Factor Authentication Setup

If you haven't enrolled in MFA yet, visit https://mfa.company.com and follow these steps:

1. Register your mobile number
2. Install Microsoft Authenticator or Google Authenticator
3. Scan the QR code displayed on screen
4. Verify by entering the 6-digit code from your authenticator app

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection timeout | Verify you are connected to the internet and try a different network |
| Authentication failed | Reset your password at https://password.company.com |
| Profile expired | Contact the IT helpdesk to request a new configuration file |
| MFA prompt not appearing | Check that push notifications are enabled in your authenticator app |

## Additional Notes

- The VPN will automatically disconnect after 8 hours of inactivity for security purposes
- Do not use public Wi-Fi hotspots without the VPN active
- For urgent VPN issues during business hours, call the IT helpdesk at extension 4500

<!-- FR -->

# Configuration et installation du VPN

## Aperçu

Le VPN d'entreprise offre un accès distant sécurisé aux ressources internes de l'entreprise, y compris les partages de fichiers, les sites intranet et les applications professionnelles. Tous les employés doivent utiliser le VPN lorsqu'ils travaillent en dehors du réseau du bureau.

## Clients VPN pris en charge

| Plateforme | Client recommandé | Source de téléchargement |
|------------|-------------------|--------------------------|
| Windows 11 | OpenVPN Connect v3.x | Portail logiciel de l'entreprise |
| macOS 14+ | OpenVPN Connect v3.x | Portail logiciel de l'entreprise |
| iOS 17+ | Application OpenVPN Connect | App Store |
| Android 14+ | Application OpenVPN Connect | Google Play Store |

## Étapes d'installation

1. **Téléchargez le client** depuis le Portail logiciel de l'entreprise (https://software.company.com)
2. **Installez l'application** en utilisant les paramètres par défaut
3. **Lancez OpenVPN Connect** et cliquez sur « Importer un profil »
4. **Naviguez vers** le fichier de configuration fourni par le service informatique (enregistré dans votre dossier Téléchargements)
5. **Entrez vos identifiants** — utilisez votre courriel professionnel et votre mot de passe réseau
6. **Effectuez l'authentification multifacteur** — approuvez la notification push sur votre appareil mobile enregistré

## Configuration de l'authentification multifacteur

Si vous n'êtes pas encore inscrit à l'authentification multifacteur, visitez https://mfa.company.com et suivez ces étapes :

1. Enregistrez votre numéro de mobile
2. Installez Microsoft Authenticator ou Google Authenticator
3. Scannez le code QR affiché à l'écran
4. Vérifiez en entrant le code à 6 chiffres de votre application d'authentification

## Dépannage

| Problème | Solution |
|----------|----------|
| Délai de connexion dépassé | Vérifiez votre connexion Internet et essayez un autre réseau |
| Échec d'authentification | Réinitialisez votre mot de passe sur https://password.company.com |
| Profil expiré | Contactez le service d'assistance informatique pour obtenir un nouveau fichier de configuration |
| Invite MFA absente | Vérifiez que les notifications push sont activées dans votre application d'authentification |

## Remarques supplémentaires

- Le VPN se déconnecte automatiquement après 8 heures d'inactivité pour des raisons de sécurité
- N'utilisez pas les points d'accès Wi-Fi publics sans que le VPN soit actif
- Pour les problèmes urgents de VPN pendant les heures de bureau, appelez le service d'assistance informatique au poste 4500
