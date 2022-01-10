/*

    biojava-adam  BioJava and ADAM integration.
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
import org.slf4j.LoggerFactory
val logger = LoggerFactory.getLogger("loadGenbankProteinFeatures")

import org.apache.log4j.{ Level, Logger }
Logger.getLogger("loadGenbankProteinFeatures").setLevel(Level.INFO)
Logger.getLogger("org.biojava").setLevel(Level.INFO)

import org.biojava.nbio.adam.BiojavaAdamContext
val bac = BiojavaAdamContext(sc)

val inputPath = Option(System.getenv("INPUT"))
val outputPath = Option(System.getenv("OUTPUT"))

if (inputPath.isEmpty || outputPath.isEmpty) {
  logger.error("INPUT and OUTPUT environment variables are required")
  System.exit(1)
}

val features = bac.loadGenbankProteinFeatures(inputPath.get)

logger.info("Saving protein sequence features to output path %s ...".format(outputPath.get))
features.save(outputPath.get, asSingleFile = true, disableFastConcat = false)

logger.info("Done")
System.exit(0)
