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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Read;

import org.biojava.nbio.genome.io.fastq.Fastq;
import org.biojava.nbio.genome.io.fastq.FastqVariant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for ReadToFastq.
 *
 * @author  Michael Heuer
 */
public final class ReadToFastqTest {
    private final Logger logger = LoggerFactory.getLogger(ReadToFastqTest.class);
    private Converter<Read, Fastq> fastqConverter;

    @Before
    public void setUp() throws Exception {
        fastqConverter = new ReadToFastq();
    }

    @Test
    public void testConstructor() {
        assertNotNull(fastqConverter);
    }

    @Test(expected=ConversionException.class)
    public void testConvertNullStrict() {
        fastqConverter.convert(null, ConversionStringency.STRICT, logger);
    }

    @Test
    public void testConvertNullLenient() {
        assertNull(fastqConverter.convert(null, ConversionStringency.LENIENT, logger));
    }

    @Test
    public void testConvertNullSilent() {
        assertNull(fastqConverter.convert(null, ConversionStringency.SILENT, logger));
    }

    @Test
    public void testConvert() {
        Read read = Read.newBuilder()
            .setName("read/1")
            .setSequence("actg")
            .setQualityScores("e896")
            .build();

        Fastq fastq = fastqConverter.convert(read, ConversionStringency.STRICT, logger);
        assertEquals(read.getName(), fastq.getDescription());
        assertEquals(read.getSequence(), fastq.getSequence());
        assertEquals(read.getQualityScores(), fastq.getQuality());
        assertEquals(FastqVariant.FASTQ_SANGER, fastq.getVariant());
    }
}
