/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2022 held jointly by the individual authors.

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

import javax.annotation.concurrent.Immutable;

import org.bdgenomics.convert.AbstractConverter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Alphabet;
import org.bdgenomics.formats.avro.Read;

import org.biojava.nbio.genome.io.fastq.Fastq;

import org.slf4j.Logger;

/**
 * Convert Biojava Fastq to bdg-formats Read.
 *
 * @author  Michael Heuer
 */
@Immutable
final class FastqToRead extends AbstractConverter<Fastq, Read> {

    /**
     * Package private no-arg constructor.
     */
    FastqToRead() {
        super(Fastq.class, Read.class);
    }


    @Override
    public Read convert(final Fastq fastq,
                        final ConversionStringency stringency,
                        final Logger logger) throws ConversionException {

        if (fastq == null) {
            warnOrThrow(fastq, "must not be null", null, stringency, logger);
            return null;
        }
        return Read.newBuilder()
            .setName(nameFromDescription(fastq.getDescription()))
            .setDescription(fastq.getDescription())
            .setAlphabet(Alphabet.DNA)
            .setSequence(fastq.getSequence().toUpperCase())
            .setLength((long) fastq.getSequence().length())
            .setQualityScores(fastq.getQuality())
            .build();
    }

    static String nameFromDescription(final String description) {
        if (description == null) {
            return null;
        }
        if (description.length() == 0) {
            return "";
        }
        int i = description.indexOf(" ");
        return (i >= 0) ? description.substring(0, i) : description;
    }
}
