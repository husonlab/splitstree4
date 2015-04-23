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


import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: bryant
 * Date: May 8, 2005
 * Time: 1:22:37 PM
 * <p/>
 * A basic class for managing a partition of a set. Several things could be made more efficient -for example
 * an array or similar could be used to detect which elements belong to which block. It may also
 * be possible to handle blocks using bitsets in the case of a partition of integers.
 */
public class Partition {

    public static final int NOT_FOUND = -1;
    public static final int NOT_UNIQUE = -2;

    private final Vector<Set<Integer>> blocks;  //Blocks. 0..nblocks-1
    private final Vector<String> blockNames; //Names. 0..nblocks-1
    private final Set<Integer> elements;

    public Partition() {
        blocks = new Vector<>();
        blockNames = new Vector<>();
        elements = new HashSet<>();
    }

    public int getNumBlocks() {
        return blocks.size();
    }

    public int getNumElements() {
        return elements.size();
    }

    /**
     * addBlock
     *
     * @param s
     * @throws Exception if new set overlaps with existing blocks
     */
    public void addBlock(Set<Integer> s) throws Exception {
        addBlock(s, null);
    }

    /**
     * Adds a block together with a name for that block.
     *
     * @param s
     * @param name
     * @throws Exception if new set overlaps with existing blocks
     */
    public void addBlock(Set<Integer> s, String name) throws Exception {
        /* First check to see if s is disjoint with the elements still there */
        for (Integer i : s) {
            if (contains(i))
                throw new Exception("Blocks in partition not disjoint");
        }
        // Add all elements to list
        elements.addAll(s);
        blocks.add(s);
        if (name == null)
            name = "";
        blockNames.add(name);
    }

    /**
     * Adds a block together with a name for that block.
     *
     * @param first index of first contained position
     * @param last  index of last contained position
     * @param name
     * @throws Exception if new set overlaps with existing blocks
     */
    public void addBlock(int first, int last, String name) throws Exception {
        /* First check to see if s is disjoint with the elements still there */
        SortedSet<Integer> set = new TreeSet<>();
        for (int i = first; i <= last; i++) {
            if (contains(i))
                throw new Exception("Blocks in partition not disjoint");
            set.add(i);
            elements.add(i);
        }
        blocks.add(set);
        if (name == null)
            name = "";
        blockNames.add(name);

    }

    /**
     * gets a block
     *
     * @param blockNum
     * @return block
     */
    public Set<Integer> getBlock(int blockNum) {
        return blocks.get(blockNum - 1);
    }

    /**
     * Returns the name of a given block (or null if it has none)
     *
     * @param blockNum
     * @return String name of the block
     */
    public String getBlockName(int blockNum) {
        return blockNames.get(blockNum - 1);
    }


    public boolean contains(Integer i) {
        return elements.contains(i);
    }

    /**
     * Determine which block contains the given object
     *
     * @param j
     * @return Returns the number of the block containing o, or Partition.NOT_FOUND if not found.
     */
    public int whichBlock(Integer j) {
        if (!contains(j))
            return Partition.NOT_FOUND;
        int i = 1;
        for (Set<Integer> block : blocks) {
            if (block.contains(j))
                return i;
            i++;
        }
        return Partition.NOT_FOUND; //we should never get here.
    }


}
