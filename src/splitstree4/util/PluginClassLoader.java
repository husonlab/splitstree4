/*
 * PluginClassLoader.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.swing.graphview.Transform;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceUtils;
import splitstree4.algorithms.util.Configurator;
import splitstree4.main.SplitsTreeProperties;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Finds all classes in the named package, of the given type
 *
 * @author huson
 *         Date: 04-Dec-2003
 */
public class PluginClassLoader {
    private static final boolean debug = false;

    // Extended the PluginClassLoader to include the Plugins from the pluginFolder
    static private HashMap<String, URLClassLoader> pluginName2URLClassLoader = new HashMap<>();

    public static List<Object> getInstances(String packageName, Class type) {
        //MZ: 2006-01-28
        List<Object> plugins = new LinkedList<>();
        String[] resources;
        try {
            resources = ResourceUtils.fetchResources(PluginClassLoader.class, packageName);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i != resources.length; ++i) {
            if (resources[i].endsWith(".class"))
                try {
                    resources[i] = resources[i].substring(0, resources[i].length() - 6);
                    Class c = classForName(PluginClassLoader.class, packageName.concat(".").concat(resources[i]));
                    if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers()) && type.isAssignableFrom(c)) {
                        if (!SplitsTreeProperties.getExpertMode() && Configurator.hasField(c, "EXPERT"))
                            continue; // ignore all expert plugins, unless in expert mode

                        Object obj;
						try {
							obj = c.newInstance();
							plugins.add(obj);
						} catch (InstantiationException ignored) {
						}
                    }
                } catch (Exception ex) {
                    // Basic.caught(ex);
                }
        }
        plugins.addAll(loadPlugins(type));
        return plugins;
    }

    private static List<Object> loadPlugins(Class type) {
        LinkedList<Object> plugins = new LinkedList<>();
        try {
            String dir = ProgramProperties.get("PluginFolder");
            if (debug) System.out.println("pluginFolder: " + dir);
            if (dir != null) {
                File pluginDir = new File(dir);
                if (pluginDir != null && pluginDir.exists() && pluginDir.isDirectory()) {
					File[] externalPlugins = pluginDir.listFiles(in -> {
						if (in.isDirectory() && !in.toString().endsWith("offline")) {
							File[] classes = in.listFiles(in1 -> in1.toString().endsWith(".class"));
							if (classes.length == 1)
								return true;
							else {
								if (debug)
									System.err.println("No class found for pluginFolder: " + in);  // @todo only System.err if vdrbose? and add plugin description file to ckeck
								return false;
							}
						} else
							return false;
					});
					for (File externalPlugin : externalPlugins) {
						if (debug) System.err.println("externalPlugin: " + externalPlugin);
						ArrayList<URL> externalPluginUrls = new ArrayList<>();
						boolean add = true;
						try {// first add basic folder
							externalPluginUrls.add(externalPlugin.toURL());
						} catch (MalformedURLException e) {
							System.err.println("unable to add plugin: " + externalPlugin + " because: ");
							e.printStackTrace();
							add = false;
                        }
                        // second add all jars from the basic folder
						File[] jars = externalPlugin.listFiles(in -> in.toString().endsWith(".jar"));
                        for (File jar : jars) {
                            try {
                                externalPluginUrls.add(jar.toURL());
                            } catch (MalformedURLException e) {
                                System.err.println("unable to add plugin: " + externalPlugin + " because: ");
                                e.printStackTrace();
                                add = false;
                            }
                        }
                        if (add) {
                            if (debug) System.err.println("Loading plugin: " + externalPlugin);
                            // load class
							File[] classes = externalPlugin.listFiles(in -> in.toString().endsWith(".class"));
							URLClassLoader ucl = new URLClassLoader(externalPluginUrls.toArray(new URL[1]), ClassLoader.getSystemClassLoader());
                            for (File aClass : classes) {
                                String className = aClass.getName().substring(0, aClass.getName().lastIndexOf("."));
                                if (debug) System.err.println(className);
                                try {
                                    Class c = Class.forName(className, true, ucl);
                                    pluginName2URLClassLoader.put(className, ucl);
                                    if (!c.isInterface()
                                            && !Modifier.isAbstract(c.getModifiers())
                                            && type.isAssignableFrom(c)) {
										if (!SplitsTreeProperties.getExpertMode() && Configurator.hasField(c, "EXPERT"))
											continue; // ignore all expert plugins, unless in expert mode
										if (debug) System.err.println("Loading pluging: " + className + " successful");
										Object obj;
										try {
											obj = c.newInstance();
											plugins.add(obj);
										} catch (InstantiationException ignored) {
										}
									}
								} catch (NoClassDefFoundError ignored) {

								}
                            }
                        }
                    }
                } else {
                    // System.err.println("PluginClassLoader: Pluginfolder " + pluginDir + " not found!");
                }
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return plugins;
    }


    public static List getInstancesSorted(String packageName, Class type) {
		List<Object> plugins = PluginClassLoader.getInstances(packageName, type);

		Object[] array = plugins.toArray();

		Arrays.sort(array, (o1, o2) -> {
			// First compare the interface... if equal, compare the name

			Class[] int1 = o1.getClass().getInterfaces();
			Class[] int2 = o2.getClass().getInterfaces();
			String name1;
			String name2;

			if (int1.length == 0 || int2.length == 0) {
				if (int1.length == 0 && int2.length > 0)
					return 1;
				else if (int1.length > 0 && int2.length == 0)
					return -1;
				else
					return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
			if (int1[0] == int2[0]) {
				// Compare the names of the classes if the same interface
				name1 = o1.getClass().getName();
				name2 = o2.getClass().getName();
			} else {
				// Compare the names of the interfaces if not the same
				name1 = int1[0].getName(); // Only look at the first it implements
				name2 = int2[0].getName();
			}
			return name1.compareTo(name2);

		});
        return Arrays.asList(array);
    }

    public static HashMap getPluginName2URLClassLoader() {
        if (pluginName2URLClassLoader.size() == 0) {
            PluginClassLoader.loadPlugins(Transform.class);
        }
        return pluginName2URLClassLoader;
    }

    public static void updatePluginURLClassLoader() {
        pluginName2URLClassLoader.clear();
        PluginClassLoader.loadPlugins(Transform.class);
    }

    public static void setPluginName2URLClassLoader(HashMap<String, URLClassLoader> pluginClass2URLClassLoader) {
        PluginClassLoader.pluginName2URLClassLoader = pluginClass2URLClassLoader;
    }

    /**
     * Get a class instance for the given fully qualified classname.
     *
	 */
    public static Class classForName(Class clazz, String name) throws ClassNotFoundException {
        return clazz.getClassLoader().loadClass(name);

    }
}

