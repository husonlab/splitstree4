/**
 * Configurator.java
 * Copyright (C) 2015 Daniel H. Huson and David J. Bryant
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * Configures transformation algorithms
 *
 * @version $Id: Configurator.java,v 1.32 2010-02-23 15:52:01 huson Exp $
 * @author Markus Franz and Daniel Huson and David Bryant
 * <p>
 * Configures transformation algorithms
 * @version $Id: Configurator.java,v 1.32 2010-02-23 15:52:01 huson Exp $
 * @author Markus Franz and Daniel Huson and David Bryant
 */
/**
 * Configures transformation algorithms
 *
 *@version $Id: Configurator.java,v 1.32 2010-02-23 15:52:01 huson Exp $
 *
 *@author Markus Franz and Daniel Huson and David Bryant
 */
package splitstree4.algorithms.util;

import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import splitstree4.core.ConfiguratorParseException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Configures the transformation algorithms
 */
public class Configurator {
    private static final char LEFTDEL = '\"'; // left delimiter for mixed-string option value
    private static final char RIGHTDEL = '\"'; // right delimiter   "

    /**
     * Gets a description of the usage of the given transform algorithm
     *
     * @param ct the transformation object (e.g. CharactersTransform, SplitsTransform, ...)
     * @return description of usage
     */
    public static String getUsage(Object ct) {
        String result = "";

        if (ct == null)
            return "";

        String name = ct.getClass().getName();
        if (name.lastIndexOf(".") != -1) name = name.substring(name.lastIndexOf(".") + 1);
        result += name;

        Method[] methods = ct.getClass().getMethods();

        for (Method m : methods) {
            if (m.getName().startsWith("setOption")) {
                result += " ";
                result += m.getName().substring(9);
                result += "=";
                Class[] parameters = m.getParameterTypes();
                for (Class parameter : parameters) {
                    result += "<";
                    String pName = parameter.getName();
                    switch (pName) {
                        case "[D":
                            result += "N double1 double2 ... doubleN";
                            break;
                        case "[[D":
                            result += "R C double11 double12 ... double1C ... doubleRC";
                            break;
                        case "[I":
                            result += "N int1 I2 ... intN";
                            break;
                        case "[[I":
                            result += "R C int11 int12 ... int1C ... intRC";
                            break;
                        case "[L":
                            result += "N word1 word2 ... wordN";
                            break;
                        case "[[L":
                            result += "R C word11 word12 ... word1C ... wordRC";
                            break;
                        default:
                            result += pName;
                            break;
                    }
                    result += ">";
                }
            }
        }
        return result;
    }


    /**
     * Gets a description of the usage of the given transform algorithm
     *
     * @param ct the transformation object (e.g. CharactersTransform, SplitsTransform, ...)
     * @return description of usage
     */
    public static String getShortUsage(Object ct) {
        String result = "";

        if (ct == null)
            return "";

        String name = ct.getClass().getName();
        if (name.lastIndexOf(".") != -1) name = name.substring(name.lastIndexOf(".") + 1);
        result += name;

        Method[] methods = ct.getClass().getMethods();

        for (Method m : methods) {
            if (m.getName().startsWith("setOption")) {
                result += " ";
                result += m.getName().substring(9);
                result += "=";
                Class[] parameters = m.getParameterTypes();
                for (Class parameter : parameters) {
                    result += "<" + parameter.getName() + ">";
                }
            }
        }
        return result;
    }

    /**
     * Gets a description of the currently set options
     * for the given transformation object
     *
     * @param ct the transformation object
     * @return description of set options
     */
    public static String getOptions(Object ct) {
        String result = "";

        if (ct == null)
            return "";

        String name = ct.getClass().getName();
        if (name.lastIndexOf(".") != -1) name = name.substring(name.lastIndexOf(".") + 1);
        result += name;

        Method[] methods = ct.getClass().getMethods();

        for (Method m : methods) {
            if (m.getName().startsWith("getOption")) {
                String optionName = m.getName().substring(9);
                StringBuilder buf = new StringBuilder();
                buf.append(" ").append(optionName).append("=");

                Object option = getOption(ct, optionName);
                if (isArray(option))
                    result += buf.toString() + toStringArray(option);
                else if (isArrayArray(option))
                    result += buf.toString() + toStringArrayArray(option);
                else if (isMixedString(option))
                    result += buf.toString() + LEFTDEL + option.toString().trim() + RIGHTDEL;
                else if (option != null && option.toString().trim().length() > 0)
                    result += buf.toString() + option.toString().trim();
            }
        }
        return result;
    }

    /**
     * returns true, if object is array
     *
     * @param object
     * @return true, if array
     */
    public static boolean isArray(Object object) {
        return (object != null && object.getClass().getName().startsWith("[") &&
                !object.getClass().getName().startsWith("[["));
    }

    /**
     * returns true, if array array
     *
     * @param object
     * @return boolean. True if this is a 2d array.
     */
    public static boolean isArrayArray(Object object) {
        return object != null && object.getClass().getName().startsWith("[[") &&
                !object.getClass().getName().startsWith("[[[");
    }

    /**
     * returns true, if object is a string containing spaces or special characters
     *
     * @param object
     * @return true,  if object is a string containing spaces or special characters
     */
    public static boolean isMixedString(Object object) {
        if (object == null)
            return false;
        try {
            Integer.parseInt(object.toString());
            return false; // is an integer
        } catch (Exception ex) {
        }
        try {
            Double.parseDouble(object.toString());
            return false; // is a double
        } catch (Exception ex) {
        }
        String str = object.toString().trim();
        if (str.length() <= 1)
            return false;
        for (int i = 0; i < str.length(); i++)
            if (!Character.isLetterOrDigit(str.charAt(i))
                    && str.charAt(i) != '_')
                return true;
        return false;
    }

    /**
     * converts array to string
     *
     * @param arrayObject
     * @return string
     */
    public static String toStringArray(Object arrayObject) {
        StringBuilder buf = new StringBuilder();
        switch (arrayObject.getClass().getName().charAt(1)) {
            case 'I': {
                int[] array = (int[]) arrayObject;
                buf.append(array.length);
                for (int anArray : array) buf.append(" ").append(anArray);
                break;
            }
            case 'D': {
                double[] array = (double[]) arrayObject;
                buf.append(array.length);
                for (double anArray : array) buf.append(" ").append(anArray);
                break;
            }
            case 'L':
            default: {
                Object[] array = (Object[]) arrayObject;
                buf.append(array.length);
                for (Object anArray : array) buf.append(" ").append(anArray);
                break;
            }
        }
        return buf.toString();
    }

    /**
     * converts array array to string
     *
     * @param arrayObject
     * @return string
     */
    public static String toStringArrayArray(Object arrayObject) {
        StringBuilder buf = new StringBuilder();
        switch (arrayObject.getClass().getName().charAt(2)) {
            case 'I': {
                int[][] array = (int[][]) arrayObject;
                buf.append(array.length).append(" ").append(array[0].length);
                for (int[] anArray : array)
                    for (int j = 0; j < array[0].length; j++)
                        buf.append(" ").append(anArray[j]);
                break;
            }
            case 'D': {
                double[][] array = (double[][]) arrayObject;
                buf.append(array.length).append(" ").append(array[0].length);
                for (double[] anArray : array)
                    for (int j = 0; j < array[0].length; j++)
                        buf.append(" ").append(anArray[j]);
                break;
            }
            case 'L':
            default: {
                Object[][] array = (Object[][]) arrayObject;
                buf.append(array.length).append(" ").append(array[0].length);
                for (Object[] anArray : array)
                    for (int j = 0; j < array[0].length; j++)
                        buf.append(" ").append(anArray[j]);
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Given a description of the options, sets them
     *
     * @param ct  the transformation object
     * @param str the options string
     */
    public static void setOptions(Object ct, String str)
            throws ConfiguratorParseException, IOException {
        if (str == null)
            return;

        NexusStreamParser sst = new NexusStreamParser(new StringReader(str));
        sst.setPunctuationCharacters(NexusStreamParser.SEMICOLON_PUNCTUATION);

        while (sst.nextToken() != NexusStreamParser.TT_EOF) {
            String optionName = null;
            String optionValue = null;

            try {
                optionName = sst.sval;
                sst.matchIgnoreCase("=");
                sst.nextToken();

                optionValue = sst.sval;
                if (optionValue.startsWith("" + LEFTDEL)) {
                    while (optionValue.length() < 2 || !optionValue.endsWith("" + RIGHTDEL)) {
                        if (sst.nextToken() == NexusStreamParser.TT_EOF)
                            throw new ConfiguratorParseException("Unexpected end-of-input while scanning for: '+" + RIGHTDEL + "'");
                        optionValue += " " + sst.sval;
                    }
                    // strip delimiters:
                    optionValue = optionValue.substring(1, optionValue.length() - 1);
                }

                boolean done = false;
                if (!done) {
                    char arrayType = Configurator.isArrayOption(ct, optionName);
                    if (arrayType != 0) {
                        int count = Integer.parseInt(optionValue);
                        List params = sst.getWordsRespectCase(count);
                        if (params.size() != count)
                            throw new Exception("wrong number of params: " + count + " vs " + params.size());
                        setOptionArray(ct, optionName, params, count);
                        done = true;
                    }
                }

                if (!done) {
                    char arrayType = Configurator.isArrayArrayOption(ct, optionName);
                    if (arrayType != 0) {
                        int rows = Integer.parseInt(optionValue);
                        sst.nextToken();
                        optionValue = sst.sval;
                        int cols = Integer.parseInt(optionValue);
                        int count = rows * cols;
                        List params = sst.getWordsRespectCase(count);
                        if (params.size() != count)
                            throw new Exception("wrong number of params: " + count + " vs " + params.size());

                        setOptionArrayArray(ct, optionName, params, rows, cols);
                        done = true;
                    }
                }

                if (!done) {
                    setOptionFromString(ct, optionName, optionValue);
                }
            } catch (Exception ex) {
                throw new ConfiguratorParseException("Set option failed: " + optionName + "="
                        + optionValue + "\nUsage: " + getUsage(ct) + " (" + ex.getMessage() + ")");
            }
        }
    }

    /**
     * Attempts to set an option
     *
     * @param ct          the transformation object
     * @param optionName  the name of the option
     * @param optionValue the value of the option
     */
    public static void setOptionFromString(Object ct, String optionName, String optionValue)
            throws ConfiguratorParseException {
        if (optionValue.length() > 1 && optionValue.charAt(0) == LEFTDEL)
            optionValue = optionValue.substring(1, optionValue.length() - 1); // chop first and last '"'
        // attempt to set the value:
        try {
            boolean value = Boolean.valueOf(optionValue);
            setOption(ct, optionName, value);
            return;
        } catch (Exception ex) {
        }
        try {
            int value = Integer.parseInt(optionValue);
            setOption(ct, optionName, value);
            return;
        } catch (Exception ex) {
        }
        try {
            double value = Double.parseDouble(optionValue);
            setOption(ct, optionName, value);
            return;
        } catch (Exception ex) {
        }
        try {
            setOption(ct, optionName, optionValue);
            return;
        } catch (Exception ex) {
        }
        throw new ConfiguratorParseException("Illegal option or value: " + optionName + "="
                + optionValue);
    }

    /**
     * Attempts to set an option of array type
     *
     * @param ct         the transformation object
     * @param optionName the name of the option
     * @param list       the list of the option values
     * @param cols       number of entries
     */
    public static void setOptionArray(Object ct, String optionName, List list, int cols)
            throws ConfiguratorParseException {

        Object[] argument;
        Class[] paramType = new Class[1];

        try {
            switch (isArrayOption(ct, optionName)) {
                case 'D': // double:
                {
                    argument = new Object[]{new double[cols]};
                    for (int i = 0; i < list.size(); i++)
                        ((double[]) (argument[0]))[i] = Double.valueOf((String) list.get(i));
                    paramType[0] = Class.forName("[D");
                    break;
                }
                case 'I': // integer:
                {
                    argument = new Object[]{new int[cols]};
                    for (int i = 0; i < list.size(); i++)
                        ((int[]) (argument[0]))[i] = Integer.valueOf((String) list.get(i));
                    break;
                }
                case 'L': // string:
                {
                    argument = new Object[]{new String[cols]};
                    for (int i = 0; i < list.size(); i++)
                        ((String[]) (argument[0]))[i] = (String) list.get(i);
                    break;
                }
                default:
                    throw new Exception("getOption not implemented for [[" + isArrayArrayOption(ct, optionName));

            }
            paramType[0] = argument[0].getClass();
            ct.getClass().getMethod("setOption" + optionName, paramType).invoke(ct, argument);
        } catch (Exception ex) {
            Basic.caught(ex);
            throw new ConfiguratorParseException("Illegal option or values: " + optionName + "="
                    + list);
        }
    }

    /**
     * Attempts to set an option of array type
     *
     * @param ct         the transformation object
     * @param optionName the name of the option
     * @param list       the list of the option values
     */
    public static void setOptionArrayArray(Object ct, String optionName, List list, int rows, int cols)
            throws ConfiguratorParseException {

        Object[] argument;
        Class[] paramType = new Class[1];

        try {
            switch (isArrayArrayOption(ct, optionName)) {
                case 'D': // double:
                {
                    argument = new Object[]{new double[rows][cols]};
                    int count = 0;
                    for (int i = 0; i < rows; i++)
                        for (int j = 0; j < cols; j++)
                            ((double[][]) (argument[0]))[i][j] = Double.valueOf((String) list.get(count++));
                    paramType[0] = Class.forName("[D");
                    break;
                }
                case 'I': // integer:
                {
                    argument = new Object[]{new int[rows][cols]};
                    int count = 0;
                    for (int i = 0; i < rows; i++)
                        for (int j = 0; j < cols; j++)
                            ((int[][]) (argument[0]))[i][j] = Integer.valueOf((String) list.get(count++));
                    break;
                }
                case 'L': // hope its a string
                {
                    argument = new Object[]{new String[rows][cols]};
                    int count = 0;
                    for (int i = 0; i < rows; i++)
                        for (int j = 0; j < cols; j++)
                            ((String[][]) (argument[0]))[i][j] = (String) list.get(count++);
                    break;
                }
                default:
                    throw new Exception("getOption not implemented for [[" + isArrayArrayOption(ct, optionName));
            }
            paramType[0] = argument[0].getClass();
            ct.getClass().getMethod("setOption" + optionName, paramType).invoke(ct, argument);
        } catch (Exception ex) {
            Basic.caught(ex);
            throw new ConfiguratorParseException("Illegal option or values: " + optionName + "="
                    + list);
        }
    }

    /**
     * Sets a boolean option
     *
     * @param ct      the transformation object
     * @param metName the name of the option
     * @param value   the new value
     */
    public static void setOption(Object ct, String metName,
                                 boolean value) throws Exception {
        Object[] argument = new Object[]{value};
        Class[] paramType = new Class[]{Boolean.TYPE};

        ct.getClass().getMethod("setOption" + metName, paramType).invoke(ct, argument);
    }

    /**
     * Sets a string option
     *
     * @param ct      the transformation object
     * @param metName the name of the option
     * @param value   the new value
     */
    public static void setOption(Object ct, String metName,
                                 String value) throws Exception {
        Object[] argument = new Object[]{value};
        Class[] paramType = new Class[]{String.class};

        ct.getClass().getMethod("setOption" + metName, paramType).invoke(ct, argument);
    }

    /**
     * Sets a double option
     *
     * @param ct      the transformation object
     * @param metName the name of the option
     * @param value   the new value
     */
    public static void setOption(Object ct, String metName,
                                 double value) throws Exception {
        Object[] argument = new Object[]{value};
        Class[] paramType = new Class[]{Double.TYPE};

        ct.getClass().getMethod("setOption" + metName, paramType).invoke(ct, argument);
    }

    /**
     * Sets an int option
     *
     * @param ct      the transformation object
     * @param metName the name of the option
     * @param value   the new value
     */
    public static void setOption(Object ct, String metName,
                                 int value) throws Exception {
        Object[] argument = new Object[]{value};
        Class[] paramType = new Class[]{Integer.TYPE};
        ct.getClass().getMethod("setOption" + metName, paramType).invoke(ct, argument);
    }

    /**
     * Gets an option
     *
     * @param ct   the  object
     * @param name the name of the option
     * @return the value of the named option as a string
     */
    public static Object getOption(Object ct, String name) {    //all "getOption" Methods must not have any parameters!

        Object value = new Object();
        try {
            value = ct.getClass().getMethod("getOption" + name).invoke(ct);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.err.println(e.getMessage());
        }
        return value;
    }

    /**
     * Gets a description of the given transform algorithm
     *
     * @param ct the transformation object (e.g. CharactersTransform, SplitsTransform, ...)
     * @return description of the implemented algorithm
     */
    public static String getDescription(Object ct) {
        String result;

        if (ct == null)
            return "";

        try {
            result = (String) ct.getClass().getMethod("getDescription").invoke(ct);
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    /**
     * return true if the Option is contained in the ct, else false
     *
     * @param ct      the transformation object
     * @param metName the name of the option
     * @return true if the Option with name metName is contained in the transform opject
     */
    public static boolean containsMethod(Object ct, String metName) {
        if (ct == null)
            return false;
        else {
            Method[] methods = ct.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().compareToIgnoreCase(metName) == 0) return true;
            }
        }
        return false;
    }

    /**
     * returns the setter method for the named option
     *
     * @param ct      the tranformation
     * @param optName
     */
    public static Method getSetterMethod(Object ct, String optName) {
        Method[] methods = ct.getClass().getMethods();
        for (Method method : methods)
            if (method.getName().equals("setOption" + optName))
                return method;
        return null;
    }


    /**
     * determines whether option is a array, and if so, returns the element type
     *
     * @param ct
     * @param optName
     * @return D, S, I, F or 0
     */
    public static char isArrayOption(Object ct, String optName) {
        Method setter = getSetterMethod(ct, optName);
        if (setter != null) {
            Object[] paramTypes = setter.getParameterTypes();
            if (paramTypes.length == 1) {
                String name = ((Class) paramTypes[0]).getName();
                if (name.startsWith("[")
                        && !name.startsWith("[["))
                    return name.charAt(1);
            }
        }
        return 0;
    }

    /**
     * determines whether option is an array or arrays, and if so, returns the element type
     *
     * @param ct
     * @param optName
     * @return D, S, I, F or 0
     */
    public static char isArrayArrayOption(Object ct, String optName) {
        Method setter = getSetterMethod(ct, optName);
        if (setter != null) {
            Object[] paramTypes = setter.getParameterTypes();
            if (paramTypes.length == 1) {
                String name = ((Class) paramTypes[0]).getName();
                if (name.startsWith("[[")
                        && !name.startsWith("[[["))
                    return name.charAt(2);
            }
        }
        return 0;
    }

    /**
     * returns the getter method for the named option
     *
     * @param ct      the tranformation
     * @param optName
     */
    public static Method getGetterMethod(Object ct, String optName) {
        Method[] methods = ct.getClass().getMethods();
        for (Method method : methods)
            if (method.getName().equals("getOption" + optName))
                return method;
        return null;
    }

    /**
     * returns the selection method for the named option
     *
     * @param ct      the tranformation
     * @param optName
     */
    public static Method getSelectionMethod(Object ct, String optName) {
        Method[] methods = ct.getClass().getMethods();
        for (Method method : methods)
            if (method.getName().equals("selectionOption" + optName))
                return method;
        return null;
    }

    /**
     * returns true, if given class has a field with the given name
     *
     * @param clazz
     * @param name
     * @return true, if object has a field with the given name
     */
    public static boolean hasField(Class clazz, String name) {
        try {
            clazz.getField(name);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}

//EOF
