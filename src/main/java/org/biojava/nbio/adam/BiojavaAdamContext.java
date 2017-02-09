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
     * Create and return a BufferedReader for the HDFS path represented by the specified file name.
     *
     * @param fileName file name, must not be null
     * @return a BufferedReader for the HDFS path represented by the specified file name
     * @throws IOException if an I/O error occurs
     */
    //BufferedReader reader(final String fileName) throws IOException {
    //    return new BufferedReader(new InputStreamReader(inputStream(fileName)));
    //}

    /**
     * Load the specified path in FASTQ format as reads.
     *
     * @param path path in FASTQ format, must not be null
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

    static <T> List<T> collect(final Iterable<T> iterable) {
        return ImmutableList.copyOf(iterable);
    }
}
