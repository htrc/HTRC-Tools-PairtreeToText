# HTRC-Tools-PairtreeToText
This application extracts full text from a HT volume stored in Pairtree by concatenating the pages in the correct order,
as specified in the associated METS XML metadata file. A number of helpful API methods are made available so this app
can also be used as a library (whose methods can be invoked from external code)

# Build
* To generate a "fat" executable JAR, run:  
  `sbt assembly`  
  then look for it in `target/scala-2.11/` folder.

  *Note:* you can run the JAR via the usual: `java -jar JARFILE`

* To generate a package that can be invoked via a shell script, run:  
  `sbt stage`  
  then find the result in `target/universal/stage/` folder.

* To generate the JAR package that can be used as a dependency in other projects, run:  
  `sbt package`  
  then look for it in `target/scala-2.11/` folder.
  
# Run
```
pairtree-to-text
  -c, --clean-ids         Specifies whether the IDs are 'clean' or not
  -o, --output  <DIR>     The output folder where the text files will be written
                          to
  -p, --pairtree  <DIR>   The path to the paitree root hierarchy to process
      --help              Show help message
      --version           Show version of this program

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
    <version>4.0.1</version>
</dependency>
```

To use via SBT:  
`libraryDependencies += "org.hathitrust.htrc" %% "pairtree-to-text" % "4.0.1"`


## PairtreeToText API

```
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param metsXmlFile The METS file describing the volume (and its page ordering)
    * @param volZipFile The volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(pairtreeDoc: PairtreeDocument, metsXmlFile: File, volZipFile: File)(implicit codec: Codec): Try[String]
  
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param volZipFile The volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(volZipFile: File)(implicit codec: Codec): Try[String]
                      
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param htid The clean or unclean HT volume ID
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding
    *                         volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId True if `htid` represents a 'clean' ID, False otherwise
    *                  (assumed False if missing)
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(htid: String, pairtreeRootPath: File, isCleanId: Boolean = false)(implicit codec: Codec): Try[String]
  
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param pairtreeDoc The PairtreeDocument representing the volume
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding
    *                         volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return The text content of the volume wrapped in Success,
    *         or Failure if an error occurred
    */
  def pairtreeToText(pairtreeDoc: PairtreeDocument, pairtreeRootPath: File)(implicit codec: Codec): Try[String]
```
