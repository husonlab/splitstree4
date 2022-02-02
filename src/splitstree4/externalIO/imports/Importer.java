/*
 * Importer.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package splitstree4.externalIO.imports;

import java.io.Reader;

/**
 * Importer interface
 *
 * @author huson
 * Date: Sep 29, 2003
 * @version $Id: Importer.java,v 1.7 2005-11-12 20:49:13 huson Exp $
 */
public interface Importer {

    String Description = "No description given.";

    /**
     * can we import this data?
     *
     * @return true, if can handle this import
     */
    boolean isApplicable(Reader input) throws Exception;

    /**
     * does this importer apply to the type of nexus block
     *
     * @return true, if can handle this import
     */
    boolean isApplicableToBlock(String blockName);

    /**
     * convert input into nexus format
     *
	 */
    String apply(Reader input) throws Exception;


    /**
     * @return description of file matching the filter
     */
    String getDescription();

    /**
     * set the data type
     *
	 */
    void setDatatype(String dataType);

    /**
     * gets the data type
     *
     * @return data type
     */
    String getDatatype();

}
