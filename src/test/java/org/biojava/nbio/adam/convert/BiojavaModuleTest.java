/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2020 held jointly by the individual authors.

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

import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Guice;

import org.junit.Before;
import org.junit.Test;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.convert.bdgenomics.BdgenomicsModule;

import org.bdgenomics.formats.avro.Feature;
import org.bdgenomics.formats.avro.Read;
import org.bdgenomics.formats.avro.Sequence;
import org.bdgenomics.formats.avro.Strand;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;

import org.biojava.nbio.genome.io.fastq.Fastq;
import org.biojava.nbio.genome.io.fastq.FastqVariant;

/**
 * Unit test for BiojavaModule.
 *
 * @author  Michael Heuer
 */
public final class BiojavaModuleTest {
    private BiojavaModule module;

    @Before
    public void setUp() {
        module = new BiojavaModule();
    }

    @Test
    public void testConstructor() {
        assertNotNull(module);
    }

    @Test
    public void testBiojavaModule() {
        Injector injector = Guice.createInjector(module, new BdgenomicsModule(), new TestModule());
        Target target = injector.getInstance(Target.class);
        assertNotNull(target.getBiojavaStrandToBdgenomicsStrand());
        assertNotNull(target.getFastqToRead());
        assertNotNull(target.getReadToFastq());
        assertNotNull(target.getDnaSequenceToFeatures());
        assertNotNull(target.getDnaSequenceToSequence());
        assertNotNull(target.getProteinSequenceToFeatures());
        assertNotNull(target.getProteinSequenceToSequence());
        assertNotNull(target.getRnaSequenceToFeatures());
        assertNotNull(target.getRnaSequenceToSequence());
    }

    /**
     * Injection target.
     */
    static class Target {
        Converter<org.biojava.nbio.core.sequence.Strand, Strand> biojavaStrandToBdgenomicsStrand;
        Converter<Fastq, Read> fastqToRead;
        Converter<Read, Fastq> readToFastq;
        Converter<DNASequence, List<Feature>> dnaSequenceToFeatures;
        Converter<DNASequence, Sequence> dnaSequenceToSequence;
        Converter<ProteinSequence, List<Feature>> proteinSequenceToFeatures;
        Converter<ProteinSequence, Sequence> proteinSequenceToSequence;
        Converter<RNASequence, List<Feature>> rnaSequenceToFeatures;
        Converter<RNASequence, Sequence> rnaSequenceToSequence;

        @Inject
        Target(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> biojavaStrandToBdgenomicsStrand,
               final Converter<Fastq, Read> fastqToRead,
               final Converter<Read, Fastq> readToFastq,
               final Converter<DNASequence, List<Feature>> dnaSequenceToFeatures,
               final Converter<DNASequence, Sequence> dnaSequenceToSequence,
               final Converter<ProteinSequence, List<Feature>> proteinSequenceToFeatures,
               final Converter<ProteinSequence, Sequence> proteinSequenceToSequence,
               final Converter<RNASequence, List<Feature>> rnaSequenceToFeatures,
               final Converter<RNASequence, Sequence> rnaSequenceToSequence) {

            this.biojavaStrandToBdgenomicsStrand = biojavaStrandToBdgenomicsStrand;
            this.fastqToRead = fastqToRead;
            this.readToFastq = readToFastq;
            this.dnaSequenceToFeatures = dnaSequenceToFeatures;
            this.dnaSequenceToSequence = dnaSequenceToSequence;
            this.proteinSequenceToFeatures = proteinSequenceToFeatures;
            this.proteinSequenceToSequence = proteinSequenceToSequence;
            this.rnaSequenceToFeatures = rnaSequenceToFeatures;
            this.rnaSequenceToSequence = rnaSequenceToSequence;
        }

        Converter<org.biojava.nbio.core.sequence.Strand, Strand> getBiojavaStrandToBdgenomicsStrand() {
            return biojavaStrandToBdgenomicsStrand;
        }

        Converter<Fastq, Read> getFastqToRead() {
            return fastqToRead;
        }

        Converter<Read, Fastq> getReadToFastq() {
            return readToFastq;
        }

        Converter<DNASequence, List<Feature>> getDnaSequenceToFeatures() {
            return dnaSequenceToFeatures;
        }

        Converter<DNASequence, Sequence> getDnaSequenceToSequence() {
            return dnaSequenceToSequence;
        }

        Converter<ProteinSequence, List<Feature>> getProteinSequenceToFeatures() {
            return proteinSequenceToFeatures;
        }

        Converter<ProteinSequence, Sequence> getProteinSequenceToSequence() {
            return proteinSequenceToSequence;
        }

        Converter<RNASequence, List<Feature>> getRnaSequenceToFeatures() {
            return rnaSequenceToFeatures;
        }

        Converter<RNASequence, Sequence> getRnaSequenceToSequence() {
            return rnaSequenceToSequence;
        }
    }

    /**
     * Test module.
     */
    class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Target.class);
        }
    }
}
