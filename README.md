# robovm-sdk-builder
iOS SDK files generator from iOS dyld cache file

Project contains two modules:
- `core` pure Java module that can be run as Java application;
- `robovm-sdk-builder` ios application that to be run with RoboVM

Goal of this project is to generate `Xcode` folder alternative for RoboVM which allows to use it on Linux/Windows platform without breaking Apple license.
RoboVM requires SDK files on Linux/Windows platform to link object files into application binary. Lucky for us Apple uses .tbl stub files instead library binaries for a while:
> TAPI is a __T__ext-based __A__pplication __P__rogramming __I__nterface. It replaces the Mach-O Dynamic Library Stub files in Apple's SDKs to reduce SDK size even further.
>
> The text-based dynamic library stub file format (.tbd) is a human readable and editable YAML text file. The TAPI projects uses the LLVM YAML parser to read those files and provides this functionality to the linker as a dynamic library.

Once started on device there are two options:
- generate SDK `in vitro` directly on device;
- or start web server to download files from device and use Java module to hack things on host pc.



