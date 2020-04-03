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

import static org.biojava.nbio.adam.convert.ConvertUtils.trimNewlines;

import javax.annotation.concurrent.Immutable;

import org.bdgenomics.convert.AbstractConverter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Alphabet;
import org.bdgenomics.formats.avro.Sequence;

import org.biojava.nbio.core.sequence.DNASequence;

import org.slf4j.Logger;

/**
 * Convert Biojava DNASequence to bdg-formats Sequence.
 *
 * @author  Michael Heuer
 */
@Immutable
final class DnaSequenceToSequence extends AbstractConverter<DNASequence, Sequence> {

    /**
     * Convert Biojava DNASequence to bdg-formats Sequence.
     */
    DnaSequenceToSequence() {
        super(DNASequence.class, Sequence.class);
    }


    @Override
    public Sequence convert(final DNASequence dnaSequence,
                            final ConversionStringency stringency,
                            final Logger logger) throws ConversionException {

        if (dnaSequence == null) {
            warnOrThrow(dnaSequence, "must not be null", null, stringency, logger);
            return null;
        }

        Sequence.Builder sb = Sequence.newBuilder()
            .setName(dnaSequence.getAccession().toString())
            .setDescription(trimNewlines(dnaSequence.getDescription()))
            .setAlphabet(Alphabet.DNA)
            .setSequence(dnaSequence.getSequenceAsString().toUpperCase())
            .setLength((long) dnaSequence.getLength());

        return sb.build();
    }
}
