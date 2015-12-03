# HTRC-Tools-PairtreeToText
This application extracts full text from a HT volume stored in Pairtree by concatenating the pages in the correct order,
as specified in the associated METS XML metadata file. A number of helpful API methods are made available so this app
can also be used as a library (whose methods can be invoked from external code)

## Building
To generate a package that can be invoked via a shell script:

`sbt stage`

then find the result in `target/universal/stage/`

Alternatively, to generate a "fat" runnable JAR:

`sbt assembly`

then find the result as:
`target/scala-2.11/pairtree-to-text-assembly-1.0-SNAPSHOT.jar`

you can run the JAR via the usual: `java -jar JARFILE`

## Usage
```
pairtree-to-text 1.0-SNAPSHOT
edu.illinois.i3.htrc.apps
  -c, --clean-ids         Specifies whether the IDs are 'clean' or not
  -o, --output  <arg>     The output path where the text files will be written to
  -p, --pairtree  <arg>   The path to the paitree root hierarchy to process
      --help              Show help message
      --version           Show version of this program

 trailing arguments:
  htids (not required)   The list of HT IDs to process
```

## API
The list of API methods available:

```
  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param metsXmlFile The METS file describing the volume (and its page ordering)
    * @param volZipFile The volume ZIP file
    * @param codec The codec to use for encoding and decoding the text (implicit)
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(metsXmlFile: File, volZipFile: File)(implicit codec: Codec): (PairtreeDocument, String)

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param volZipFile The volume ZIP file
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(volZipFile: File): (PairtreeDocument, String)

  /**
    * Retrieve full text (concatenated pages) from HT volume
    *
    * @param htid The clean or unclean HT volume ID
    * @param pairtreeRootPath The root of the pairtree folder structure;<br>
    *                         for example for a volume ID mdp.39015039688257, the corresponding volume ZIP file is:<br>
    *                         [pairtreeRootPath]/mdp/pairtree_root/39/01/50/39/68/82/57/39015039688257/39015039688257.zip
    * @param isCleanId True if `htid` represents a 'clean' ID, False otherwise (assumed False if missing)
    * @return A pair representing the volume and its textual content
    */
  def pairtreeToText(htid: String, pairtreeRootPath: File, isCleanId: Boolean = false): (PairtreeDocument, String)

  /**
    * Retrieve the correct page sequence from METS
    *
    * @param metsXml The METS XML
    * @return The sequence of page file names
    */
  def getPageSeq(metsXml: xml.Elem): Seq[String]
```