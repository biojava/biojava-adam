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

import org.bdgenomics.formats.avro.Strand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for BiojavaStrandToBdgenomicsStrand.
 *
 * @author  Michael Heuer
 */
public final class BiojavaStrandToBdgenomicsStrandTest {
    private final Logger logger = LoggerFactory.getLogger(BiojavaStrandToBdgenomicsStrandTest.class);
    private Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter;

    @Before
    public void setUp() {
        strandConverter = new BiojavaStrandToBdgenomicsStrand();
    }

    @Test
    public void testConstructor() {
        assertNotNull(strandConverter);
    }

    @Test(expected=ConversionException.class)
    public void testConvertNullStrict() {
        strandConverter.convert(null, ConversionStringency.STRICT, logger);
    }

    @Test
    public void testConvertNullLenient() {
        assertNull(strandConverter.convert(null, ConversionStringency.LENIENT, logger));
    }

    @Test
    public void testConvertNullSilent() {
        assertNull(strandConverter.convert(null, ConversionStringency.SILENT, logger));
    }

    @Test
    public void testConvert() {
        assertEquals(Strand.REVERSE, strandConverter.convert(org.biojava.nbio.core.sequence.Strand.NEGATIVE, ConversionStringency.STRICT, logger));
        assertEquals(Strand.FORWARD, strandConverter.convert(org.biojava.nbio.core.sequence.Strand.POSITIVE, ConversionStringency.STRICT, logger));
        assertEquals(Strand.UNKNOWN, strandConverter.convert(org.biojava.nbio.core.sequence.Strand.UNDEFINED, ConversionStringency.STRICT, logger));
    }
}
