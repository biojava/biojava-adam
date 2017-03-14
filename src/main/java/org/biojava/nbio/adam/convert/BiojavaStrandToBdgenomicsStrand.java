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
package org.biojava.nbio.adam.convert;

import javax.annotation.concurrent.Immutable;

import org.bdgenomics.convert.AbstractConverter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.slf4j.Logger;

/**
 * Convert BioJava Strand to bdg-formats Strand.
 *
 * @author  Michael Heuer
 */
@Immutable
final class BiojavaStrandToBdgenomicsStrand extends AbstractConverter<org.biojava.nbio.core.sequence.Strand, org.bdgenomics.formats.avro.Strand> {

    /**
     * Package private no-arg constructor.
     */
    BiojavaStrandToBdgenomicsStrand() {
        super(org.biojava.nbio.core.sequence.Strand.class, org.bdgenomics.formats.avro.Strand.class);
    }


    @Override
    public org.bdgenomics.formats.avro.Strand convert(final org.biojava.nbio.core.sequence.Strand strand,
                                                      final ConversionStringency stringency,
                                                      final Logger logger) throws ConversionException {

        if (strand == null) {
            warnOrThrow(strand, "must not be null", null, stringency, logger);
            return null;
        }
        switch (strand) {
            case NEGATIVE:
                return org.bdgenomics.formats.avro.Strand.REVERSE;
            case POSITIVE:
                return org.bdgenomics.formats.avro.Strand.FORWARD;
            case UNDEFINED:
                return org.bdgenomics.formats.avro.Strand.UNKNOWN;
        }
        return null;
    }
}
