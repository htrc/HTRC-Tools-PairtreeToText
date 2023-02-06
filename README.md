[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/htrc/HTRC-Tools-PairtreeToText/ci.yml?branch=develop)](https://github.com/htrc/HTRC-Tools-PairtreeToText/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/htrc/HTRC-Tools-PairtreeToText/branch/develop/graph/badge.svg?token=ZFB6X3AKGV)](https://codecov.io/gh/htrc/HTRC-Tools-PairtreeToText)
[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/htrc/HTRC-Tools-PairtreeToText?include_prereleases&sort=semver)](https://github.com/htrc/HTRC-Tools-PairtreeToText/releases/latest)

# HTRC-Tools-PairtreeToText
Tool that extracts full text from a HathiTrust volume stored in Pairtree by concatenating the pages 
in the correct order, optionally performing additional post-processing to identify running headers, 
fix end-of-line hyphenation, and reformat the text.

# Build
`sbt dist`

then find the built package in the `target/universal/` folder.

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