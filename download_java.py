import json
import urllib.request
import tarfile
import os

# 下载Java 17
print("Downloading Java 17...")
url = "https://api.adoptium.net/v3/assets/latest/17/hotspot?os=macos&architecture=x86-64&image_type=jre"
try:
    with urllib.request.urlopen(url) as response:
        data = json.loads(response.read().decode())
        if data:
            download_url = data[0]['binary']['package']['link']
            print(f"Downloading from: {download_url}")
            urllib.request.urlretrieve(download_url, "openjdk17.tar.gz")
            print("Download completed!")
        else:
            print("No Java 17 found")
except Exception as e:
    print(f"Error: {e}")

# 解压Java
print("Extracting Java...")
try:
    with tarfile.open("openjdk17.tar.gz", "r:gz") as tar:
        tar.extractall()
    print("Extraction completed!")
except Exception as e:
    print(f"Error: {e}")

# 查找解压后的目录
print("Finding Java directory...")
try:
    java_dir = None
    for item in os.listdir("."):
        if item.startswith("jdk") or item.startswith("openjdk"):
            java_dir = item
            break
    if java_dir:
        print(f"Java directory found: {java_dir}")
        # 设置JAVA_HOME环境变量
        print(f"export JAVA_HOME=\"{os.path.abspath(java_dir)}\"")
        print(f"export PATH=\"$JAVA_HOME/bin:$PATH\"")
    else:
        print("Java directory not found")
except Exception as e:
    print(f"Error: {e}")
