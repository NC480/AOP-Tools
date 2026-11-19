# AOP Tools – Meteor addon, Fabric 1.21.11

Meteor Client addon saját szerveren használható admin- és trolleszközökhöz, Scratch-szerű blokkeditorral.

**AOP Tools is a Minecraft 1.21.11 Fabric mod and Meteor Client addon with a searchable Scratch-style command-block editor for authorized server administration.** It supports OP, deOP, kill, kick, ban and continuous block-under-player workflows with ALL and named-player (XY) variants.

GitHub release: https://github.com/NC480/AOP-Tools/releases/latest

## Követelmények

- Minecraft Java Edition 1.21.11
- Java 21
- Fabric Loader 0.20.1
- Fabric API 0.141.6+1.21.11
- Meteor Client 1.21.11

## Telepítés

1. Töltsd le az aop-tools-1.6.0.jar fájlt a GitHub Releases oldalról, vagy fordítsd le forrásból.
2. Másold a JAR-t a Minecraft mods mappájába.
3. Ugyanott legyen a Fabric API és a Minecraft 1.21.11-hez készült Meteor Client.
4. Indítsd el a Fabric-profilt.
5. Nyisd meg a Meteor GUI-t (Right Shift), majd az **AOP Manager** kategóriát.

## Fordítás forrásból

Java 21 szükséges.

~~~powershell
.\gradlew.bat clean build
~~~

A játékba telepíthető JAR a build/libs/aop-tools-1.6.0.jar fájl lesz. A repository GitHub Actions workflow-ja minden pushnál és pull requestnél ugyanezt a buildet futtatja.

### Prism Launcher fejlesztői példány

A helyi példány neve huncut. Fordítás és telepítés, a játék elindítása nélkül:

~~~powershell
$env:JAVA_HOME = "$env:APPDATA\PrismLauncher\java\java-runtime-delta"
.\gradlew.bat clean deployToHuncut
~~~

## AOP Manager menüpontok

### OP Mindenkinek

Egy kattintásra OP-olja a kliens játékoslistáján látható összes játékost, majd kikapcsol.

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
- BLOKK ALL LÁBA ALÁ – folyamatosan a játékosok alá helyezi a megadott blokkot.
- BLOKK XY LÁBA ALÁ – folyamatosan a megadott játékos alá helyezi a blokkot.

A bal oldali kereső szűri a blokkpalettát. Az XY mezőben a játékosnév, a blokkmezőben például `tnt`, `minecraft:tnt` vagy más érvényes blokkazonosító adható meg. A paletta szélesebb, a blokkok kisebbek, így egyszerre több látható. A program fentről lefelé fut; a blokkok áthúzással átrendezhetők, jobb kattintással törölhetők, az egérgörgővel pedig mindkét oszlop mozgatható.

### Preset-konténerek

A jobb oldali terület egy elnevezhető preset-konténer: a belehúzott blokkok együtt alkotnak egy presetet. A felső sor `<` és `>` gombjaival válthatsz közöttük, a `+ Új` gombbal új konténert készíthetsz, majd az **Aktívvá tesz** gombbal kiválaszthatod, melyik fusson a bind megnyomásakor. Az **Autorun** kapcsoló belépés után automatikusan az aktív presetet indítja el. A korábbi egyetlen blokkprogram első megnyitáskor automatikusan az `Alap` presetbe kerül.

A Parancs Blokkok modulhoz beállított billentyű- vagy egérbind játék közben azonnal lefuttatja a mentett programot, és nem nyitja meg az editort. A Meteor csempére kattintás és az /aop editor parancs továbbra is a szerkesztőt nyitja meg.

Az editorban beállítható:

- az ALL blokkok kihagyják-e a saját fiókot;
- a parancsok, illetve a folyamatos láb-alá blokkok ismétlési ideje 0,05–10,0 másodperces csúszkával.

A **Futtatás / leállítás** gomb elindítja a programot. Ha KILL, KICK vagy BAN blokk is van benne, másodszor is rá kell kattintani. A láb-alá blokkok addig ismétlődnek, amíg ugyanazzal a binddel, a gombbal vagy az `/aop cancel` paranccsal le nem állítod őket.

## Chatparancsok

- /aop – minden látható online játékos OP-olása.
- /aop editor – a Parancs Blokkok editor megnyitása.
- /aop status – az aktuális állapot megjelenítése.
- /aop cancel – a várakozó és folyamatos futtatás leállítása.

## Fontos

- A mod nem kerül meg szerverjogosultságot. A műveletek csak akkor működnek, ha a szerver valóban engedélyezi őket a belépett fióknak.
- Az ALL blokkok a kliens játékoslistájából dolgoznak; a szerver által elrejtett játékosokat a kliens nem feltétlenül ismeri.
- A láb-alá funkció valódi blokkokat módosít a szerveren az `execute` és `setblock` parancsokkal. A TNT blokk elhelyezése önmagában nem gyújtja meg a TNT-t.
- A KILL, KICK és BAN blokkok valódi szerverparancsokat küldenek. Teszt előtt hagyd bekapcsolva a **Saját fiók: KIHAGY** beállítást.
