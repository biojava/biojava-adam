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

import static org.biojava.nbio.adam.convert.ConvertUtils.trimNewlines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import org.bdgenomics.convert.Converter;
import org.bdgenomics.convert.AbstractConverter;
import org.bdgenomics.convert.ConversionException;
import org.bdgenomics.convert.ConversionStringency;

import org.bdgenomics.formats.avro.Dbxref;
import org.bdgenomics.formats.avro.Feature;
import org.bdgenomics.formats.avro.Strand;

import org.biojava.nbio.core.sequence.RNASequence;

import org.biojava.nbio.core.sequence.features.FeatureInterface;
import org.biojava.nbio.core.sequence.features.Qualifier;

import org.biojava.nbio.core.sequence.location.template.AbstractLocation;
import org.biojava.nbio.core.sequence.location.template.Point;

import org.slf4j.Logger;

/**
 * Convert Biojava RNASequence to a list of bdg-formats Features.
 *
 * @author  Michael Heuer
 */
@Immutable
final class RnaSequenceToFeatures extends AbstractConverter<RNASequence, List<Feature>> {

    /** Convert Biojava Strand to bdg-formats Strand. */
    private final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter;


    /**
     * Convert Biojava RNASequence to a list of bdg-formats Features.
     *
     * @param strandConverter convert Biojava Strand to bdg-formats Strand, must not be null
     */
    RnaSequenceToFeatures(final Converter<org.biojava.nbio.core.sequence.Strand, Strand> strandConverter) {
        super(RNASequence.class, List.class);

        checkNotNull(strandConverter);
        this.strandConverter = strandConverter;
    }


    @Override
    public List<Feature> convert(final RNASequence rnaSequence,
                                 final ConversionStringency stringency,
                                 final Logger logger) throws ConversionException {

        if (rnaSequence == null) {
            warnOrThrow(rnaSequence, "must not be null", null, stringency, logger);
            return null;
        }

        final Feature.Builder fb = Feature.newBuilder()
            .setReferenceName(rnaSequence.getAccession().toString());

        int size = rnaSequence.getFeatures().size();
        List<Feature> features = new ArrayList<Feature>(size);

        for (FeatureInterface feature : rnaSequence.getFeatures()) {

            if (feature.getSource() == null) {
                fb.setSource(feature.getSource());
            }
            else {
                fb.clearSource();
            }

            if (feature.getType() == null) {
                fb.setFeatureType(feature.getType());
            }
            else {
                fb.clearFeatureType();
            }

            AbstractLocation location = feature.getLocations();

            if (location == null) {
                fb.clearStart();
                fb.clearEnd();
                fb.clearStrand();
            }
            else {
                Point start = location.getStart();
                fb.setStart(start.getPosition() - 1L);

                Point end = location.getEnd();
                fb.setEnd((long) end.getPosition());

                org.biojava.nbio.core.sequence.Strand strand = location.getStrand();
                if (strand == null) {
                    fb.clearStrand();
                }
                else {
                    fb.setStrand(strandConverter.convert(strand, stringency, logger));
                }
            }

            if (feature.getShortDescription() != null) {
                fb.setName(trimNewlines(feature.getShortDescription()));
            }
            else if (feature.getDescription() != null) {
                fb.setName(trimNewlines(feature.getDescription()));
            }
            else {
                fb.clearName();
            }

            // /db_xref=  --> feature.dbxrefs
            List<String> notes = new ArrayList<String>();
            // /note=  --> feature.notes
            List<Dbxref> dbxrefs = new ArrayList<Dbxref>();
            // remaining qualifiers
            Map<String, String> attributes = new HashMap<String, String>();
            Map<String, List<Qualifier>> qualifiers = feature.getQualifiers();

            for (Map.Entry<String, List<Qualifier>> entry : qualifiers.entrySet()) {
                String key = entry.getKey();
                List<Qualifier> value = entry.getValue();
                StringBuilder sb = new StringBuilder();
                for (Iterator<Qualifier> i = value.iterator(); i.hasNext(); ) {
                    String stringValue = trimNewlines(i.next().getValue());

                    if ("db_xref".equals(key)) {
                        String[] tokens = stringValue.split(":");
                        if (tokens.length == 2) {
                            dbxrefs.add(new Dbxref(tokens[0], tokens[1]));
                        }
                    }
                    else if ("note".equals(key)) {
                        notes.add(stringValue);
                    }
                    else {
                        sb.append(stringValue);
                        if (i.hasNext()) {
                            sb.append(",");
                        }
                    }
                }
                if ("db_xref".equals(key)) {
                    fb.setDbxrefs(dbxrefs);
                }
                else if ("note".equals(key)) {
                    fb.setNotes(notes);
                }
                else {
                    attributes.put(key, sb.toString());
                }
            }
            if (!attributes.isEmpty()) {
                fb.setAttributes(attributes);
            }

            features.add(fb.build());
        }

        return features;
    }
}
