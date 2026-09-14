# First Kotlin App: Hello, World!

This guide creates and runs a small Kotlin program on Windows.

## 1. Install Java

Kotlin needs a Java Development Kit (JDK).

1. Install a recent JDK, such as [Eclipse Temurin](https://adoptium.net/temurin/releases/).
2. During installation, allow it to set `JAVA_HOME` if that option is available.
3. Open a new PowerShell window and verify Java:

```powershell
java -version
```

You should see a Java version printed.

## 2. Install Kotlin

Install the Kotlin compiler by following the Windows instructions in the [official Kotlin documentation](https://kotlinlang.org/docs/command-line.html).

After installation, open a new PowerShell window and verify Kotlin:

```powershell
kotlinc -version
```

You should see a Kotlin compiler version printed.

## 3. Create the source file

In PowerShell, move into this project folder:

```powershell
cd c:\all-projects\first-kotlin-app
```

Create a file named `Main.kt` in VS Code with this code:

```kotlin
fun main() {
    println("Hello, World!")
}
```

Save the file.

## 4. Compile the program

Run this command in the project folder:

```powershell
kotlinc Main.kt -include-runtime -d hello.jar
```

This compiles `Main.kt` into an executable Java archive named `hello.jar`.

### Command breakdown

- `kotlinc` = the Kotlin compiler
- `Main.kt` = the source file to compile
- `-include-runtime` = includes the Kotlin runtime in the generated JAR so it can run without separately installing Kotlin on the target machine
- `-d hello.jar` = writes the compiled output to a file named `hello.jar`

### What is a JAR file?

A JAR (Java Archive) file is a package that contains compiled Java/Kotlin classes and other resources. It is similar to a ZIP file, but it is designed for Java programs.

In this project, `hello.jar` is the compiled version of your Kotlin application. After building it, you can run it with Java:

```powershell
java -jar hello.jar
```

This creates a runnable JAR file that can be launched with `java -jar hello.jar`.

## 5. Run the program

```powershell
java -jar hello.jar
```

Expected output:

```text
Hello, World!
```

## 6. Try changing it

Change the message in `Main.kt`, for example:

```kotlin
fun main() {
    println("Hello from my first Kotlin app!")
}
```

Compile and run it again:

```powershell
kotlinc Main.kt -include-runtime -d hello.jar
java -jar hello.jar
```

## Troubleshooting

- If `java` is not recognized, close and reopen PowerShell after installing the JDK.
- If `kotlinc` is not recognized, check that the Kotlin compiler's `bin` folder is on your `PATH`, then open a new PowerShell window.
- Make sure you run the commands from `c:\all-projects\first-kotlin-app` and that the file is named exactly `Main.kt`.
