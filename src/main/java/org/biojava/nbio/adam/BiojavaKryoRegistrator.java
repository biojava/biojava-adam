/*

    biojava-adam  Biojava and ADAM integration.
    Copyright (c) 2017-2018 held jointly by the individual authors.

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
package org.biojava.nbio.adam;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.bdgenomics.adam.serialization.ADAMKryoRegistrator;

import org.biojava.nbio.core.sequence.compound.AminoAcidCompoundSet;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.RNACompoundSet;

/**
 * Biojava Kryo registrator.
 *
 * @author  Michael Heuer
 */
public class BiojavaKryoRegistrator extends ADAMKryoRegistrator {

    @Override
    public void registerClasses(final Kryo kryo) {
        super.registerClasses(kryo);

        UnmodifiableCollectionsSerializer.registerSerializers(kryo);

        kryo.register(AminoAcidCompoundSet.class, new Serializer<AminoAcidCompoundSet>() {
            @Override
            public void write(final Kryo kryo, final Output output, final AminoAcidCompoundSet compoundSet) {
                // empty
            }
            @Override
            public AminoAcidCompoundSet read(final Kryo kryo, final Input input, final Class cls) {
                return AminoAcidCompoundSet.getAminoAcidCompoundSet();
            }
        });
        kryo.register(DNACompoundSet.class, new Serializer<DNACompoundSet>() {
            @Override
            public void write(final Kryo kryo, final Output output, final DNACompoundSet compoundSet) {
                // empty
            }
            @Override
            public DNACompoundSet read(final Kryo kryo, final Input input, final Class cls) {
                return DNACompoundSet.getDNACompoundSet();
            }
        });
        kryo.register(RNACompoundSet.class, new Serializer<RNACompoundSet>() {
            @Override
            public void write(final Kryo kryo, final Output output, final RNACompoundSet compoundSet) {
                // empty
            }
            @Override
            public RNACompoundSet read(final Kryo kryo, final Input input, final Class cls) {
                return RNACompoundSet.getRNACompoundSet();
            }
        });
    }
}
