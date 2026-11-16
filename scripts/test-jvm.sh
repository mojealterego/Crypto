#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
mkdir -p build/test-classes
javac -Xlint:all -d build/test-classes \
 app/src/main/java/pl/omega/swarm/{CommandCodec,GgufHeader,HuggingFacePaths,ModelFiles,HuggingFaceDownloader}.java \
 tests/{CommandCodecTest,GgufLibraryTest,HfDownloadPolicyTest}.java
java -cp build/test-classes CommandCodecTest
java -cp build/test-classes GgufLibraryTest
java -cp build/test-classes HfDownloadPolicyTest
