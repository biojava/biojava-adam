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
package org.biojava.nbio.adam

import java.io.InputStream

import com.google.inject.Guice

import net.codingwell.scalaguice.InjectorExtensions._

import org.apache.hadoop.fs.Path

import org.bdgenomics.adam.rdd.ADAMContext

import org.bdgenomics.adam.rdd.feature.FeatureRDD

import org.bdgenomics.adam.rdd.read.ReadRDD

import org.bdgenomics.adam.rdd.sequence.SequenceRDD

import org.bdgenomics.convert.Converter
import org.bdgenomics.convert.ConversionException
import org.bdgenomics.convert.ConversionStringency

import org.bdgenomics.convert.bdgenomics.BdgenomicsModule

import org.bdgenomics.formats.avro.Feature
import org.bdgenomics.formats.avro.Read
import org.bdgenomics.formats.avro.Sequence

import org.bdgenomics.utils.misc.Logging

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

import org.biojava.nbio.sequencing.io.fastq.Fastq
import org.biojava.nbio.sequencing.io.fastq.FastqReader
import org.biojava.nbio.sequencing.io.fastq.SangerFastqReader

import org.biojava.nbio.adam.convert.BiojavaModule

import scala.collection.JavaConversions._

object BiojavaAdamScalaContext {
  def apply(ac: ADAMContext): BiojavaAdamScalaContext = {
    val injector = Guice.createInjector(new BiojavaModule(), new BdgenomicsModule())
    val readConverter = injector.instance[Converter[Fastq, Read]]
    val dnaSequenceConverter = injector.instance[Converter[DNASequence, Sequence]]
    val dnaSequenceFeatureConverter = injector.instance[Converter[DNASequence, java.util.List[Feature]]]
    val proteinSequenceConverter = injector.instance[Converter[ProteinSequence, Sequence]]
    val proteinSequenceFeatureConverter = injector.instance[Converter[ProteinSequence, java.util.List[Feature]]]
    val rnaSequenceConverter = injector.instance[Converter[RNASequence, Sequence]]
    val rnaSequenceFeatureConverter = injector.instance[Converter[RNASequence, java.util.List[Feature]]]

    new BiojavaAdamScalaContext(
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

class BiojavaAdamScalaContext
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

  /**
   * Create and return an InputStream for the HDFS path represented by the specified file name.
   *
   * @param fileName file name, must not be null
   * @return an InputStream for the HDFS path represented by the specified file name
   * @throws IOException if an I/O error occurs
   */
  def openInputStream(fileName: String): InputStream = {
    val path = new Path(fileName)
    val fileSystem = path.getFileSystem(ac.sc.hadoopConfiguration)
    fileSystem.open(path)
  }

  /**
   * Load the specified path in FASTQ format as reads with Biojava.
   *
   * @param path path in FASTQ format, must not be null
   * @return RDD of reads
   * @throws IOException if an I/O error occurs
   */
  def loadBiojavaFastqReads(path: String): ReadRDD = {
    log.info(s"Loading $path in FASTQ format as reads with Biojava...")
    val fastqReader = new SangerFastqReader()
    val inputStream = openInputStream(path)
    val fastqs = ac.sc.parallelize(fastqReader.read(inputStream).iterator.toList)
    val reads = fastqs.map(readConverter.convert(_, ConversionStringency.STRICT, log))
    ReadRDD.apply(reads)
  }
}
