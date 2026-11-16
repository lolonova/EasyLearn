# Eduko — première version Android

Application éducative Android pour les élèves de 5e, 4e et 3e. Le projet source reste hébergé dans le dépôt EasyLearn.

## Version 0.1.0
- Profil local : prénom, classe, compagnon choisi et besoins d'apprentissage facultatifs (DYS, TDAH, TSA, concentration, mémorisation…).
- Accueil avec demande écrite, reconnaissance vocale Android et prise de photo.
- Première leçon de démonstration sur Pythagore.
- Quiz de cinq questions avec bilan sur cinq étoiles.
- Écrans de démonstration pour le professeur et les ressources audio/vidéo.

## Matières prévues
Mathématiques, Français, Anglais, Italien, Histoire-Géographie, Physique-Chimie et SVT.

## Stratégie de connaissances recommandée
Eduko doit utiliser une approche hybride, avec le cours de l'élève comme source principale :
1. Une base pédagogique structurée par matière, niveau, chapitre et compétence, alignée sur les programmes.
2. Le cours ou l'exercice photographié par l'élève devient la référence prioritaire après extraction OCR et validation.
3. Les connaissances générales du professeur IA servent à reformuler, expliquer autrement, ajouter des exemples et combler les lacunes.
4. La recherche en ligne est réservée aux ressources actualisées et aux vidéos pertinentes, en affichant les sources.

Dans chaque leçon, Eduko devra distinguer « ce que dit ton cours » et « pour aller plus loin ». Si une explication générale semble contredire le document de l'enseignant, l'application le signalera et demandera vérification plutôt que de remplacer le cours silencieusement.

La base doit être cumulative : les cours validés par l'élève enrichissent sa bibliothèque personnelle et restent consultables pour les prochaines révisions. Les données personnelles et documents scolaires doivent rester privés.

## Limites de la version 0.1.0
C'est une première base Android locale. La génération IA, l'OCR intelligent des photos, le véritable dialogue IA, la synthèse vocale du cours et le catalogue vidéo ne sont pas encore connectés. Les boutons audio/vidéo et le professeur sont des écrans de démonstration, pas des services déjà opérationnels. Les personnages 3D définitifs et leurs animations seront intégrés dans une étape graphique ultérieure.

## Compilation
Le workflow GitHub Actions « Build Eduko APK » construit l'APK debug à chaque push sur main et sur demande manuelle.

Build de test demandé le 16 novembre 2026.
