# orx-git-archiver

An extension that hooks into `Program.requestAssets` to commit
changed code to Git and provide filenames based on the commit hash.

## How do I use it?

```kotlin
application {
    program {
        extend(GitArchiver()) {


        }
        extend(Screenshots())
    }
}
```
Now when a screenshot is taken, first all uncommitted code is committed to git.
The screenshot is saved with the first 7 characters of the commit hash in the filename.
