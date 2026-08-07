
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

dependencies {
    // Lua scripting engine for pattern scripts
    implementation("org.luaj:luaj-jse:3.0.1")
    
    // JUnit for testing
    testImplementation("junit:junit:4.13.2")
}
