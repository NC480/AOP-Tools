# AOP Tools – Meteor addon, Fabric 1.21.11

Meteor Client addon szerveradminisztrációhoz, automatikus OP-kezeléshez, belépési parancsokhoz és egy Scratch-szerű blokkeditorhoz.

**AOP Tools is a Minecraft 1.21.11 Fabric mod and Meteor Client addon with a Scratch-style command-block editor for authorized server administration.** It supports OP, deOP, kill, kick and ban workflows with ALL and named-player (XY) variants.

GitHub release: https://github.com/NC480/AOP-Tools/releases/latest

## Követelmények

- Minecraft Java Edition 1.21.11
- Java 21
- Fabric Loader 0.20.1
- Fabric API 0.141.6+1.21.11
- Meteor Client 1.21.11

## Telepítés

1. Töltsd le az aop-tools-1.4.1.jar fájlt a GitHub Releases oldalról, vagy fordítsd le forrásból.
2. Másold a JAR-t a Minecraft mods mappájába.
3. Ugyanott legyen a Fabric API és a Minecraft 1.21.11-hez készült Meteor Client.
4. Indítsd el a Fabric-profilt.
5. Nyisd meg a Meteor GUI-t (Right Shift), majd az **AOP Manager** kategóriát.

## Fordítás forrásból

Java 21 szükséges.

~~~powershell
.\gradlew.bat clean build
~~~

A játékba telepíthető JAR a build/libs/aop-tools-1.4.1.jar fájl lesz. A repository GitHub Actions workflow-ja minden pushnál és pull requestnél ugyanezt a buildet futtatja.

### Prism Launcher fejlesztői példány

A helyi példány neve huncut. Fordítás és telepítés, a játék elindítása nélkül:

~~~powershell
$env:JAVA_HOME = "$env:APPDATA\PrismLauncher\java\java-runtime-delta"
.\gradlew.bat clean deployToHuncut
~~~

## AOP Manager menüpontok

### OP Mindenkinek

Egy kattintásra OP-olja a kliens játékoslistáján látható összes játékost, majd kikapcsol.

### Automata OP

Belépéskor, a megadott szerver- és sajátfiók-egyezésnél OP-olja a célfiókokat. Beállítható, hogy a végén deOP-olja a belépett fiókot.

### Belépési Parancsok

Belépéskor tetszőleges, előre megadott szerverparancsokat futtat. A szerver és a belépési fiók pontos egyezése szükséges.

### Parancs Blokkok

Bal kattintással azonnal megnyílik a Scratch-szerű editor. Jobb kattintás után a **BLOKKEDITOR MEGNYITÁSA** gombbal is elérhető. A bal oldali színes blokkokat a jobb oldali programterületre lehet húzni:

- OP ALL – minden látható online játékos OP-olása.
- deOP ALL – minden látható online játékos deOP-olása.
- KICK ALL – minden látható online játékos kirúgása.
- KICK XY – a névmezőben megadott játékos kirúgása.
- BAN ALL – minden látható online játékos tiltása.
- BAN XY – a névmezőben megadott játékos tiltása.
- KILL ALL – minden látható online játékos megölése.
- OP XY – a névmezőben megadott játékos OP-olása.
- deOP XY – a névmezőben megadott játékos deOP-olása.
- KILL XY – a névmezőben megadott játékos megölése.

A program fentről lefelé fut. A blokkok áthúzással átrendezhetők, jobb kattintással törölhetők, hosszabb programnál az egérgörgővel lehet mozogni. Minden változás automatikusan mentődik a Meteor profiljába.

A Parancs Blokkok modulhoz beállított billentyű- vagy egérbind játék közben azonnal lefuttatja a mentett programot, és nem nyitja meg az editort. A Meteor csempére kattintás és az /aop editor parancs továbbra is a szerkesztőt nyitja meg.

Az editor felső részén beállítható:

- a pontos szervercím és a belépéshez használt saját fióknév;
- fusson-e a blokkprogram belépéskor;
- automatikusan futhassanak-e a KILL, KICK és BAN blokkok;
- az ALL blokkok kihagyják-e a saját fiókot;
- a parancsok közötti várakozás egy 0,05–10,0 másodperces csúszkával.

A **Futtatás most** gomb azonnal sorba állítja a programot. Ha kick vagy ban blokk is van benne, a gombot a megerősítéshez másodszor is meg kell nyomni. Automatikus belépésnél a veszélyes blokkok alapból kimaradnak.

## Chatparancsok

- /aop – minden látható online játékos OP-olása.
- /aop editor – a Parancs Blokkok editor megnyitása.
- /aop status – az aktuális állapot megjelenítése.
- /aop auto <true|false> – Automata OP be/ki.
- /aop join-commands <true|false> – Belépési Parancsok be/ki.
- /aop cancel – a várakozó parancssor megszakítása.

## Fontos

- A mod nem kerül meg szerverjogosultságot. Az op, deop, kick és ban csak akkor működik, ha a szerver valóban engedélyezi a belépett fióknak.
- Az ALL blokkok a kliens játékoslistájából dolgoznak; a szerver által elrejtett játékosokat a kliens nem feltétlenül ismeri.
- Online-mode szerveren a mod nem vált és nem hamisít Minecraft-fiókot.
- A KILL, KICK és BAN blokkok valódi szerverparancsokat küldenek. Teszt előtt hagyd bekapcsolva a **Saját fiók: KIHAGY** beállítást.
