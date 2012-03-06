
  NERDF: Named Entities Recognition (NER) + RDF
  =============================================

  This is a small demo to perform Named Entities Recognition driven by a
  gazeteer built from an RDF dataset.

  If you want to try it, here is how you can checkout and compile it:

    cd /tmp
    git clone git://github.com/castagna/nerdf.git
    cd /tmp/nerdf
    mvn package

  Load some data (for example the data.nt file included in NERDF) into 
  TDB and point NERDF to it (via standard <init-param> in 
  src/main/webapp/WEB-INFO/web.xml, by default NERDF looks into
  /tmp/nerdf/target/tdb):

    mkdir -p /tmp/nerdf/target/tdb
    tdbloader --loc /tmp/nerdf/target/tdb data.nt

  Run the demo:
  
    mvn jetty:run
    firefox http://127.0.0.1:8080/index.html
