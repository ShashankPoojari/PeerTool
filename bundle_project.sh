#!/usr/bin/env bash
set -e

ZIP_NAME="peerflow-android-project.zip"
echo "Creating production clean bundle: $ZIP_NAME..."

# Remove previous zip if exists
rm -f "$ZIP_NAME"

# Archive all project source code, assets, gradle configurations, and GitHub workflow
zip -r "$ZIP_NAME" . \
  -x "*.gradle/*" \
  -x ".gradle/*" \
  -x "build/*" \
  -x "*/build/*" \
  -x "app/build/*" \
  -x "*.git/*" \
  -x ".git/*" \
  -x "*.apk" \
  -x "*.aab" \
  -x "*.zip"

echo "Successfully created $ZIP_NAME ("$(ls -lh "$ZIP_NAME" | awk '{print $5}')")"
echo "When uploaded to GitHub, the workflow at .github/workflows/build-apk.yml will automatically build and publish the APK!"
