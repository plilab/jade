#!/usr/bin/env bash

# This script runs the test for decompiling class-level constructs on multiple .class files.

# To use it:
#   - Compile test java files into .class files if needs be and keep all .class files in a folder and its subfolders
#   - Then, from project root, run `bash run_class_multiple_tests.sh <path to folder containing test files>. The script finds all .class files within the folder and its subfolders
# REMEMBER TO RE-COMPILE .JAVA INTO .CLASS WHENEVER YOU MODIFY TEST JAVA FILES!
# See https://github.com/adamsmd/jade/pull/2 for manual equivalence for a single test

if [ "$#" -ne 1 ]; then
    echo "Error: Wrong number of arguments."
    echo "Usage: bash $0 <path to directory containing test files>"
    exit 1
fi

rm -rf tmp
mkdir tmp

count=0
directory_path=$1

for i in $( find ${directory_path} -name "*.class" -type f ); do

    basename="${i##*/}"
    echo "-----------------------------"
    echo "Decompiling ${i}"

    # Start timer
    start=$SECONDS

    # decompile class files
    # V1: print debug to out.txt
    # ./gradlew run --args="--log=debug decompile ${i} tmp" > out.txt

    # V2: no debug output
    ./gradlew run --args="decompile ${i} tmp"

    echo "Done decompiling ${basename}"
    cd tmp

    # Compile newly-obtained Java file into .class file
    # If there is any dependency, include them with a -classpath parameter instead, for example
    # javac -classpath denpendency1.jar:dependency2.jar:dependency3.jar: "${basename%*.class}.java"
    echo "Recompiling ${basename}"
    javac "${basename%*.class}.java"

    echo "Done recompiling ${basename}"
    echo ""

    # Compare class skeletons of old and new bytecodes
    echo "Diff for ${basename}:"

    echo "    Original class file path: ${i}"
    echo "    Current class file path: tmp/${basename}"

    # Return to root folder
    cd ..

    # Run diff command and capture its output. 
    # This script currently only check if the class skeletons are the same, i.e. output of running javap -p -s on the class files are the same
    javap -p -s ${i} > 1.txt
    javap -p -s tmp/${basename} > 2.txt
    diff_output=$(diff 1.txt 2.txt)
    
    # Add output to file
    if [ -z "$diff_output" ]; then
        echo "No mismatch between ${i} and tmp/${basename}"
    else
        echo "There's mismatch: $diff_output"
    fi

    # Display elapsed time
    elapsed=$((SECONDS - start))
    echo "Time elapsed: $elapsed seconds"

    count=$((count+1))
    echo "Processed $count files"
done

rm -f 1.txt
rm -f 2.txt