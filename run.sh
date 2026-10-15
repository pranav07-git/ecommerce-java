#!/bin/sh
# Compile and start the console app (needs only a JDK, Java 17+).
cd "$(dirname "$0")" || exit 1
mkdir -p out && javac -d out $(find src/main -name '*.java') && java -cp out com.ecommerce.Main
