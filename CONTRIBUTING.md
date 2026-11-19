# Közreműködés

## Fejlesztői környezet

- Java 21
- Minecraft 1.21.11
- Fabric Loader 0.20.1
- Fabric API 0.141.6+1.21.11
- Meteor Client 1.21.11

## Build

Windows:

~~~powershell
.\gradlew.bat clean build
~~~

Linux/macOS:

~~~bash
./gradlew clean build
~~~

A kiadható remappelt JAR a build/libs mappába kerül.

## Pull request

1. Készíts külön branchet.
2. A változtatás legyen egyértelműen körülhatárolt.
3. Futtasd a clean build feladatot.
4. Írd le, mit és hogyan teszteltél.
5. Ne tölts fel Minecraft-, Paper-, Fabric API- vagy Meteor Client JAR-fájlokat.

Csak olyan szerveren tesztelj adminisztrációs parancsokat, amelyhez engedélyed van.
