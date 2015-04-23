/**
 * Copyright 2015, Daniel Huson and David Bryant
 *
 *(Some files contain contributions from other authors, who are then mentioned separately)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package splitstree.externalIO.imports;

import java.io.Reader;

/**
 * Importer interface
 *
 * @author huson
 *         Date: Sep 29, 2003
 * @version $Id: Importer.java,v 1.7 2005-11-12 20:49:13 huson Exp $
 */
public interface Importer {

    String Description = "No description given.";

    /**
     * can we import this data?
     *
     * @param input
     * @return true, if can handle this import
     */
    boolean isApplicable(Reader input) throws Exception;

    /**
     * does this importer apply to the type of nexus block
     *
     * @param blockName
     * @return true, if can handle this import
     */
    boolean isApplicableToBlock(String blockName);

    /**
     * convert input into nexus format
     *
     * @param input
     * @return
     */
    String apply(Reader input) throws Exception;


    /**
     * @return description of file matching the filter
     */
    String getDescription();

    /**
     * set the data type
     *
     * @param dataType
     */
    void setDatatype(String dataType);

    /**
     * gets the data type
     *
     * @return data type
     */
    String getDatatype();

}
