# Tomato
This is a simple pomodoro timer for macOS, with settings and notifications.

## Features
- 🍅 Simple pomodoro functionality
- 🔔 Notifications
- ⚙️ Simple settings
- 🔮 Undocumented debug feature

## Installation
If you want to install this app, you can do it several ways:
- Download the latest release from the releases section...
- ...or clone the repository and build it yourself

## Building
First ensure that you have all the following dependencies installed:
- macOS
- Xcode CLT or Xcode
- JDK

Then clone the repository using `git` command and `cd` to it
```zsh
git clone https://github.com/m0untain-li0n/Tomato.git
```

Compile the runtime
```zsh
jlink --add-modules java.base,java.desktop,java.logging --output "Runtime" --compress=zip-9
```
Then you can use `make` to build either JAR or application bundle
```zsh
make && make jar
make app
```
