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

import org.bdgenomics.adam.rdd.ADAMContext;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.convert.bdgenomics.BdgenomicsModule;

import org.bdgenomics.formats.avro.Read;
import org.bdgenomics.formats.avro.Sequence;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;

import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper;

import org.biojava.nbio.sequencing.io.fastq.Fastq;
import org.biojava.nbio.sequencing.io.fastq.FastqReader;
import org.biojava.nbio.sequencing.io.fastq.SangerFastqReader;

import org.biojava.nbio.adam.convert.BiojavaModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** Convert BioJava ProteinSequence to bdg-formats Sequence. */
    private final Converter<ProteinSequence, Sequence> proteinSequenceConverter;


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
        proteinSequenceConverter = injector.getInstance(Key.get(new TypeLiteral<Converter<ProteinSequence, Sequence>>() {}));
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
    public RDD<Read> biojavaLoadFastq(final String path) throws IOException {
        log().info("Loading " + path + " as FASTQ format...");
        FastqReader fastqReader = new SangerFastqReader();
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<Fastq> fastqs = javaSparkContext.parallelize(collect(fastqReader.read(inputStream)));
            JavaRDD<Read> reads = fastqs.map(fastq -> readConverter.convert(fastq, ConversionStringency.STRICT, log()));
            return reads.rdd();
        }
    }

    /**
     * Load the specified path in FASTA format as DNA sequences.
     *
     * @param path path in FASTA format, must not be null
     * @return RDD of DNA sequences
     * @throws IOException if an I/O error occurs
     */
    public RDD<Sequence> biojavaLoadFastaDna(final String path) throws IOException {
        log().info("Loading " + path + " as DNA sequences in FASTA format...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<DNASequence> dnaSequences = javaSparkContext.parallelize(collect(FastaReaderHelper.readFastaDNASequence(inputStream)));
            JavaRDD<Sequence> sequences = dnaSequences.map(dnaSequence -> dnaSequenceConverter.convert(dnaSequence, ConversionStringency.STRICT, log()));
            return sequences.rdd();
        }
    }

    /**
     * Load the specified path in FASTA format as protein sequences.
     *
     * @param path path in FASTA format, must not be null
     * @return RDD of protein sequences
     * @throws IOException if an I/O error occurs
     */
    public RDD<Sequence> biojavaLoadFastaProtein(final String path) throws IOException {
        log().info("Loading " + path + " as protein sequences in FASTA format...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<ProteinSequence> proteinSequences = javaSparkContext.parallelize(collect(FastaReaderHelper.readFastaProteinSequence(inputStream)));
            JavaRDD<Sequence> sequences = proteinSequences.map(proteinSequence -> proteinSequenceConverter.convert(proteinSequence, ConversionStringency.STRICT, log()));
            return sequences.rdd();
        }
    }

    /**
     * Load the specified path in Genbank format as DNA sequences.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of DNA sequences
     * @throws Exception if an I/O error occurs
     */
    public RDD<Sequence> biojavaLoadGenbankDna(final String path) throws Exception {
        log().info("Loading " + path + " as DNA sequences in Genbank format...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<DNASequence> dnaSequences = javaSparkContext.parallelize(collect(GenbankReaderHelper.readGenbankDNASequence(inputStream)));
            JavaRDD<Sequence> sequences = dnaSequences.map(dnaSequence -> dnaSequenceConverter.convert(dnaSequence, ConversionStringency.STRICT, log()));
            return sequences.rdd();
        }
    }

    /**
     * Load the specified path in Genbank format as protein sequences.
     *
     * @param path path in Genbank format, must not be null
     * @return RDD of protein sequences
     * @throws Exception if an I/O error occurs
     */
    public RDD<Sequence> biojavaLoadGenbankProtein(final String path) throws Exception {
        log().info("Loading " + path + " as protein sequences in Genbank format...");
        try (InputStream inputStream = inputStream(path)) {
            JavaRDD<ProteinSequence> proteinSequences = javaSparkContext.parallelize(collect(GenbankReaderHelper.readGenbankProteinSequence(inputStream)));
            JavaRDD<Sequence> sequences = proteinSequences.map(proteinSequence -> proteinSequenceConverter.convert(proteinSequence, ConversionStringency.STRICT, log()));
            return sequences.rdd();
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
