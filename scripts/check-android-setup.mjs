import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const androidRoot = path.join(root, 'android-client');

const requiredFiles = [
  'build.gradle.kts',
  'settings.gradle.kts',
  'gradle.properties',
  'gradlew',
  'gradlew.bat',
  'gradle/wrapper/gradle-wrapper.properties',
  'gradle/wrapper/gradle-wrapper.jar',
  'app/build.gradle.kts',
  'app/proguard-rules.pro',
  'app/src/main/AndroidManifest.xml',
  'app/src/main/res/layout/activity_game.xml',
  'app/src/main/res/values/themes.xml',
  'app/src/main/res/xml/backup_rules.xml',
  'app/src/main/res/xml/data_extraction_rules.xml'
];

const missing = requiredFiles.filter((relativePath) => !fs.existsSync(path.join(androidRoot, relativePath)));

if (missing.length > 0) {
  console.error('Android setup is incomplete. Missing files:');
  for (const relativePath of missing) {
    console.error(`- android-client/${relativePath}`);
  }
  process.exit(1);
}

console.log('Android setup file check passed.');
