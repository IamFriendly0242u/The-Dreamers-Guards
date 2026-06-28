# The Dreamers Guards

**The Dreamers Guards** is a high-performance, enterprise-grade anti-cheat and security framework designed to preserve gameplay integrity on Fabric servers. Built specifically for modern multiplayer environments running Minecraft 26.1.2 and Java 25, this mod provides a robust defense against unauthorized modifications, cheat clients, and exploits.

## Key Features

*   **Advanced Client-Side Validation**: Performs real-time, encrypted network authentication between client and server to verify mod integrity.
*   **Proactive Threat Detection**: Automatically detects and blocks blacklisted modifications via a comprehensive signature-based scanner targeting popular utilities like Wurst, Meteor, and LiquidBounce.
*   **Proactive Discord Integration**: Instant, real-time threat reporting via webhooks, featuring dynamic 3D isometric player head rendering for immediate visual identification.
*   **Multi-Phase Punishment System**: A progressive, 4-stage automated enforcement suite featuring anti-evasion logging to handle players attempting to log out during count-downs.
*   **Reliable Management**: Automated configuration backup rotation with hot-reloading capabilities via the integrated `/guards` command suite.

## Technical Specifications

*   **Platform**: Fabric Loader
*   **Minecraft Version**: ~26.1.2
*   **Java**: 25+
*   **Required Dependency**: The Dreamers Lib

## Installation & Requirements

This mod requires **The Dreamers Lib** to be installed on both the server and client environment. Gaining access to the server without the library companion framework will result in an immediate connection termination.


## License

This project is protected under **All Rights Reserved** license. No part of this software may be copied, modified, merged, published, or redistributed in any form without prior written permission.

**Built by IamFriendly0242u**

### Developer Setup

Add the following to your `build.gradle` to compile against the companion library framework:

```groovy
dependencies {
	minecraft "com.mojang:minecraft:\${project.minecraft_version}"
	implementation "net.fabricmc:fabric-loader:\${project.loader_version}"
	implementation "net.fabricmc.fabric-api:fabric-api:\${project.fabric_api_version}"

	modImplementation files("../TheDreamersLib/build/libs/thedreamers_lib-1.0.0.jar")
}
