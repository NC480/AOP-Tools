# Változásnapló

A fontosabb változások ebben a fájlban követhetők.

## 1.14.3 – 2026-11-23

- A botmodul Meteor-panelje külön, közvetlen `BOTOK INDÍTÁSA` és `BOTOK LEÁLLÍTÁSA` gombokat kapott.
- Új `/aop bots`, `/aop bots stop` és `/aop bots status` parancsok kerülik meg a csempe-toggle hibás vagy félreérthető kezelését.
- Az aktiválás és a háttérfolyamat indítása bekerült a Minecraft naplóba, ezért a GUI-, beállítás- és kapcsolati hiba egyértelműen elkülöníthető.

## 1.14.2 – 2026-11-23

- A külön Bot szerver-whitelist csempe és a redundáns saját-szerver megerősítő kapcsoló megszűnt; a botmodul aktiválása közvetlenül elindítja a háttérklienst.
- A localhost/privát LAN korlátozás megmaradt, a segédkliens pedig automatikusan csak a modulban megadott egyetlen ellenőrzött végpontot fogadja el.
- Sikertelen belépéskor a Meteor már a háttérnapló utolsó konkrét hibaüzenetét is megjeleníti.

## 1.14.1 – 2026-11-23

- A bot csak a teljes Minecraft-belépés és a `GAME` protokollállapot elérése után számít sikeresnek; a puszta TCP/proxykapcsolat már nem ad téves sikerjelzést.

## 1.14.0 – 2026-11-23

- A helyi stresszteszt botjai egyedi, ellenőrzött neveket, 1–60 másodperces belépési időközt, kapcsolati timeoutot és korlátozott újrapróbálást kaptak.
- Ha egyetlen bot sem tud belépni, a háttérfolyamat most hibával leáll és a Meteor a naplóhoz irányít, nem várja ki üresen a teljes futási időt.
- A KICK/BAN ALL és XY blokkok saját, presetben tárolt indokot fogadnak; a régi presetek kompatibilis alapindokot kapnak.
- A kiadás része a teljes editor/preset/whitelist állapotkezelési audit, a biztonságos Prism-telepítés és a kibővített offline ellenőrzőkészlet.

## 1.13.0

- A blokkeditor kis ablakban is elérhető eszköztárat, méretezhető paneleket és beszúrásjelzőt kapott. Átméretezéskor megőrzi a beírt keresést, játékosnevet és blokkazonosítót.
- A whitelist szerkesztő külön törlésgombot ad minden látható sorhoz, és hozzáadáskor az új címhez görget.

## 1.12.0

- A Prism-csomagból bekapcsolható a Boosted Brightness 1.2.
- A 24 karakteres presetnevek ütközésekor a szerkesztő helyet hagy a sorszámnak. A paletta a kattintáshoz és a rajzoláshoz azonos sormagasságot használ.
- A stresszteszt időkorlátjába a csatlakozási idő is beleszámít.
- A telepítő hibát jelez, ha egy zárolt régi modverzió megakadályozza a frissítést.

## 1.4.1 – 2026-11-19

- Minden blokkparancshoz ALL és XY változat.
- Új KICK XY és BAN XY blokkok.

## 1.4.0 – 2026-11-19

- Másodperc-alapú, 0,05–10,0 másodperces parancsközi csúszka.
- KILL ALL és KILL XY blokkok.
- Görgethető blokkpaletta kis GUI-méretnél.
- Mindig látható XY játékosnév mező.
- A modul bindje közvetlenül futtatja a mentett blokkprogramot.
- Stabil, Meteor-kompatibilis editorháttér.
- GitHub Actions build és publikálásra előkészített projekt.

## 1.3.0

- Scratch-szerű blokkeditor.
- OP ALL, deOP ALL, KICK ALL, BAN ALL, OP XY és deOP XY blokkok.
- Belépéskori automatikus futtatás és veszélyes műveletek védelme.

## 1.2.0

- Külön Meteor-modulok az Automata OP és a Belépési Parancsok funkcióhoz.

## 1.0.0

- Első működő Fabric/Meteor kiadás és /aop parancs.
