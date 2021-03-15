/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2021 held jointly by the individual authors.

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
package org.biojava.nbio.adam

import java.io.InputStream

import com.google.inject.Guice

import grizzled.slf4j.Logging

import net.codingwell.scalaguice.InjectorExtensions._

import org.apache.hadoop.fs.Path

import org.apache.hadoop.io.compress.CompressionCodecFactory

import org.bdgenomics.adam.ds.ADAMContext

import org.bdgenomics.adam.ds.feature.FeatureDataset

import org.bdgenomics.adam.ds.read.ReadDataset

import org.bdgenomics.adam.ds.sequence.SequenceDataset

import org.bdgenomics.convert.Converter
import org.bdgenomics.convert.ConversionException
import org.bdgenomics.convert.ConversionStringency

import org.bdgenomics.convert.bdgenomics.BdgenomicsModule

import org.bdgenomics.formats.avro.Feature
import org.bdgenomics.formats.avro.Read
import org.bdgenomics.formats.avro.Sequence

import org.biojava.nbio.core.exceptions.CompoundNotFoundException

import org.biojava.nbio.core.sequence.DNASequence
import org.biojava.nbio.core.sequence.ProteinSequence
import org.biojava.nbio.core.sequence.RNASequence

import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet
import org.biojava.nbio.core.sequence.compound.AmbiguityRNACompoundSet
import org.biojava.nbio.core.sequence.compound.NucleotideCompound

import org.biojava.nbio.core.sequence.io.DNASequenceCreator
import org.biojava.nbio.core.sequence.io.FastaReader
import org.biojava.nbio.core.sequence.io.FastaReaderHelper
import org.biojava.nbio.core.sequence.io.GenbankReader
import org.biojava.nbio.core.sequence.io.GenbankReaderHelper
import org.biojava.nbio.core.sequence.io.GenericFastaHeaderParser
import org.biojava.nbio.core.sequence.io.GenericGenbankHeaderParser
import org.biojava.nbio.core.sequence.io.RNASequenceCreator

import org.biojava.nbio.genome.io.fastq.Fastq
import org.biojava.nbio.genome.io.fastq.FastqReader
import org.biojava.nbio.genome.io.fastq.SangerFastqReader

import org.biojava.nbio.adam.convert.BiojavaModule

import scala.collection.JavaConversions._

import scala.util.{ Failure, Success }

object BiojavaAdamContext {

  /**
   * Create a new BiojavaAdamContext extending the specified ADAMContext.
   *
   * @param ac ADAMContext to extend.
   */
  def apply(ac: ADAMContext): BiojavaAdamContext = {
    val injector = Guice.createInjector(new BiojavaModule(), new BdgenomicsModule())
    val readConverter = injector.instance[Converter[Fastq, Read]]
    val dnaSequenceConverter = injector.instance[Converter[DNASequence, Sequence]]
    val dnaSequenceFeatureConverter = injector.instance[Converter[DNASequence, java.util.List[Feature]]]
    val proteinSequenceConverter = injector.instance[Converter[ProteinSequence, Sequence]]
    val proteinSequenceFeatureConverter = injector.instance[Converter[ProteinSequence, java.util.List[Feature]]]
    val rnaSequenceConverter = injector.instance[Converter[RNASequence, Sequence]]
    val rnaSequenceFeatureConverter = injector.instance[Converter[RNASequence, java.util.List[Feature]]]

    new BiojavaAdamContext(
      ac,
      readConverter,
      dnaSequenceConverter,
      dnaSequenceFeatureConverter,
      proteinSequenceConverter,
      proteinSequenceFeatureConverter,
      rnaSequenceConverter,
      rnaSequenceFeatureConverter
    )
  }
}

/**
 * Extends ADAMContext with load methods for Biojava models.
 *
 * @param ac ADAMContext to extend.
 * @param readConverter Convert Biojava Fastq to bdg-formats Read.
 * @param dnaSequenceConverter Convert Biojava DNASequence to bdg-formats Sequence.
 * @param dnaSequenceFeaturesConverter Convert Biojava DNASequence to a list of bdg-formats Features.
 * @param proteinSequenceConverter Convert Biojava ProteinSequence to bdg-formats Sequence
 * @param proteinSequenceFeaturesConverter Convert Biojava ProteinSequence to a list of bdg-formats Features
 * @param rnaSequenceConverter Convert Biojava RNASequence to bdg-formats Sequence
 * @param rnaSequenceFeaturesConverter Convert Biojava RNASequence to a list of bdg-formats Features
 */
class BiojavaAdamContext
(
  @transient val ac: ADAMContext,
  val readConverter: Converter[Fastq, Read],
  val dnaSequenceConverter: Converter[DNASequence, Sequence],
  val dnaSequenceFeaturesConverter: Converter[DNASequence, java.util.List[Feature]],
  val proteinSequenceConverter: Converter[ProteinSequence, Sequence],
  val proteinSequenceFeaturesConverter: Converter[ProteinSequence, java.util.List[Feature]],
  val rnaSequenceConverter: Converter[RNASequence, Sequence],
  val rnaSequenceFeaturesConverter: Converter[RNASequence, java.util.List[Feature]]
) extends Serializable with Logging {

  // FASTQ format

  /**
   * Load the specified path in FASTQ format as reads with Biojava.
   * Alias for <code>loadBiojavaFastqReads</code>.
   *
   * @param path path in FASTQ format, must not be null
   * @return RDD of reads
   * @throws IOException if an I/O error occurs
   */
  def loadFastqReads(path: String): ReadDataset = {
    loadBiojavaFastqReads(path)
  }

  /**
   * Load the specified path in FASTQ format as reads with Biojava.
   *
   * @param path path in FASTQ format, must not be null
   * @return RDD of reads
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaFastqReads(path: String): ReadDataset = {
    logger.info(s"Loading $path in FASTQ format as reads with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val fastqs = ac.sc.parallelize(readFastq(inputStream))
      val reads = fastqs.map(readConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      ReadDataset(reads)
    }) match {
      case Success(r) => r
      case Failure(e) => throw e
    }
  }

  // FASTA format

  /**
   * Load the specified path in FASTA format as DNA sequences with Biojava.
   *
   * @param path path in FASTA format, must not be null
   * @return RDD of DNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaFastaDna(path: String): SequenceDataset = {
    logger.info(s"Loading $path in FASTA format as DNA sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val dnaSequences = ac.sc.parallelize(readFastaDna(inputStream))
      val sequences = dnaSequences.map(dnaSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in FASTA format as protein sequences with Biojava.
   *
   * @param path path in FASTA format, must not be null
   * @return RDD of protein sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaFastaProtein(path: String): SequenceDataset = {
    logger.info(s"Loading $path in FASTA format as protein sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val proteinSequences = ac.sc.parallelize(readFastaProtein(inputStream))
      val sequences = proteinSequences.map(proteinSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in FASTA format as RNA sequences with Biojava.
   *
   * @param path path in FASTA format, must not be null
   * @return RDD of RNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaFastaRna(path: String): SequenceDataset = {
    logger.info(s"Loading $path in FASTA format as RNA sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val rnaSequences = ac.sc.parallelize(readFastaRna(inputStream))
      val sequences = rnaSequences.map(rnaSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  // Genbank format

  /**
   * Load the specified path in Genbank format as DNA sequences with Biojava.
   * Alias for <code>loadBiojavaGenbankDna</code>.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of DNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadGenbankDna(path: String): SequenceDataset = {
    loadBiojavaGenbankDna(path)
  }

  /**
   * Load the specified path in Genbank format as DNA sequences with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of DNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaGenbankDna(path: String): SequenceDataset = {
    logger.info(s"Loading $path in Genbank format as DNA sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val dnaSequences = ac.sc.parallelize(readGenbankDna(inputStream))
      val sequences = dnaSequences.map(dnaSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in Genbank format as DNA sequence features with Biojava.
   * Alias for <code>loadBiojavaGenbankDnaFeatures</code>.
   * 
   * @param path path in Genbank format, must not be null
   * @return RDD of DNA sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadGenbankDnaFeatures(path: String): FeatureDataset = {
    loadBiojavaGenbankDnaFeatures(path)
  }

  /**
   * Load the specified path in Genbank format as DNA sequence features with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of DNA sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadBiojavaGenbankDnaFeatures(path: String): FeatureDataset = {
    logger.info(s"Loading $path in Genbank format as DNA sequence features with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val dnaSequences = ac.sc.parallelize(readGenbankDna(inputStream))
      val features = dnaSequences.flatMap(dnaSequenceFeaturesConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      FeatureDataset(features)
    }) match {
      case Success(f) => f
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in Genbank format as protein sequences with Biojava.
   * Alias for <code>loadBiojavaGenbankProtein</code>.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of protein sequences
   * @throws IOException if an I/O error occurs
   */
  def loadGenbankProtein(path: String): SequenceDataset = {
    loadBiojavaGenbankProtein(path)
  }

  /**
   * Load the specified path in Genbank format as protein sequences with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of protein sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaGenbankProtein(path: String): SequenceDataset = {
    logger.info(s"Loading $path in Genbank format as protein sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val proteinSequences = ac.sc.parallelize(readGenbankProtein(inputStream))
      val sequences = proteinSequences.map(proteinSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in Genbank format as protein sequence features with Biojava.
   * Alias for <code>loadBiojavaGenbankProteinFeatures</code>.
   * 
   * @param path path in Genbank format, must not be null
   * @return RDD of protein sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadGenbankProteinFeatures(path: String): FeatureDataset = {
    loadBiojavaGenbankProteinFeatures(path)
  }

  /**
   * Load the specified path in Genbank format as protein sequence features with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of protein sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadBiojavaGenbankProteinFeatures(path: String): FeatureDataset = {
    logger.info(s"Loading $path in Genbank format as protein sequence features with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val proteinSequences = ac.sc.parallelize(readGenbankProtein(inputStream))
      val features = proteinSequences.flatMap(proteinSequenceFeaturesConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      FeatureDataset(features)
    }) match {
      case Success(f) => f
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in Genbank format as RNA sequences with Biojava.
   * Alias for <code>loadBiojavaGenbankRna</code>.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of RNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadGenbankRna(path: String): SequenceDataset = {
    loadBiojavaGenbankRna(path)
  }

  /**
   * Load the specified path in Genbank format as RNA sequences with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of RNA sequences
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaGenbankRna(path: String): SequenceDataset = {
    logger.info(s"Loading $path in Genbank format as RNA sequences with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val rnaSequences = ac.sc.parallelize(readGenbankRna(inputStream))
      val sequences = rnaSequences.map(rnaSequenceConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      SequenceDataset(sequences)
    }) match {
      case Success(s) => s
      case Failure(e) => throw e
    }
  }

  /**
   * Load the specified path in Genbank format as RNA sequence features with Biojava.
   * Alias for <code>loadBiojavaGenbankRnaFeatures</code>.
   * 
   * @param path path in Genbank format, must not be null
   * @return RDD of RNA sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadGenbankRnaFeatures(path: String): FeatureDataset = {
    loadBiojavaGenbankRnaFeatures(path)
  }

  /**
   * Load the specified path in Genbank format as RNA sequence features with Biojava.
   *
   * @param path path in Genbank format, must not be null
   * @return RDD of RNA sequence features
   * @throws Exception if an I/O error occurs
   */
  def loadBiojavaGenbankRnaFeatures(path: String): FeatureDataset = {
    logger.info(s"Loading $path in Genbank format as RNA sequence features with Biojava...")
    TryWith(openInputStream(path))(inputStream => {
      val rnaSequences = ac.sc.parallelize(readGenbankRna(inputStream))
      val features = rnaSequences.flatMap(rnaSequenceFeaturesConverter.convert(_, ConversionStringency.STRICT, logger.logger))
      FeatureDataset(features)
    }) match {
      case Success(f) => f
      case Failure(e) => throw e
    }
  }

  /**
   * Create and return an InputStream for the HDFS path represented by the specified file name.
   *
   * @param fileName file name, must not be null
   * @return an InputStream for the HDFS path represented by the specified file name
   * @throws IOException if an I/O error occurs
   */
  private def openInputStream(fileName: String): InputStream = {
    val path = new Path(fileName)
    val fileSystem = path.getFileSystem(ac.sc.hadoopConfiguration)
    val codecFactory = new CompressionCodecFactory(ac.sc.hadoopConfiguration)
    val codec = codecFactory.getCodec(path)

    if (codec == null) {
      fileSystem.open(path)
    } else {
      codec.createInputStream(fileSystem.open(path))
    }
  }

  private def readFastq(inputStream: InputStream): Seq[Fastq] = {
    val fastqReader = new SangerFastqReader()
    fastqReader.read(inputStream).iterator.toList
  }

  private def readFastaDna(inputStream: InputStream): Seq[DNASequence] = {
    val fastaReader = new FastaReader[DNASequence, NucleotideCompound](
      inputStream,
      new GenericFastaHeaderParser[DNASequence, NucleotideCompound](),
      new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()))

    fastaReader.process.values.iterator.toList
  }

  private def readFastaProtein(inputStream: InputStream): Seq[ProteinSequence] = {
    FastaReaderHelper.readFastaProteinSequence(inputStream).values.iterator.toList
  }

  private def readFastaRna(inputStream: InputStream): Seq[RNASequence] = {
    val fastaReader = new FastaReader[RNASequence, NucleotideCompound](
      inputStream,
      new GenericFastaHeaderParser[RNASequence, NucleotideCompound](),
      new RNASequenceCreator(AmbiguityRNACompoundSet.getRNACompoundSet()))

    fastaReader.process.values.iterator.toList
  }

  private def readGenbankDna(inputStream: InputStream): Seq[DNASequence] = {
    val genbankReader = new GenbankReader[DNASequence, NucleotideCompound](
      inputStream,
      new GenericGenbankHeaderParser[DNASequence, NucleotideCompound](),
      new DNASequenceCreator(AmbiguityDNACompoundSet.getDNACompoundSet()))

    genbankReader.process.values.iterator.toList
  }

  private def readGenbankProtein(inputStream: InputStream): Seq[ProteinSequence] = {
    GenbankReaderHelper.readGenbankProteinSequence(inputStream).values.iterator.toList
  }

  private def readGenbankRna(inputStream: InputStream): Seq[RNASequence] = {
    val genbankReader = new GenbankReader[RNASequence, NucleotideCompound](
      inputStream,
      new GenericGenbankHeaderParser[RNASequence, NucleotideCompound](),
      new RNASequenceCreator(AmbiguityRNACompoundSet.getRNACompoundSet()))

    genbankReader.process.values.iterator.toList
  }
}
