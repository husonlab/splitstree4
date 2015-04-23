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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: Sep 21, 2005
 * Time: 6:53:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class DecimalFormatUS {
    private String formatString = "";
    private final DecimalFormat decFormat = new DecimalFormat(formatString, new DecimalFormatSymbols(Locale.US));

    public DecimalFormatUS(String formatString) {
        this.formatString = formatString;
    }

    String format(double x) {
        return decFormat.format(x);
    }

    String format(float x) {
        return decFormat.format(x);
    }
}
