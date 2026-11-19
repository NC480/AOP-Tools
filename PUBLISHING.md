# GitHub publikálási ellenőrzőlista

## Első feltöltés

1. Inicializáld a mappát Git-repozitóriumként.
2. Kapcsold hozzá a már létrehozott GitHub repository URL-jét.
3. Ellenőrizd, hogy a paper-*.jar, a build és a .gradle mappa nincs a feltöltendő fájlok között.
4. Készíts commitot és pushold az alapértelmezett branchre.
5. Ellenőrizd a GitHub Actions Build futását.

## Kiadás

1. Frissítsd a mod_version értékét a gradle.properties fájlban.
2. Frissítsd a CHANGELOG.md fájlt.
3. Futtasd:

~~~powershell
.\gradlew.bat clean build
~~~

4. Hozz létre a verzióval egyező taget, például v1.4.1.
5. GitHub Release-ben töltsd fel a build/libs/aop-tools-1.4.1.jar fájlt.
6. A sources JAR fejlesztőknek opcionálisan csatolható, de a játékhoz a sima remappelt JAR kell.

Soha ne töltsd fel a Paper szerver JAR-ját vagy a saját Minecraft/Meteor konfigurációdat.
