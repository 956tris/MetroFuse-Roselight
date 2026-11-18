"""Validate manifests and stage versioned native plugin packages for CI."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("--validate-only", action="store_true")
args = parser.parse_args()
manifest = json.loads(Path("native/manifest.json").read_text())
version = manifest["version"]
if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
    raise SystemExit("Native manifest must contain a release version")
requested = os.environ.get("REQUESTED_VERSION", "")
if requested and requested != version:
    raise SystemExit("Requested version does not match native/manifest.json")
if os.environ.get("RELEASE_EVENT") == "push" and os.environ.get("RELEASE_REF") != "v" + version:
    raise SystemExit("Release tag does not match native/manifest.json")
if os.environ.get("GITHUB_OUTPUT"):
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write("version=" + version + "\n")
if args.validate_only:
    print("Validated Roselight " + version)
    raise SystemExit(0)

out = Path("release-assets")
out.mkdir(exist_ok=True)
checksums = []
for extension, manifest_path in [("apk", "assets/metrofuse/manifest.json"), ("jar", "manifest.json")]:
    source = Path("build/outputs/plugins/roselight." + extension)
    with zipfile.ZipFile(source) as package:
        if json.loads(package.read(manifest_path)) != manifest:
            raise SystemExit("Packaged manifest differs from source: " + str(source))
    target = out / ("roselight-" + version + "." + extension)
    shutil.copyfile(source, target)
    checksums.append(hashlib.sha256(target.read_bytes()).hexdigest() + "  " + target.name)

def revision(directory):
    return subprocess.check_output(["git", "-C", str(directory), "rev-parse", "HEAD"], text=True).strip()

metadata = {"version": version, "sourceCommit": revision(Path.cwd()), "composeAbi": manifest["composeAbi"], "api": manifest["api"]}
if Path("host-sdk/.git").exists():
    metadata["hostSdkCommit"] = revision(Path("host-sdk"))
info = out / ("build-info-" + version + ".json")
info.write_text(json.dumps(metadata, indent=2) + "\n")
checksums.append(hashlib.sha256(info.read_bytes()).hexdigest() + "  " + info.name)
(out / ("SHA256SUMS-" + version + ".txt")).write_text("\n".join(checksums) + "\n")
print("Prepared versioned APK/JAR packages for " + version)
