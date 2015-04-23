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

package splitstree.util;

import java.io.FileWriter;
import java.io.IOException;

/**
 * bare bones cgviz writer
 *
 * @author huson
 *         Date: 06-Dec-2004
 */

public class CGVizWriter {
    private FileWriter w;

    /**
     * Constructor
     *
     * @param fileName
     */
    public CGVizWriter(String fileName) {
        try {
            w = new FileWriter(fileName);
        } catch (IOException e) {
        }
    }

    /**
     * start a block
     *
     * @param type
     * @param name
     * @param dimension
     */
    public void begin(String type, String name, int dimension) {
        try {
            w.write("{" + type + " " + name + "\n");
            w.write("[__GLOBAL__] dimension=" + dimension + ":\n");
        } catch (IOException e) {
        }
    }

    /**
     * add a line to a block
     *
     * @param line
     */
    public void add(String line) {
        try {
            w.write(line + "\n");
        } catch (IOException e) {
        }
    }

    /**
     * end a block
     */
    public void end() {
        try {
            w.write("}\n");
        } catch (IOException e) {
        }
    }

    /**
     * close the writer
     */
    public void close() {
        try {
            w.flush();
            w.close();
        } catch (IOException e) {
        }
        w = null;
    }
}

