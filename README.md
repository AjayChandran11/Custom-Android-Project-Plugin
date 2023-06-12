# Custom-Android-Project-Plugin

This plugin is used to add a new custom project in the Quickstart section of Android Studio. It is written in Intellij IDEA using gradle dependencies and Intellij's
PSI dependencies.

Using this, a custom project can be created, for example,
- By adding the necessary class files required
- By building a custom SDk
- By adding dependencies and repositories

Please try to use the same version of Intellij IDEA and Android Studio to avoid issues.

CustomGradleBuildListener - This listener is used to build the project immediately after the project is created to build all the custom files added.
CustomGradleSyncListener - This listener is used to obtain the necessary packages and file names and add the custom files to our need using psi traversal.
