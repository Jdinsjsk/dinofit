// Généré depuis assets/ranks.json : mêmes rangs que la version PC.
package com.dinofit.app.core.model

/** Les 19 rangs, débloqués par le tonnage cumulé (kg × répétitions, toutes séances confondues). */
val DinoRanks: List<DinoRank> = listOf(
    DinoRank(1, "fossile", "Fossile", "Fossil", 0.0, "Tout commence par des os. Soulève tes 5 premières tonnes pour reprendre vie."),
    DinoRank(2, "archeopteryx", "Archéoptéryx", "Archaeopteryx", 5000.0, "Mi-oiseau, mi-dino. Tu prends ton envol."),
    DinoRank(3, "dimorphodon", "Dimorphodon", "Dimorphodon", 15000.0, "Petit reptile volant à grosse tête. La régularité commence."),
    DinoRank(4, "velociraptor", "Vélociraptor", "Velociraptor", 30000.0, "Chasseur en meute, rapide et malin."),
    DinoRank(5, "dilophosaure", "Dilophosaure", "Dilophosaurus", 50000.0, "Collerette déployée. Tu commences à impressionner."),
    DinoRank(6, "corythosaure", "Corythosaure", "Corythosaurus", 75000.0, "Crête en casque. Solide sur ses appuis."),
    DinoRank(7, "parasaurolophus", "Parasaurolophus", "Parasaurolophus", 105000.0, "Sa crête résonnait comme une trompette. On t'entend arriver."),
    DinoRank(8, "pteranodon", "Ptéranodon", "Pteranodon", 140000.0, "Sept mètres d'envergure. Tu planes au-dessus du lot."),
    DinoRank(9, "stegosaure", "Stégosaure", "Stegosaurus", 180000.0, "Plaques dorsales et queue à pointes. Solide comme un roc."),
    DinoRank(10, "ankylosaure", "Ankylosaure", "Ankylosaurus", 230000.0, "Un char d'assaut vivant. Rien ne t'arrête."),
    DinoRank(11, "utahraptor", "Utahraptor", "Utahraptor", 290000.0, "Le plus grand des raptors. Griffes de 24 cm."),
    DinoRank(12, "triceratops", "Tricératops", "Triceratops", 360000.0, "Trois cornes, zéro peur. Tu imposes le respect."),
    DinoRank(13, "carnotaure", "Carnotaure", "Carnotaurus", 440000.0, "Le taureau carnivore. Un sprinteur redoutable."),
    DinoRank(14, "allosaure", "Allosaure", "Allosaurus", 530000.0, "Le prédateur roi du Jurassique."),
    DinoRank(15, "elasmosaure", "Élasmosaure", "Elasmosaurus", 640000.0, "Un cou de sept mètres. Tu vois loin."),
    DinoRank(16, "brachiosaure", "Brachiosaure", "Brachiosaurus", 770000.0, "La tête dans les nuages. Peu de gens arrivent si haut."),
    DinoRank(17, "mosasaure", "Mosasaure", "Mosasaurus", 920000.0, "Le monstre des mers. Tu domines les profondeurs."),
    DinoRank(18, "spinosaure", "Spinosaure", "Spinosaurus", 1100000.0, "Plus grand que le T-Rex. L'avant-dernière marche."),
    DinoRank(19, "tyrannosaure", "Tyrannosaure", "Tyrannosaurus", 1500000.0, "Le roi. Tu as soulevé plus de 1 500 tonnes."),
)

/** Faits de secours quand dinofacts.fyi n'est pas joignable (hors connexion). */
val OfflineDinoFacts: List<DinoFact> = listOf(
    DinoFact(dino = "Tyrannosaurus", fact = "Le Tyrannosaure avait la morsure la plus puissante de tous les animaux terrestres connus : jusqu'à 57 000 newtons.", sourceUrl = null),
    DinoFact(dino = "Stegosaurus", fact = "Le cerveau du Stégosaure avait à peu près la taille d'une noix, pour un animal de 9 mètres.", sourceUrl = null),
    DinoFact(dino = "Velociraptor", fact = "Le vrai Vélociraptor avait des plumes et mesurait à peine la taille d'une dinde.", sourceUrl = null),
    DinoFact(dino = "Brachiosaurus", fact = "Le Brachiosaure pouvait lever la tête à plus de 12 mètres du sol, la hauteur d'un immeuble de 4 étages.", sourceUrl = null),
    DinoFact(dino = "Triceratops", fact = "Le crâne du Tricératops pouvait mesurer plus de 2 mètres de long, l'un des plus grands de tous les animaux terrestres.", sourceUrl = null),
    DinoFact(dino = "Ankylosaurus", fact = "La massue de la queue de l'Ankylosaure pouvait atteindre 57 cm de large, assez pour briser des os.", sourceUrl = null),
    DinoFact(dino = "Spinosaurus", fact = "Le Spinosaure vivait en partie dans l'eau et chassait de grands poissons, comme un crocodile géant.", sourceUrl = null),
    DinoFact(dino = "Archaeopteryx", fact = "L'Archéoptéryx, découvert en 1861, est l'un des premiers fossiles à relier les dinosaures aux oiseaux.", sourceUrl = null),
    DinoFact(dino = "Parasaurolophus", fact = "La crête creuse du Parasaurolophus servait probablement de caisse de résonance pour produire des sons graves.", sourceUrl = null),
    DinoFact(dino = "Pteranodon", fact = "Le Ptéranodon n'était pas un dinosaure mais un ptérosaure, et il n'avait pas de dents.", sourceUrl = null),
    DinoFact(dino = "Mosasaurus", fact = "Le Mosasaure était un lézard marin géant de plus de 12 mètres, cousin éloigné des varans actuels.", sourceUrl = null),
    DinoFact(dino = "Allosaurus", fact = "Des fossiles d'Allosaure portent des blessures causées par la queue à pointes d'un Stégosaure.", sourceUrl = null),
    DinoFact(dino = "Carnotaurus", fact = "Le Carnotaure avait des bras encore plus petits que ceux du T-Rex, presque inutiles.", sourceUrl = null),
    DinoFact(dino = "Utahraptor", fact = "La griffe en faucille de l'Utahraptor pouvait mesurer jusqu'à 24 cm.", sourceUrl = null),
    DinoFact(dino = "Elasmosaurus", fact = "Le cou de l'Élasmosaure comptait 72 vertèbres, l'un des records du règne animal.", sourceUrl = null),
)
