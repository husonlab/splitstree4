/*
 * CGVizWriter.java Copyright (C) 2022 Daniel H. Huson
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
package splitstree4.util;

import java.io.FileWriter;
import java.io.IOException;

/**
 * bare bones cgviz writer
 *
 * @author huson
 * Date: 06-Dec-2004
 */

public class CGVizWriter {
    private FileWriter w;

    /**
     * Constructor
     *
	 */
    public CGVizWriter(String fileName) {
		try {
			w = new FileWriter(fileName);
		} catch (IOException ignored) {
		}
    }

    /**
     * start a block
     *
	 */
    public void begin(String type, String name, int dimension) {
		try {
			w.write("{" + type + " " + name + "\n");
			w.write("[__GLOBAL__] dimension=" + dimension + ":\n");
		} catch (IOException ignored) {
		}
    }

    /**
     * add a line to a block
     *
	 */
    public void add(String line) {
		try {
			w.write(line + "\n");
		} catch (IOException ignored) {
		}
    }

    /**
     * end a block
     */
    public void end() {
		try {
			w.write("}\n");
		} catch (IOException ignored) {
		}
    }

    /**
     * close the writer
     */
    public void close() {
		try {
			w.flush();
			w.close();
		} catch (IOException ignored) {
		}
        w = null;
    }
}

