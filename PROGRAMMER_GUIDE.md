Moved, now at:
[https://erddap.github.io/docs/contributing/programmer-guide](https://erddap.github.io/docs/contributing/programmer-guide)

### Troubleshooting Setup
If `mvn compile` fails with "Archive is not a ZIP archive" for `etopo1_ice_g_i2.zip`:
1. The download likely timed out and corrupted the file.
2. Delete `WEB-INF/ref/etopo1_ice_g_i2.zip` and retry.
3. Workaround: Use `-DskipResourceDownload` to skip the download.