# MineFeaturesLines

## First steps

* Clone this repository: `git clone https://github.com/GabrielaMichelon/MineFeaturesLines.git`
* Clone the git reposiritory of the target sytems in a folder on your computer (as an example, LibSSH git repository: https://gitlab.com/libssh/libssh-mirror.git)


## Requires

* JDK 8
* [Gradle](http://gradle.org/ "Gradle") &#8805; 4.4 as build system

You can add jdk and gradle as environmental variables to make easier the script execution in the command line:

* Parameters
  - **First paramenter**: the Git project folder
  - **Second paramenter**: the results folder
  - **Third parameter**: the Git commit hash to analyze

* Type the following command in a command line
  - gradle run -Pmyargs='\<folder Git repository>','\<results folder>','\<Git commit hash>'
