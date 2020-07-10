if(!(Test-Path($env:JAVA_HOME))) {
  Start-FileDownload 'https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14.0.1%2B7.1/OpenJDK14U-jdk_x64_windows_hotspot_14.0.1_7.zip' -FileName 'jdk.zip'
  7z x jdk.zip -obuild\
}
