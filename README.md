# biojava-adam

BioJava and ADAM integration.

### Hacking biojava-adam

Install

 * JDK 1.8 or later, http://openjdk.java.net
 * Apache Maven 3.3.9 or later, http://maven.apache.org
 * Apache Spark 2.1.0 or later, http://spark.apache.org


To build

    $ mvn install


To run

```
$ spark-shell \
    --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
    --conf spark.kryo.registrator=org.biojava.nbio.adam.BiojavaKryoRegistrator \
    --jars target/biojava-adam-5.0.0-SNAPSHOT.jar

Using SPARK_SHELL=/usr/local/bin/spark-shell
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.1.0
      /_/

Using Scala version 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_60)
Type in expressions to have them evaluated.
Type :help for more information.

scala> import org.biojava.nbio.adam.BiojavaAdamContext
import org.biojava.nbio.adam.BiojavaAdamContext

scala> val biojavaContext = new BiojavaAdamContext(sc)
biojavaContext: org.biojava.nbio.adam.BiojavaAdamContext = org.biojava.nbio.adam.BiojavaAdamContext@1e041848

scala> val reads = biojavaContext.loadFastqReads("fastq_sample1.fq")
reads: org.apache.spark.rdd.RDD[org.bdgenomics.formats.avro.Read] = MapPartitionsRDD[1]
  at map at BiojavaAdamContext.java:136

scala> reads.first
res0: org.bdgenomics.formats.avro.Read = {"name": "H06HDADXX130110:2:2116:3345:91806/1", "description":
"H06HDADXX130110:2:2116:3345:91806/1", "alphabet": "DNA", "sequence": "GTTAGGGTTAGGGTTGGGTTAGGGTTAGGGTT
AGGGTTAGGGGTAGGGTTAGGGTTAGGGGTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGGTAGGGCTAGGGTTAAGGGTAGGGTTAGCGAAAGGGCTG
GGGTTAGGGGTGCGGGTACGCGTAGCATTAGGGCTAGAAGTAGGATCTGCAGTGCCTGACCGCGTCTGCGCGGCGACTGCCCAAAGCCTGGGGCCGACTCCAG
GCTGAAGCTCAT", "length": 250, "qualityScores": ">=<=???>?>???=??>>8<?><=2=<===1194<?;:?>>?#3==>########
#######################################################################################################
############################################################################################",
"qualityScoreVariant": "FASTQ_SANGER"}
```
