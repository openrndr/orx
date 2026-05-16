# orx-git-archive-gradle

A Gradle plugin that turns a git history and a `screenshots` directory into a Markdown file.
Works in combination with [orx-git-archiver](../orx-git-archiver).

## Enabling the plugin

`openrndr-template` may enable this plugin by default, but you can also add it manually. 

Open `build.gradle.kts` and make sure the following `alias` is inside the `plugins` block:
```kotlin
plugins {
    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
}
```
Then, somewhere in `build.gradle.kts` register the plugin like this:
```kotlin
tasks.register<org.openrndr.extra.gitarchiver.GitArchiveToMarkdown>("gitArchiveToMarkDown") {
    historySize.set(20)
}
```
## Usage

1. Use `orx-git-archiver` to archive screenshots: 
   1. Add `extend(GitArchiver())` and `extend(Screenshots())` to your program.
   2. Press `space` to take a screenshot AND make changes to your program. Do this a few times to 
      commit the changing source code and collect screenshots of each version. 
      While using `GitArchiver` the screenshots will have the Git commit id in their filename.
2. Run the `gitArchiveToMarkDown` Gradle task provided by `orx-git-archiver-gradle`. This will generate a `build/git-archive-markdown/README.md` file
   including up to 20 screenshots by default, and the source code of each version. The plugin does this by 
   reading the `screenshots` directory and the Git history.