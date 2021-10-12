val gsonVersion: String by rootProject.extra

dependencies {
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.rauschig:jarchivelib:1.0.0")
    implementation(project(":orx-noise"))
}

