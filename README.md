# AOP Tools – Meteor addon, Fabric 1.21.11

## Mi ez?

Az AOP Tools egy Minecraft 1.21.11-es Fabric mod és Meteor Client addon saját szerverek adminisztrálásához és játékos tréfák készítéséhez. A kereshető, Scratch-szerű editorban színes blokkokból állíthatsz össze OP, deOP, kill, kick, ban és láb-alá blokkot helyező műveleteket. A programokat elnevezett presetekbe mentheted, majd bindről vagy opcionálisan belépéskor futtathatod.

**English:** AOP Tools is a Minecraft 1.21.11 Fabric mod and Meteor Client addon for authorized server administration and player pranks. Build reusable presets in a searchable Scratch-style editor, then run OP, deOP, kill, kick, ban and continuous block-under-player actions from a keybind or optional join autorun.

GitHub release: https://github.com/NC480/AOP-Tools/releases/latest

## Követelmények

- Minecraft Java Edition 1.21.11
- Java 21
- Fabric Loader 0.20.1
- Fabric API 0.141.6+1.21.11
- Meteor Client 1.21.11

## Telepítés

1. Töltsd le az aop-tools-1.8.0.jar fájlt a GitHub Releases oldalról, vagy fordítsd le forrásból.
2. Másold a JAR-t a Minecraft mods mappájába.
3. Ugyanott legyen a Fabric API és a Minecraft 1.21.11-hez készült Meteor Client.
4. Indítsd el a Fabric-profilt.
5. Nyisd meg a Meteor GUI-t (Right Shift), majd az **AOP Manager** kategóriát.

## Fordítás forrásból

Java 21 szükséges.

~~~powershell
.\gradlew.bat clean build
~~~

A játékba telepíthető JAR a build/libs/aop-tools-1.8.0.jar fájl lesz. A repository GitHub Actions workflow-ja minden pushnál és pull requestnél ugyanezt a buildet futtatja.

## Optimalizált kliens és kompatibilitás

Az AOP Tools kliensoldali Meteor addon, nem módosítja a világ renderelését, a chunkkezelést vagy a hálózati protokollt, ezért együtt használható az alábbi Fabric 1.21.11-es optimalizáló és diagnosztikai modokkal. Ezek opcionálisak: az AOP Tools nélkülük is működik.

- **Sodium** – gyorsabb kliensoldali renderelés és jobb képkockaidő.
- **Entity Culling** – a nem látható entitások és blokkentitások renderelésének kihagyása.
- **FerriteCore** – memóriahasználat csökkentése.
- **Lithium** – játékmenet- és ticklogika optimalizálása.
- **ImmediatelyFast** – GUI, HUD, szöveg, entitás és immediate-mode renderelés gyorsítása.
- **Krypton** – hálózati verem optimalizálása.
- **Clumps** – XP-gömbök összevonása; modern Minecraft-verziókon a tényleges hatásához a szerverre is telepíteni kell.
- **Fast IP Ping** – a szerverlista pingelésének gyorsítása.
- **FastQuit** – egyjátékos világ mentése a háttérben, gyorsabb visszatérés a főmenübe.
- **spark** – teljesítményprofilozó; mérésre és hibakeresésre szolgál, önmagában nem növeli az FPS-t.
- **ExploitPreventer** – ismert kliensoldali támadások ellen véd, többek között blokkolja a helyi hálózatot célzó resource-pack URL-eket és elkülöníti a resource-pack cache-t a fiókok között a fingerprinting csökkentésére.

A FastQuit kötelező függősége, a **Cloth Config**, szintén szükséges. OptiFine/OptiFabric nincs a javasolt összeállításban, mert ütközhet a Sodium és az ImmediatelyFast működésével.

### Ellenőrzött Huncut Optimized összeállítás

| Komponens | Verzió |
|---|---|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.141.6+1.21.11 |
| Meteor Client | 1.21.11 build 86 |
| AOP Tools | 1.8.0 |
| Sodium | 0.8.14 |
| Entity Culling | 1.12.1 |
| FerriteCore | 8.2.0 |
| Lithium | 0.21.4 |
| ImmediatelyFast | 1.14.3 |
| Krypton | 0.2.10 |
| Clumps | 29.0.0.1 |
| Fast IP Ping | 1.0.12 |
| FastQuit | 3.1.3 |
| spark | 1.10.170 |
| Cloth Config | 21.11.153 |
| ExploitPreventer | 1.2.0 |

### Importálható Prism Launcher-csomag

A GitHub Release oldalról letölthető [`aop-tools-huncut-optimized-1.8.0.mrpack`](https://github.com/NC480/AOP-Tools/releases/download/v1.8.0/aop-tools-huncut-optimized-1.8.0.mrpack) közvetlenül importálható a Prism Launcher **Add Instance → Import** menüpontjával. A csomag nem tartalmaz fiókot, UUID-t, szerverlistát, világot, naplót, képernyőképet, Meteor-profilt, AOP presetet, helyi elérési utat vagy játékidőadatot. Csak a Minecraft/Fabric verziót és a hivatalos, hash-ellenőrzött modletöltéseket írja le.

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
