# Library consumer rules cover Room, kotlinx.serialization, Ktor and OkHttp.
# Add app-specific keep rules here, as narrow as possible.

# Stack traces from release builds keep their line numbers; retrace them with the build's mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
