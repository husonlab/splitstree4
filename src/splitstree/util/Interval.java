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


/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 30, 2005
 * Time: 12:37:31 PM
 * <p/>
 * Class for storing confidence intervals.
 */
//TODO: Change floats to doubles.
public class Interval {
    public float low;
    public float high;

    public Interval() {
        low = high = 0;
    }

    public Interval(float low, float high) {
        this.low = low;
        this.high = high;
    }

    public Interval(double low, double high) {
        this.low = (float) low;
        this.high = (float) high;
    }

    public String print() {
        DecimalFormatUS dec = new DecimalFormatUS("#0.0#####");
        return "(" + dec.format(low) + "," + dec.format(high) + ")";
    }
}
