#!/bin/sh
# Compiles the project and starts it. Extra arguments are passed to Main,
# e.g.  ./run.sh console     or     ./run.sh token
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -sourcepath src -d out src/Main.java || exit 1
java -cp out Main "$@"
