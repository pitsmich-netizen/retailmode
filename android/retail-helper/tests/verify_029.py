from pathlib import Path
import re
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
application=manifest.find('application')
aliases=[a for a in application.findall('activity-alias') if a.get(ns+'name')=='.LauncherActivity']
assert len(aliases)==1 and aliases[0].get(ns+'enabled')=='false'
assert any(a.get(ns+'name')=='.MainActivity' and a.get(ns+'exported')=='true' for a in application.findall('activity'))
assert 'versionCode = 15' in (root/'app/build.gradle.kts').read_text()
assert 'versionName = "0.2.9"' in (root/'app/build.gradle.kts').read_text()
hider=(root/'app/src/main/java/com/example/retailhelper/CompletionHider.java').read_text()
assert 'COMPONENT_ENABLED_STATE_DISABLED' in hider
assert 'Intent.ACTION_DELETE' not in hider
nav=(root/'app/src/main/java/com/example/retailhelper/AccessibilityNavigator.java').read_text()
assert 'ACTION_ACCESSIBILITY_SETTINGS' in nav
assert 'openExportedInstalledServicesScreen' in nav
print('0.2.9 static invariants OK')
