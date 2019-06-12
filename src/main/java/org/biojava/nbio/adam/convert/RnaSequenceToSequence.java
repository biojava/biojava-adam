/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2019 held jointly by the individual authors.

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
import org.bdgenomics.formats.avro.Sequence;

import org.biojava.nbio.core.sequence.RNASequence;

import org.slf4j.Logger;

/**
 * Convert Biojava RNASequence to bdg-formats Sequence.
 *
 * @author  Michael Heuer
 */
@Immutable
final class RnaSequenceToSequence extends AbstractConverter<RNASequence, Sequence> {

    /**
     * Convert Biojava RNASequence to bdg-formats Sequence.
     */
    RnaSequenceToSequence() {
        super(RNASequence.class, Sequence.class);
    }


    @Override
    public Sequence convert(final RNASequence rnaSequence,
                            final ConversionStringency stringency,
                            final Logger logger) throws ConversionException {

        if (rnaSequence == null) {
            warnOrThrow(rnaSequence, "must not be null", null, stringency, logger);
            return null;
        }

        Sequence.Builder sb = Sequence.newBuilder()
            .setName(rnaSequence.getAccession().toString())
            .setDescription(rnaSequence.getDescription())
            .setAlphabet(Alphabet.RNA)
            .setSequence(rnaSequence.getSequenceAsString().toUpperCase())
            .setLength((long) rnaSequence.getLength());

        return sb.build();
    }
}
