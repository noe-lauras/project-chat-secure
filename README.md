# Projet Chat Sécurisé

Ce projet vise à créer une application client-serveur permettant un échange sécurisé de messages. L'implémentation se base sur l'utilisation de sockets TCP pour la communication entre le client et le serveur.
## Fonctionnalités

### Communication de base : 
Le client et le serveur peuvent échanger des chaînes de caractères via des sockets TCP.

### Fin de connexion : 
La réception de la chaîne "bye" entraîne la fin de la connexion entre le client et le serveur.

### Échange de clé AES : 
Lorsqu'un client se connecte, une clé AES est générée et échangée entre le client et le serveur.

### Chiffrement et déchiffrement : 
La clé partagée est utilisée pour chiffrer les messages envoyés et les déchiffrer à la réception. Les versions chiffrée et en clair de chaque message sont affichées.

### Sécurité améliorée : 
Pour renforcer la sécurité des échanges, nous avons mis en place les améliorations suivantes :

. Utilisation de classes Java spécifiques pour la gestion des sockets et de la cryptographie.

. Choix d'un numéro de port standard (par exemple, 5000) pour la facilité de configuration et d'utilisation.

. Utilisation de l'algorithme AES pour le chiffrement, un algorithme de chiffrement symétrique robuste.

. Intégration de Threads pour permettre au serveur de gérer simultanément plusieurs clients.


