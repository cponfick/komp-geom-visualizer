# Computational Geometry in Kotlin - Visualisation

This project is a sample application for the [Komp Geom](https://github.com/cponfick/komp-geom) library, which is a
Kotlin multiplatform library for geometric computations.

It is mainly used to visualize computational geometry algorithms and data structures, for debugging purposes.

## Starting the Application

To start the desktop application, run the following command:

```bash
./gradlew jvmRun -DmainClass=io.github.cponfick.MainKt --quiet
```

To start the web application, run the following command:

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```