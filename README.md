# HTRC-Tools-PairtreeToText
Tool that extracts full text from a HathiTrust volume stored in Pairtree by concatenating the pages 
in the correct order, optionally performing additional post-processing to identify running headers, 
fix end-of-line hyphenation, and reformat the text.

# Build
* To generate an executable package that can be invoked via a shell script, run:  
  `sbt app/stage`  
  then find the result in `app/target/universal/stage/` folder.

* To generate the JAR package that can be used as a dependency in other projects, run:  
  `sbt lib/package`  
  then look for it in `lib/target/scala-2.11/` folder.
  
# Run
The following command line arguments are available:
```
pairtree-to-text
HathiTrust Research Center
  -p, --pairtree  <DIR>       The path to the pairtree root hierarchy to process
  -o, --output  <DIR>         The folder where the output will be written to
  -b, --body-only             Remove running headers/footers from the pages
                              before concatenation
  -h, --fix-hyphenation       Remove hyphenation for words occurring at the end
                              of a line
  -l, --para-lines            Join lines such that each paragraph is on a single
                              line

      --clean-ids             Specifies whether the IDs are 'clean' or not
  -c, --codec  <CODEC>        The codec to use for reading the volume
  -n, --num-partitions  <N>   The number of partitions to split the input set of
                              HT IDs into, for increased parallelism
      --spark-log  <FILE>     Where to write logging output from Spark to
  -w, --write-pages           Writes each page as a separate text file
      --help                  Show help message
      --version               Show version of this program

 trailing arguments:
  htids (not required)   The file containing the list of HT IDs to process (if
                         omitted, will read from stdin)
```

# APIs

To use via Maven:
```
<dependency>
    <groupId>org.hathitrust.htrc</groupId>
    <artifactId>pairtree-to-text_2.11</artifactId>
    <version>5.2.0-SNAPSHOT</version>
</dependency>
```

To use via SBT:  
`libraryDependencies += "org.hathitrust.htrc" %% "pairtree-to-text" % "5.2.0-SNAPSHOT"`
