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
package org.biojava.nbio.adam

import java.io.Closeable
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

/**
 * Try with closeable resources.
 */
private[adam] object TryWith {
  def apply[C <: Closeable, R](closeable: => C)(f: C => R): Try[R] =
    Try(closeable).flatMap(c => {
      try {
        val rv = f(c)
        Try(c.close()).map(_ => rv)
      }
      catch {
        case NonFatal(exceptionInFunction) =>
          try {
            c.close()
            Failure(exceptionInFunction)
          }
          catch {
            case NonFatal(exceptionInClose) =>
              exceptionInFunction.addSuppressed(exceptionInClose)
              Failure(exceptionInFunction)
          }
      }
    })
}
