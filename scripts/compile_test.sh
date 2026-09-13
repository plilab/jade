find src/test/resources -name "*.java" -exec sh -c '
  for f do
    name=$(basename "$f" .java)
    mkdir -p "src/test/classFiles/$name"
    javac -d "src/test/classFiles/$name" "$f"
  done
' sh {} +