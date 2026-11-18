"""Validate the native plugin boundary without requiring a running host."""
import json
import struct
import sys
import zipfile
from pathlib import Path


def dex_classes(dex):
    string_count, string_offset = struct.unpack_from('<II', dex, 56)
    type_count, type_offset = struct.unpack_from('<II', dex, 64)
    class_count, class_offset = struct.unpack_from('<II', dex, 96)
    strings = []
    for index in range(string_count):
        offset = struct.unpack_from('<I', dex, string_offset + index * 4)[0]
        while dex[offset] & 128:
            offset += 1
        offset += 1
        strings.append(dex[offset:dex.index(b'\0', offset)].decode('utf-8', errors='replace'))
    types = [strings[struct.unpack_from('<I', dex, type_offset + index * 4)[0]] for index in range(type_count)]
    return types, [types[struct.unpack_from('<I', dex, class_offset + index * 32)[0]] for index in range(class_count)]


def check(apk):
    with zipfile.ZipFile(apk) as package:
        names = package.namelist()
        assert len(names) == len(set(names)), 'Duplicate APK entries'
        manifest = json.loads(package.read('assets/metrofuse/manifest.json'))
        assert manifest['runtime'] == 'kotlin'
        assert manifest['entryClass'] == 'dev.metrofuse.roselight.RoselightPlugin'
        assert 'AndroidManifest.xml' in names and 'classes.dex' in names
        assert not any(name.endswith(('.so', '.class', '.jar')) for name in names), 'Host or desktop dependencies bundled'
        dex = [dex_classes(package.read(entry)) for entry in names if entry.endswith('.dex')]
        references = [name for types, _ in dex for name in types]
        desktop_references = [name for name in references if name.endswith('_skikoKt;') or name.startswith('Lorg/jetbrains/skiko/')]
        assert not desktop_references, f'Desktop-only API references in Android plugin: {desktop_references}'
        classes = [name for _, definitions in dex for name in definitions]
        assert classes
        unexpected = [name for name in classes if not name.startswith('Ldev/metrofuse/roselight/') and name != 'Lcom/android/tools/r8/annotations/LambdaMethod;']
        assert not unexpected, f'Non-plugin class definitions bundled: {unexpected}'
        assert 'Ldev/metrofuse/roselight/RoselightPlugin;' in classes
        print(f'{apk.name}: {len(classes)} plugin classes; no bundled host/runtime classes.')


if __name__ == '__main__':
    check(Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent.parent / 'build/outputs/plugins/roselight.apk')
