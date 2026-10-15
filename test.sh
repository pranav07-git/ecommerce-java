#!/bin/sh
# Compile and run the dependency-free self-test.
cd "$(dirname "$0")" || exit 1
mkdir -p out && javac -d out $(find src -name '*.java') && java -cp out com.ecommerce.SelfTest
