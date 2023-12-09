# Application de Chat Sécurisée

Il s'agit d'une application de chat client-serveur simple implémentée en Java avec une communication sécurisée utilisant l'échange de clés Diffie-Hellman. L'application permet aux utilisateurs de se connecter à un serveur, d'échanger des messages et garantit la confidentialité de la communication.

## Fonctionnalités

- **Communication Sécurisée :** Implémente l'échange de clés Diffie-Hellman pour une communication sécurisée entre le client et le serveur.
- **Multithreading :** Utilise des threads pour gérer plusieurs clients simultanément.
- **Sérialisation d'Objets :** Les messages sont sérialisés et envoyés sur le réseau à l'aide de `ObjectOutputStream` et `ObjectInputStream`.
- **Ping UDP :** Découvre l'adresse IP du serveur à l'aide d'un mécanisme de ping UDP simple.

## Pour Commencer

### Prérequis

- Java Development Kit (JDK) installé.
- Éditeur de code (par exemple, IntelliJ, Eclipse) pour le développement.

### Utilisation

1. **Serveur :**
    - Compilez et exécutez `MainServer.java`.
    - Le serveur écoutera les connexions entrantes sur le port spécifié.

2. **Client :**
    - Compilez et exécutez `MainClient.java`.
    - Entrez un nom d'utilisateur lorsqu'on vous le demande.
    - Le client tentera de se connecter au serveur, d'échanger des clés et de démarrer le chat.

## Structure du Projet

- **MainClient.java :** Classe principale pour l'exécution du client.
- **MainServer.java :** Classe principale pour l'exécution du serveur.
- **Client.java :** Implémentation du côté client du chat. Gère la connexion au serveur, l'envoi et la réception de messages.
- **Server.java :** Implémentation du côté serveur du chat. Gère les connexions des clients, la diffusion des messages, et la gestion des clés Diffie-Hellman.
- **ClientGUI.java :** Interface graphique pour le client du chat. Permet à l'utilisateur d'interagir avec le chat de manière conviviale.
- **DHKey.java :** Implémentation de l'échange de clés Diffie-Hellman. Génère les clés, crypte et décrypte les messages.
- **Message.java :** Classe Message pour la communication. Contient les informations nécessaires pour les échanges entre le client et le serveur.
- **MessageListener.java :** Interface pour les rappels de messages. Utilisée pour mettre à jour l'interface utilisateur avec les nouveaux messages reçus.

## Contribuer

N'hésitez pas à contribuer au projet en ouvrant des problèmes ou en soumettant des demandes d'extraction. Les contributions sont les bienvenues !

## Contexte du Projet

Ce projet a été développé dans le cadre d'un sujet proposé par M. Pouit, enseignant à l'INU Champollion. L'objectif principal était de concevoir un système de chat sécurisé utilisant un protocole de notre choix pour l'échange sécurisé de messages entre un client et un serveur. En mettant en œuvre ces concepts de cryptographie, le projet vise à fournir une expérience de communication confidentielle et authentifiée dans un environnement de chat en ligne. Cette initiative s'inscrit dans le cadre de l'apprentissage des technologies réseau, de la sécurité informatique et de la programmation orientée objet.
