val gsonVersion: String by rootProject.extra

dependencies {
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation(project(":orx-noise"))
}

