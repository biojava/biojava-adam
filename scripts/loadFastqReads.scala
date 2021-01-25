/*

    biojava-adam  BioJava and ADAM integration.
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
import org.slf4j.LoggerFactory
val logger = LoggerFactory.getLogger("loadFastqReads")

import org.apache.log4j.{ Level, Logger }
Logger.getLogger("loadFastqReads").setLevel(Level.INFO)
Logger.getLogger("org.biojava").setLevel(Level.INFO)

import org.biojava.nbio.adam.BiojavaAdamContext
val bac = BiojavaAdamContext(sc)

val inputPath = Option(System.getenv("INPUT"))
val outputPath = Option(System.getenv("OUTPUT"))

if (!inputPath.isDefined || !outputPath.isDefined) {
  logger.error("INPUT and OUTPUT environment variables are required")
  System.exit(1)
}

val reads = bac.loadFastqReads(inputPath.get)

logger.info("Saving reads to output path %s ...".format(outputPath.get))
reads.save(outputPath.get, asSingleFile = true)

logger.info("Done")
System.exit(0)
