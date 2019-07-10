# biojava-adam

[Biojava](http://biojava.org) and [ADAM](https://github.com/bigdatagenomics/adam) integration.

[![Build Status](https://travis-ci.org/biojava/biojava-adam.svg?branch=master)](https://travis-ci.org/biojava/biojava-adam)
[![Maven Central](https://img.shields.io/maven-central/v/org.biojava/biojava-adam.svg?maxAge=600)](http://search.maven.org/#search%7Cga%7C1%7Corg.biojava)
[![API Documentation](http://javadoc.io/badge/org.biojava/biojava-adam.svg?color=brightgreen&label=scaladoc)](http://javadoc.io/doc/org.biojava/biojava-adam)


### Hacking biojava-adam

Install

 * JDK 1.8 or later, http://openjdk.java.net
 * Apache Maven 3.3.9 or later, http://maven.apache.org
 * Apache Spark 2.4.3 or later, http://spark.apache.org
 * ADAM: Genomic Data System 0.28.0-SNAPSHOT or later, https://github.com/bigdatagenomics/adam

To build

    $ mvn install


### Running biojava-adam

To run interactively in `spark-shell`

```
$ spark-shell \
    --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
    --conf spark.kryo.registrator=org.biojava.nbio.adam.BiojavaKryoRegistrator \
    --jars target/biojava-adam-0.2.0-SNAPSHOT.jar,$PATH_TO_ADAM_ASSEMBLY_JAR

Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.4.3
      /_/

Using Scala version 2.11.12 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_191)
Type in expressions to have them evaluated.
Type :help for more information.

scala> import org.biojava.nbio.adam.BiojavaAdamContext
import org.biojava.nbio.adam.BiojavaAdamContext

scala> val bc = BiojavaAdamContext(sc)
bc: org.biojava.nbio.adam.BiojavaAdamContext = org.biojava.nbio.adam.BiojavaAdamContext@4f8900b0

scala> val reads = bc.loadFastqReads("src/test/resources/org/biojava/nbio/adam/bqsr.0.fq")
reads: org.bdgenomics.adam.rdd.read.ReadRDD = RDDBoundReadRDD with 0 reference sequences

scala> reads.rdd.first
res0: org.bdgenomics.formats.avro.Read = {"name": "SRR062634.10022079/1", "description":
"SRR062634.10022079/1", "alphabet": "DNA", "sequence": "AATTCAAAACCAGCCTGGCCAATATGGTGAAACCTCATCTCTACTAAAA
ATACAAAAATTAGCCAGGCATGGTGGTGCGTGCGTGTAGTCCCAGCTACTT", "length": 100, "qualityScores": "?-DDBEEB=EEEDDEDEE
EA:D?5?E?CEBE5ED?D:AEDEDEDED-B,BC0AC,BB6@CDBDEC?BCBAA@5,=8CA-?A>?2:&048<BB5BE#####",
"qualityScoreVariant": "FASTQ_SANGER", "attributes": {}}

scala> val dna = bc.loadBiojavaFastaDna("src/test/resources/org/biojava/nbio/adam/hla_gen.0.fa")
dna: org.bdgenomics.adam.rdd.sequence.SequenceRDD = RDDBoundSequenceRDD with 0 reference sequences

scala> dna.rdd.first
res0: org.bdgenomics.formats.avro.Sequence = {"name": "HLA:HLA00001 A*01:01:01:01 3503 bp",
"description": null, "alphabet": "DNA", "sequence": "CAGGAGCAGAGGGGTCAGGGCGAAGTCCCAGGGCCCCAGGCGTGGCTCTCAG
GGTCTCAGGCCCCGAAGGCGGTGTATGGATTGGGGAGTCCCAGCCTTGGGGATTCCCCAACTCCGCAGTTTCTTTTCTCCCTCTCCCAACCTACGTAGGGTCCTT
CATCCTGGATACTCACGACGCGGACCCAGTTCTCACTCCCATTGGGTGTCGGGTTTCCAGAGAAGCCAATCAGTGTCGTCGCGGTCGCTGTTCTAAAGTCCGCAC
...

scala> val prot = bc.loadBiojavaFastaProtein("src/test/resources/org/biojava/nbio/adam/hla_prot.0.fa")
prot: org.bdgenomics.adam.rdd.sequence.SequenceRDD = RDDBoundSequenceRDD with 0 reference sequences

scala> prot.rdd.first
res2: org.bdgenomics.formats.avro.Sequence = {"name": "HLA:HLA00001 A*01:01:01:01 365 bp", "description":
null, "alphabet": "PROTEIN", "sequence": "MAVMAPRTLLLLLSGALALTQTWAGSHSMRYFFTSVSRPGRGEPRFIAVGYVDDTQFVRFDSD
AASQKMEPRAPWIEQEGPEYWDQETRNMKAHSQTDRANLGTLRGYYNQSEDGSHTIQIMYGCDVGPDGRFLRGYRQDAYDGKDYIALNEDLRSWTAADMAAQITK
RKWEAVHAAEQRRVYLEGRCVDGLRRYLENGKETLQRTDPPKTHMTHHPISDHEATLRCWALGFYPAEITLTWQRDGEDQTQDTELVETRPAGDGTFQKWAAVVV
PSGEEQRYTCHVQHEGLPKPLTLRWELSSQPTIPIVGIIAGLVLLGAVITGAVVAAVMWRRKSSDRKGGSYTQAASSDSAQGSDVSLTACKV", "length":
365, "attributes": {}}

scala> val genbankDna = bc.loadGenbankDna("src/test/resources/org/biojava/nbio/adam/SCU49845.gb")
genbankDna: org.bdgenomics.adam.rdd.sequence.SequenceRDD = RDDBoundSequenceRDD with 0 reference sequences

scala> genbankDna.rdd.first
res4: org.bdgenomics.formats.avro.Sequence = {"name": "U49845", "description": "Saccharomyces cerevisiae
TCP1-beta gene, partial cds; and Axl2p\n(AXL2) and Rev7p (REV7) genes, complete cds.", "alphabet": "DNA",
"sequence": "GATCCTCCATATACAACGGTATCTCCACCTCAGGTTTAGATCTCAACAACGGAACCATTGCCGACATGAGACAGTTAGGTATCGTCGAGAGT
TACAAGCTAAAACGAGCAGTAGTCAGCTCTGCATCTGAAGCCGCTGAAGTTCTACTAAGGGTGGATAACATCATCCGTGCAAGACCAAGAACCGCCAATAGACAA
CATATGTAACATATTTAGGATATACCTCGAAAATAATAAACCGCCACACTGTCATTATTATAATTAGAAACAGAACGCAAAAATTATCCACTATATAATTCAAAG
...

scala> val features = bc.loadGenbankDnaFeatures("src/test/resources/org/biojava/nbio/adam/SCU49845.gb")
features: org.bdgenomics.adam.rdd.feature.FeatureRDD = RDDBoundFeatureRDD with 0 reference sequences

scala> features.rdd.first
res5: org.bdgenomics.formats.avro.Feature = {"featureId": null, "name": "source", "source": null,
"featureType": null, "contigName": "U49845", "start": 0, "end": 5028, "strand": "FORWARD", "phase":
null, "frame": null, "score": null, "geneId": null, "transcriptId": null, "exonId": null, "aliases":
[], "parentIds": [], "target": null, "gap": null, "derivesFrom": null, "notes": [], "dbxrefs": [],
"ontologyTerms": [], "circular": null, "attributes": {}}
```


### Example biojava-adam scripts

Some scripts for `spark-shell` written in Scala are provided in the `scripts` directory. E.g. to transform
DNA sequences in Genbank format to `Sequence`s in Parquet format:

```
$ INPUT=Homo_sapiens.GRCh38.96.chromosome.21.dat.gz \
  OUTPUT=Homo_sapiens.GRCh38.96.chromosome.21.sequences.adam \
  spark-shell \
    --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
    --conf spark.kryo.registrator=org.biojava.nbio.adam.BiojavaKryoRegistrator \
    --jars target/biojava-adam-0.2.0-SNAPSHOT.jar,$PATH_TO_ADAM_ASSEMBLY_JAR
    -i scripts/loadGenbankDna.scala
```

All the scripts follow a similar pattern, with input path specified by `INPUT` environment variable and output
path specified by `OUTPUT` environment variable.
