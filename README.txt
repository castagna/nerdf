
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

  This is just a small, quick and dirty, proof of concept (and the name
  isn't great on purpose), there are many (and much better) similar services
  out there, for example:

   - http://www.opencalais.com/
   - http://www.zemanta.com/
   - http://tagme.di.unipi.it/
   - http://dbpedia.org/spotlight
   - ...

  But, they don't give you the source code and they do not work with *your*
  data. ;-)

