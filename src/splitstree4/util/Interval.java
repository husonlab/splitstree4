/*
 * Interval.java Copyright (C) 2022 Daniel H. Huson
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


import jloda.util.StringUtils;

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

	public String toString() {
		return "(" + StringUtils.removeTrailingZerosAfterDot(String.format("%.3f", low)) + "," + StringUtils.removeTrailingZerosAfterDot(String.format("%.3f", high)) + ")";
	}
}
