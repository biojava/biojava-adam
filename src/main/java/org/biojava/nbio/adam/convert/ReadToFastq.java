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

import org.bdgenomics.formats.avro.Read;

import org.biojava.nbio.genome.io.fastq.Fastq;
import org.biojava.nbio.genome.io.fastq.FastqBuilder;
import org.biojava.nbio.genome.io.fastq.FastqVariant;

import org.slf4j.Logger;

/**
 * Convert bdg-formats Read to Biojava Fastq.
 *
 * @author  Michael Heuer
 */
@Immutable
final class ReadToFastq extends AbstractConverter<Read, Fastq> {

    /**
     * Package private no-arg constructor.
     */
    ReadToFastq() {
        super(Read.class, Fastq.class);
    }


    @Override
    public Fastq convert(final Read read,
                         final ConversionStringency stringency,
                         final Logger logger) throws ConversionException {

        if (read == null) {
            warnOrThrow(read, "must not be null", null, stringency, logger);
            return null;
        }
        Fastq fastq = null;
        try {
            fastq = new FastqBuilder()
                .withDescription(description(read.getName(), read.getDescription()))
                .withSequence(read.getSequence())
                .withQuality(read.getQualityScores())
                .withVariant(FastqVariant.FASTQ_SANGER)
                .build();
        }
        catch (NullPointerException | IllegalArgumentException e) {
            warnOrThrow(read, "could not convert read", e, stringency, logger); 
        }
        return fastq;
    }

    static String description(final String name, final String description) {
        if (name == null && description == null) {
            return null;
        }
        if (description == null) {
            return name;
        }
        if (name == null) {
            return description;
        }
        return name + " " + description;
    }
}
