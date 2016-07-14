# bpmdetect
A tool to detect the tempo of a .wav file.  

---
## Usage
To get the tempo of a piece of music, use the static method `BPMDetect.detectBPM(file)`.  
Please note that the input file has to be in .wav format.

---
## Installation
This tool is based on the **BeatRoot** software, which allows for excellent onset time detection.

In order to compile **bpmdetect** with maven, you'll have download version 0.5.8 of beatroot from [here](http://www.eecs.qmul.ac.uk/~simond/beatroot/) and use maven to install it into a local repository:

`mvn install:install-file -Dfile=path/to/beatroot-0.5.8.jar -DgroupId=at.ofai.music -DartifactId=beatroot -Dversion=0.5.8 -Dpackaging=jar`

---

I don't have a maven repository for **bpmdetect** yet, so to use it as a library, you'll have to download the latest version from the distributions on GitHub.
