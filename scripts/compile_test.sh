find src/test/resources/decompilation -name "*.java" -exec sh -c '
  for f do
    name=$(basename "$f" .java)
    mkdir -p "src/test/resources/classFiles/$name"
    javac -d "src/test/resources/classFiles/$name" "$f"
  done
' sh {} +