# Android Signing Lineage

`adaptive-theme-a-to-b.lineage` is Android's public `SigningCertificateLineage`
metadata for `dev.lexip.hecate`.

It was extracted from the Google Play-signed 2.3.0 FOSS transition APK.  
The lineage permits Android to accept an update from the legacy Google Play signing key (A) to the developer-controlled signing key (B).
It contains public certificates and signature metadata only.

Signer certificates:

| Signer | SHA-256 certificate fingerprint |
| --- | --- |
| Legacy Google Play key (A) | `AC:EF:12:60:DC:67:02:72:AA:52:16:43:5E:85:FC:CA:F3:3D:47:1F:94:39:CD:38:F6:25:2B:42:AC:56:2C:7D` |
| Developer signing key (B) | `1A:21:E8:10:4B:CE:8E:90:A5:91:B5:52:7D:01:A5:32:0B:2A:0D:46:FC:8F:97:EA:3D:5A:F4:22:5C:9A:FD:07` |

The expected SHA-256 of the lineage file is
`05B08C36D1F7C30E0EE28BC8A960DEADBD4C9F93A0D37B2B541A707D1386CA60`.

This is not needed to reproduce an unsigned build or to copy the signature from an upstream signed APK.  
It is committed here only for GitHub actions.
