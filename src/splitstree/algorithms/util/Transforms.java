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

package splitstree.algorithms.util;

import splitstree.algorithms.Transformation;
import splitstree.core.Document;
import splitstree.util.PluginClassLoader;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * utitily for getting all named transforms
 * Daniel Huson and David Bryant
 */
public class Transforms {
    private static Map<String, Transformation> name2trans = new HashMap<>();
    private static Map<Transformation, String> trans2inputBlock = new HashMap<>();
    private static Map<Transformation, String> trans2outputBlock = new HashMap<>();

    /**
     * get all  transforms for a given name
     *
     * @param name
     * @return list of all transforms for the given name
     */
    public static List<Transformation> getAllTransforms(String name) {
        List<Transformation> result = new LinkedList<>();

        if (name.contains(".")) {
            try {
                if (name2trans.get(name) == null) {
                    Class theClass = Class.forName(name);
                    Transformation trans = (Transformation) theClass.newInstance();
                    name2trans.put(name, trans);
                    setInputOutputBlocks(name, trans);
                }
                result.add(name2trans.get(name));
            } catch (Exception ex) {
            }
        } else {
            // check if it is a plugin
            if (PluginClassLoader.getPluginName2URLClassLoader().containsKey(name)) {
                try {
                    System.err.println("Transform: found PluginData: " + name);
                    Class theClass = Class.forName(name, true, (URLClassLoader) PluginClassLoader.getPluginName2URLClassLoader().get(name));
                } catch (Exception ex) {
                }
            } else {
                for (String str : Document.getListOfBlockNames()) {
                    try {
                        String block = str.toLowerCase();

                        String fullName = "splitstree.algorithms." + block + "." + name;
                        if (name2trans.get(fullName) == null) {
                            Class theClass = Class.forName(fullName);
                            Transformation trans = (Transformation) theClass.newInstance();
                            name2trans.put(fullName, trans);
                            setInputOutputBlocks(fullName, trans);
                        }
                        result.add(name2trans.get(fullName));
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return result;
    }

    private static void setInputOutputBlocks(String fullName, Transformation trans) {
        String path = fullName.substring(0, fullName.lastIndexOf("."));

        String[] names = Document.getListOfBlockNames().toArray(new String[Document.getListOfBlockNames().size()]);

        for (int i = 0; i < names.length; i++) {
            for (int j = i + 1; j < names.length; j++) {
                try {
                    String interfaceName = path + "." + names[i] + "2" + names[j];
                    Class theInterface = Class.forName(interfaceName);

                    if (theInterface.isInstance(trans)) {
                        trans2inputBlock.put(trans, names[i]);
                        trans2outputBlock.put(trans, names[j]);
                    }
                } catch (Exception ex) {
                }
            }
        }
    }

    public static String getInputBlockName(Transformation trans) {
        return trans2inputBlock.get(trans);
    }

    public static String getOutputBlockName(Transformation trans) {
        return trans2outputBlock.get(trans);
    }
}
