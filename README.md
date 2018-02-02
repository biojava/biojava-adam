# biojava-adam

BioJava and ADAM integration.

### Hacking biojava-adam

Install

 * JDK 1.8 or later, http://openjdk.java.net
 * Apache Maven 3.3.9 or later, http://maven.apache.org
 * Apache Spark 2.2.1 or later, http://spark.apache.org


To build

    $ mvn install


To run

```
$ spark-shell \
    --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
    --conf spark.kryo.registrator=org.biojava.nbio.adam.BiojavaKryoRegistrator \
    --jars target/biojava-adam-5.0.0-SNAPSHOT.jar

Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.2.1
      /_/

Using Scala version 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_111)
Type in expressions to have them evaluated.
Type :help for more information.

scala> import org.biojava.nbio.adam.BiojavaAdamContext
import org.biojava.nbio.adam.BiojavaAdamContext

scala> val biojavaContext = new BiojavaAdamContext(sc)
biojavaContext: org.biojava.nbio.adam.BiojavaAdamContext = org.biojava.nbio.adam.BiojavaAdamContext@1e041848

scala> val reads = biojavaContext.loadFastqReads("fastq_sample1.fq")
reads: org.bdgenomics.adam.rdd.sequence.ReadRDD = ReadRDD(MapPartitionsRDD[1] at map at BiojavaAdamContext.java:180,SequenceDictionary{
H06HDADXX130110:1:2103:11970:57672/2->250
H06HDADXX130110:2:2116:3345:91806/2->250
H06HDADXX130110:1:2103:11970:57672/1->250
H06HDADXX130110:2:2116:3345:91806/1->250
H06JUADXX130110:1:1108:6424:55322/1->250
H06JUADXX130110:1:1108:6424:55322/2->250})

scala> reads.rdd.first
res0: org.bdgenomics.formats.avro.Read = {"name": "H06HDADXX130110:2:2116:3345:91806/1", "description":
"H06HDADXX130110:2:2116:3345:91806/1", "alphabet": "DNA", "sequence": "GTTAGGGTTAGGGTTGGGTTAGGGTTAGGGTT
AGGGTTAGGGGTAGGGTTAGGGTTAGGGGTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGGTAGGGCTAGGGTTAAGGGTAGGGTTAGCGAAAGGGCTG
GGGTTAGGGGTGCGGGTACGCGTAGCATTAGGGCTAGAAGTAGGATCTGCAGTGCCTGACCGCGTCTGCGCGGCGACTGCCCAAAGCCTGGGGCCGACTCCAG
GCTGAAGCTCAT", "length": 250, "qualityScores": ">=<=???>?>???=??>>8<?><=2=<===1194<?;:?>>?#3==>########
#######################################################################################################
############################################################################################",
"qualityScoreVariant": "FASTQ_SANGER"}

scala> val sequences = biojavaContext.loadGenbankDna("SCU49845.gb")
sequences: org.bdgenomics.adam.rdd.sequence.SequenceRDD = SequenceRDD(MapPartitionsRDD[7] at map at BiojavaAdamContext.java:244,SequenceDictionary{
U49845->5028})

scala> sequences.rdd.first
res1: org.bdgenomics.formats.avro.Sequence = {"name": "U49845", "description": "Saccharomyces cerevisiae
TCP1-beta gene, partial cds; and Axl2p\n(AXL2) and Rev7p (REV7) genes, complete cds.", "alphabet": "DNA",
"sequence": "GATCCTCCATATACAACGGTATCTCCACCTCAGGTTTAGATCTCAACAACGGAACCATTGCCGACATGAGACAGTTAGGTATCGTCGAGAGT
TACAAGCTAAAACGAGCAGTAGTCAGCTCTGCATCTGAAGCCGCTGAAGTTCTACTAAGGGTGGATAACATCATCCGTGCAAGACCAAGAACCGCCAATAGACAA
CATATGTAACATATTTAGGATATACCTCGAAAATAATAAACCGCCACACTGTCATTATTATAATTAGAAACAGAACGCAAAAATTATCCACTATATAATTCAAAG
...
```
