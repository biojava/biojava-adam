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
package org.biojava.nbio.adam.convert;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Dbxref;
import org.bdgenomics.formats.avro.Feature;
import org.bdgenomics.formats.avro.OntologyTerm;
import org.bdgenomics.formats.avro.Read;
import org.bdgenomics.formats.avro.Sequence;
import org.bdgenomics.formats.avro.Strand;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;

import org.biojava.nbio.genome.io.fastq.Fastq;

/**
 * Guice module for the org.biojava.nbio.adam.convert package.
 *
 * @author  Michael Heuer
 */
@Immutable
public final class BiojavaModule extends AbstractModule {
    @Override
    protected void configure() {
        // empty
    }

    @Provides @Singleton
    Converter<org.biojava.nbio.core.sequence.Strand, Strand> createBiojavaStrandToBdgenomicsStrand() {
        return new BiojavaStrandToBdgenomicsStrand();
    }

    @Provides @Singleton
    Converter<Fastq, Read> createFastqToRead() {
        return new FastqToRead();
    }

    @Provides @Singleton
    Converter<Read, Fastq> createReadToFastq() {
        return new ReadToFastq();
    }

    @Provides @Singleton
    Converter<DNASequence, List<Feature>> createDnaSequenceToFeatures(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter) {
        return new DnaSequenceToFeatures(strandConverter);
    }

    @Provides @Singleton
    Converter<ProteinSequence, List<Feature>> createProteinSequenceToFeatures(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter) {
        return new ProteinSequenceToFeatures(strandConverter);
    }

    @Provides @Singleton
    Converter<RNASequence, List<Feature>> createRnaSequenceToFeatures(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter) {
        return new RnaSequenceToFeatures(strandConverter);
    }

    @Provides @Singleton
    Converter<DNASequence, Sequence> createDnaSequenceToSequence() {
        return new DnaSequenceToSequence();
    }

    @Provides @Singleton
    Converter<ProteinSequence, Sequence> createProteinSequenceToSequence() {
        return new ProteinSequenceToSequence();
    }

    @Provides @Singleton
    Converter<RNASequence, Sequence> createRnaSequenceToSequence() {
        return new RnaSequenceToSequence();
    }
}
