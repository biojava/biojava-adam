/*

    biojava-adam  BioJava and ADAM integration.
    Copyright (c) 2017 held jointly by the individual authors.

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.fsf.org/licensing/licenses/lgpl.html
    > http://www.opensource.org/licenses/lgpl-license.php

*/
package org.biojava.nbio.adam;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import com.google.inject.Injector;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.spark.SparkContext;

import org.apache.spark.rdd.RDD;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import org.apache.spark.storage.StorageLevel;

import org.bdgenomics.adam.rdd.ADAMContext;

import org.bdgenomics.adam.rdd.feature.FeatureRDD;

import org.bdgenomics.adam.rdd.sequence.ReadRDD;
import org.bdgenomics.adam.rdd.sequence.SequenceRDD;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.convert.bdgenomics.BdgenomicsModule;

import org.bdgenomics.formats.avro.Feature;
import org.bdgenomics.formats.avro.Read;
import org.bdgenomics.formats.avro.Sequence;

import org.biojava.nbio.core.exceptions.CompoundNotFoundException;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;

import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.compound.AmbiguityRNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;

import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.biojava.nbio.core.sequence.io.GenbankReader;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser;
import org.biojava.nbio.core.sequence.io.GenericGenbankHeaderParser;
import org.biojava.nbio.core.sequence.io.RNASequenceCreator;

import org.biojava.nbio.sequencing.io.fastq.Fastq;
import org.biojava.nbio.sequencing.io.fastq.FastqReader;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqReader;

import org.biojava.nbio.adam.convert.BiojavaModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Some;

/**
 * Extends ADAMContext with load methods for BioJava models.
 */
public class BiojavaAdamContext extends ADAMContext {
    /** Java Spark context. */
    private final transient JavaSparkContext javaSparkContext;

    /** Convert BioJava Fastq to bdg-formats Read. */
    private final Converter<Fastq, Read> readConverter;

    /** Convert BioJava DNASequence to bdg-formats Sequence. */
    private final Converter<DNASequence, Sequence> dnaSequenceConverter;

    /** Convert BioJava DNASequence to a list of bdg-formats Features. */
    private final Converter<DNASequence, List<Feature>> dnaSequenceFeaturesConverter;

    /** Convert BioJava ProteinSequence to bdg-formats Sequence. */
    private final Converter<ProteinSequence, Sequence> proteinSequenceConverter;

    /** Convert BioJava ProteinSequence to a list of bdg-formats Features. */
    private final Converter<ProteinSequence, List<Feature>> proteinSequenceFeaturesConverter;

    /** Convert BioJava RNASequence to bdg-formats Sequence. */
    private final Converter<RNASequence, Sequence> rnaSequenceConverter;

    /** Convert BioJava RNASequence to a list of bdg-formats Features. */
    private final Converter<RNASequence, List<Feature>> rnaSequenceFeaturesConverter;


    /**
     * Create a new BiojavaAdamContext with the specified Spark context.
     *
     * @param sc Spark context, must not be null
     */
    public BiojavaAdamContext(final SparkContext sc) {        
        super(sc);

        javaSparkContext = new JavaSparkContext(sc);

        Injector injector = Guice.createInjector(new BiojavaModule(), new BdgenomicsModule());
        readConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<Fastq, Read>>() {}));
        dnaSequenceConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<DNASequence, Sequence>>() {}));
        dnaSequenceFeaturesConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<DNASequence, List<Feature>>>() {}));
        proteinSequenceConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<ProteinSequence, Sequence>>() {}));
        proteinSequenceFeaturesConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<ProteinSequence, List<Feature>>>() {}));
        rnaSequenceConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<RNASequence, Sequence>>() {}));
        rnaSequenceFeaturesConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<RNASequence, List<Feature>>>() {}));
    }


    /**
     * Create and return an InputStream for the HDFS path represented by the specified file name.
     *
     * @param fileName file name, must not be null
     * @return an InputStream for the HDFS path represented by the specified file name
     * @throws IOException if an I/O error occurs
     */
    InputStream inputStream(final String fileName) throws IOException {
        checkNotNull(fileName);
        Path path = new Path(fileName);
        FileSystem fileSystem = path.getFileSystem(javaSparkContext.hadoopConfiguration());
        return fileSystem.open(path);
    }

    /**
     * Load the specified path in FASTQ format as reads.
     *
     * @param path path in FASTQ format, must not be null
     * @return RDD of reads
     * @throws IOException if an I/O error occurs
     */
    public ReadRDD loadFastqReads(final String path) throws IOException {
        log().info("Loading " + path + " in FASTQ format as reads...");
        FastqReader fastqReader = new SangerFastqReader();
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<Fastq> fastqs = javaSparkContext.parallelize(collect(fastqReader.read(inputStream)));
            JavaRDD<Read> reads = fastqs.map(fastq -> readConverter.convert(fastq, ConversionStringency.STRICT, log()));
            return ReadRDD.apply(reads.rdd());
        }
    }

    /**
     * Load the specified path in FASTA format as DNA sequences.
     *
     * @param path path in FASTA format, must not be null
     * @return RDD of DNA sequences
     * @throws IOException if an I/O error occurs
     */
    public SequenceRDD loadFastaDna(final String path) throws IOException {
        log().info("Loading " + path + " in FASTA format as DNA sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<DNASequence> dnaSequences = javaSparkContext.parallelize(readFastaDna(inputStream));
            JavaRDD<Sequence> sequences = dnaSequences.map(dnaSequence -> dnaSequenceConverter.convert(dnaSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in FASTA format as protein sequences.
     *
     * @param path path in FASTA format, must not be null
     * @return RDD of protein sequences
     * @throws IOException if an I/O error occurs
     */
    public SequenceRDD loadFastaProtein(final String path) throws IOException {
        log().info("Loading " + path + " in FASTA format as protein sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<ProteinSequence> proteinSequences = javaSparkContext.parallelize(collect(FastaReaderHelper.readFastaProteinSequence(inputStream)));
            JavaRDD<Sequence> sequences = proteinSequences.map(proteinSequence -> proteinSequenceConverter.convert(proteinSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in FASTA format as RNA sequences.
     *
     * @param path path in FASTA format, must not be null
     * @return RDD of RNA sequences
     * @throws IOException if an I/O error occurs
     */
    public SequenceRDD loadFastaRna(final String path) throws IOException {
        log().info("Loading " + path + " in FASTA format as RNA sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<RNASequence> rnaSequences = javaSparkContext.parallelize(readFastaRna(inputStream));
            JavaRDD<Sequence> sequences = rnaSequences.map(rnaSequence -> rnaSequenceConverter.convert(rnaSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as DNA sequences.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of DNA sequences
     * @throws Exception if an I/O error occurs
     */
    public SequenceRDD loadGenbankDna(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as DNA sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<DNASequence> dnaSequences = javaSparkContext.parallelize(readGenbankDna(inputStream));
            JavaRDD<Sequence> sequences = dnaSequences.map(dnaSequence -> dnaSequenceConverter.convert(dnaSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as DNA sequence features.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of DNA sequence features
     * @throws Exception if an I/O error occurs
     */
    public FeatureRDD loadGenbankDnaFeatures(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as DNA sequence features...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<DNASequence> dnaSequences = javaSparkContext.parallelize(readGenbankDna(inputStream));
            JavaRDD<Feature> features = dnaSequences.flatMap(sequence -> dnaSequenceFeaturesConverter.convert(sequence, ConversionStringency.STRICT, log()).iterator());
            return FeatureRDD.apply(features.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as protein sequences.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of protein sequences
     * @throws Exception if an I/O error occurs
     */
    public SequenceRDD loadGenbankProtein(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as protein sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<ProteinSequence> proteinSequences = javaSparkContext.parallelize(collect(GenbankReaderHelper.readGenbankProteinSequence(inputStream)));
            JavaRDD<Sequence> sequences = proteinSequences.map(proteinSequence -> proteinSequenceConverter.convert(proteinSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as protein sequence features.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of protein sequence features
     * @throws Exception if an I/O error occurs
     */
    public FeatureRDD loadGenbankProteinFeatures(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as protein sequence features...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<ProteinSequence> proteinSequences = javaSparkContext.parallelize(collect(GenbankReaderHelper.readGenbankProteinSequence(inputStream)));
            JavaRDD<Feature> features = proteinSequences.flatMap(sequence -> proteinSequenceFeaturesConverter.convert(sequence, ConversionStringency.STRICT, log()).iterator());
            return FeatureRDD.apply(features.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as RNA sequences.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of RNA sequences
     * @throws Exception if an I/O error occurs
     */
    public SequenceRDD loadGenbankRna(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as RNA sequences...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<RNASequence> rnaSequences = javaSparkContext.parallelize(readGenbankRna(inputStream));
            JavaRDD<Sequence> sequences = rnaSequences.map(rnaSequence -> rnaSequenceConverter.convert(rnaSequence, ConversionStringency.STRICT, log()));
            return SequenceRDD.apply(sequences.rdd());
        }
    }

    /**
     * Load the specified path in Genbank format as RNA sequence features.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of RNA sequence features
     * @throws Exception if an I/O error occurs
     */
    public FeatureRDD loadGenbankRnaFeatures(final String path) throws Exception {
        log().info("Loading " + path + " in Genbank format as RNA sequence features...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<RNASequence> rnaSequences = javaSparkContext.parallelize(readGenbankRna(inputStream));
            JavaRDD<Feature> features = rnaSequences.flatMap(sequence -> rnaSequenceFeaturesConverter.convert(sequence, ConversionStringency.STRICT, log()).iterator());
            return FeatureRDD.apply(features.rdd());
        }
    }

    /**
     * Read DNA sequences with ambiguous bases from the specified input stream in FASTA format.
     *
     * @param inputStream input stream to read from
     * @return a list of zero or more DNA sequences read from the specified input stream in FASTA format
     */
    static List<DNASequence> readFastaDna(final InputStream inputStream) throws IOException {
        FastaReader<DNASequence, NucleotideCompound> fastaReader = new FastaReader<DNASequence, NucleotideCompound>(
            inputStream,
            new GenericFastaHeaderParser<DNASequence, NucleotideCompound>(),
            new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()));

        return collect(fastaReader.process().values());
    }

    /**
     * Read RNA sequences with ambiguous bases from the specified input stream in FASTA format.
     *
     * @param inputStream input stream to read from
     * @return a list of zero or more RNA sequences read from the specified input stream in FASTA format
     */
    static List<RNASequence> readFastaRna(final InputStream inputStream) throws IOException {
        FastaReader<RNASequence, NucleotideCompound> fastaReader = new FastaReader<RNASequence, NucleotideCompound>(
            inputStream,
            new GenericFastaHeaderParser<RNASequence, NucleotideCompound>(),
            new RNASequenceCreator(AmbiguityRNACompoundSet.getRNACompoundSet()));

        return collect(fastaReader.process().values());
    }

    /**
     * Read DNA sequences with ambiguous bases from the specified input stream in Genbank format.
     *
     * @param inputStream input stream to read from
     * @return a list of zero or more DNA sequences read from the specified input stream in Genbank format
     */
    static List<DNASequence> readGenbankDna(final InputStream inputStream) throws IOException {
        GenbankReader<DNASequence, NucleotideCompound> genbankReader = new GenbankReader<DNASequence, NucleotideCompound>(
            inputStream,
            new GenericGenbankHeaderParser<DNASequence, NucleotideCompound>(),
            new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()));

        try {
            return collect(genbankReader.process().values());
        }
        catch (CompoundNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Read RNA sequences with ambiguous bases from the specified input stream in Genbank format.
     *
     * @param inputStream input stream to read from
     * @return a list of zero or more RNA sequences read from the specified input stream in Genbank format
     */
    static List<RNASequence> readGenbankRna(final InputStream inputStream) throws IOException {
        GenbankReader<RNASequence, NucleotideCompound> genbankReader = new GenbankReader<RNASequence, NucleotideCompound>(
            inputStream,
            new GenericGenbankHeaderParser<RNASequence, NucleotideCompound>(),
            new RNASequenceCreator(AmbiguityRNACompoundSet.getRNACompoundSet()));

        try {
            return collect(genbankReader.process().values());
        }
        catch (CompoundNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Collect the specified iterable into a list.
     *
     * @param iterable iterable to collect
     * @return the specified iterable collected into a list
     */
    static <T> List<T> collect(final Iterable<T> iterable) {
        return ImmutableList.copyOf(iterable);
    }

    /**
     * Collect the values in the specified map into a list.
     *
     * @param map map with values to collect
     * @return the values in the specified map collected into a list
     */
    static <K, V> List<V> collect(final Map<K, V> map) {
        return ImmutableList.copyOf(map.values());
    }
}
